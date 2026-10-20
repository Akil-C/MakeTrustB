package com.markettrust.auth.service;

import com.markettrust.auth.dto.*;
import com.markettrust.exception.BadRequestException;
import com.markettrust.exception.ResourceNotFoundException;
import com.markettrust.exception.UnauthorizedException;
import com.markettrust.security.JwtTokenProvider;
import com.markettrust.seller.entity.KycStatus;
import com.markettrust.seller.entity.SellerLevel;
import com.markettrust.seller.entity.SellerProfile;
import com.markettrust.seller.repository.SellerProfileRepository;
import com.markettrust.user.entity.*;
import com.markettrust.user.repository.RefreshTokenRepository;
import com.markettrust.user.repository.RoleRepository;
import com.markettrust.user.repository.UserRepository;
import com.markettrust.wallet.entity.Wallet;
import com.markettrust.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core authentication service — registration, login, token lifecycle, password reset.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager  authenticationManager;
    private final UserRepository         userRepository;
    private final RoleRepository         roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder        passwordEncoder;
    private final JwtTokenProvider       jwtTokenProvider;
    private final WalletService          walletService;
    private final SellerProfileRepository sellerProfileRepository;
    private final RestTemplate           restTemplate;

    @Value("${app.captcha.enabled:false}")
    private boolean captchaEnabled;

    @Value("${app.captcha.turnstile.secret-key:}")
    private String turnstileSecretKey;

    @Value("${app.jwt.refresh-token-expiry-days:7}")
    private long refreshTokenExpiryDays;

    // ---------------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------------

    /**
     * Registers a new user, creates their wallet and profile placeholder, then
     * issues JWT tokens.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Validate captcha
        validateCaptcha(request.getCaptchaToken());

        String email = request.getEmail().toLowerCase().trim();

        // Guard duplicate email / phone
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email is already registered: " + email);
        }
        if (request.getPhone() != null && !request.getPhone().isBlank() && userRepository.existsByPhone(request.getPhone().trim())) {
            throw new BadRequestException("Phone number is already registered: " + request.getPhone().trim());
        }

        RoleName roleName = (request.getRole() != null) ? request.getRole() : RoleName.BUYER;
        String rawRoleStr = roleName.name().replace("ROLE_", "");
        Role role = roleRepository.findByName(roleName)
                .or(() -> roleRepository.findByName(RoleName.valueOf("ROLE_" + rawRoleStr)))
                .or(() -> roleRepository.findByName(RoleName.valueOf(rawRoleStr)))
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

        Set<Role> roles = new HashSet<>();
        roles.add(role);

        // Build and persist user
        User user = User.builder()
                .name(request.getName().trim())
                .email(email)
                .phone(request.getPhone() != null && !request.getPhone().isBlank() ? request.getPhone().trim() : null)
                .password(passwordEncoder.encode(request.getPassword()))
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .phoneVerified(false)
                .roles(roles)
                .build();

        user = userRepository.save(user);
        log.info("New user registered: id={} email={} role={}", user.getId(), user.getEmail(), roleName);

        // Post-registration setup
        createWalletForUser(user);
        if (roleName == RoleName.BUYER || roleName == RoleName.ROLE_BUYER) {
            createBuyerProfile(user);
        } else if (roleName == RoleName.SELLER || roleName == RoleName.ROLE_SELLER) {
            createSellerProfilePlaceholder(user);
        }

        // Issue tokens
        return buildAuthResponse(user);
    }

    /**
     * Authenticates a user and issues JWT tokens.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        validateCaptcha(request.getCaptchaToken());

        String email = request.getEmail().toLowerCase().trim();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email,
                        request.getPassword()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getStatus() == UserStatus.BANNED) {
            throw new UnauthorizedException("Your account has been banned. Please contact support.");
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new UnauthorizedException("Your account is suspended. Please contact support.");
        }

        log.info("User logged in: id={} email={}", user.getId(), user.getEmail());
        return buildAuthResponse(user);
    }

    /**
     * Validates a refresh token and issues a new access token.
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(storedToken);
            throw new UnauthorizedException("Refresh token has expired. Please log in again.");
        }

        User user = storedToken.getUser();
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);

        log.info("Access token refreshed for userId={}", user.getId());

        Wallet wallet = walletService.getWalletByUserId(user.getId());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(storedToken.getToken())
                .expiresIn(jwtTokenProvider.getAccessTokenExpirySeconds())
                .userInfo(buildUserInfo(user, wallet))
                .build();
    }

    /**
     * Revokes the provided refresh token (logout).
     */
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
            refreshTokenRepository.delete(token);
            log.info("User logged out: userId={}", token.getUser().getId());
        });
    }

    /**
     * Simulates sending a password-reset email by logging the reset link.
     * Replace with a real email service in production.
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail().toLowerCase()).ifPresent(user -> {
            String resetToken = UUID.randomUUID().toString();
            // TODO: persist PasswordResetToken entity, send via email service
            log.info("[DEMO] Password reset link for userId={}: https://markettrust.com/reset-password?token={}",
                    user.getId(), resetToken);
        });
        // Always return success to avoid email enumeration
    }

    /**
     * Simulates resetting the password via a token.
     * Replace with real token lookup + validation in production.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // TODO: look up PasswordResetToken, validate expiry, update password
        log.info("[DEMO] Password reset called with token={}", request.getToken());
    }

    /**
     * Validates a Cloudflare Turnstile captcha token.
     * Returns {@code true} immediately if captcha is disabled (demo mode).
     *
     * @param token the captcha token from the client
     * @return {@code true} if valid or captcha is disabled
     */
    public boolean validateCaptcha(String token) {
        if (!captchaEnabled) {
            log.debug("Captcha validation skipped (demo mode)");
            return true;
        }
        if (token == null || token.isBlank()) {
            throw new BadRequestException("Captcha token is required");
        }
        try {
            Map<?, ?> response = restTemplate.postForObject(
                    "https://challenges.cloudflare.com/turnstile/v0/siteverify",
                    Map.of("secret", turnstileSecretKey, "response", token),
                    Map.class
            );
            boolean success = Boolean.TRUE.equals(response != null ? response.get("success") : false);
            if (!success) {
                throw new BadRequestException("Captcha verification failed. Please try again.");
            }
            return true;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Captcha validation error: {}", e.getMessage());
            throw new BadRequestException("Captcha service unavailable. Please try again.");
        }
    }

    // ---------------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------------

    private void createWalletForUser(User user) {
        walletService.createWallet(user.getId());
        log.debug("Wallet provisioned for userId={}", user.getId());
    }

    private void createBuyerProfile(User user) {
        // TODO: delegate to BuyerProfileService when available
        log.debug("Buyer profile placeholder created for userId={}", user.getId());
    }

    private void createSellerProfilePlaceholder(User user) {
        if (sellerProfileRepository.findByUserId(user.getId()).isEmpty()) {
            SellerProfile sp = new SellerProfile();
            sp.setUserId(user.getId());
            sp.setUser(user);
            sp.setDisplayName(user.getName() != null && !user.getName().isBlank() ? user.getName() : "Seller " + user.getId());
            sp.setBio("Verified Merchant on Nexora Marketplace.");
            sp.setCity("Bengaluru");
            sp.setState("Karnataka");
            sp.setCountry("India");
            sp.setKycStatus(KycStatus.PENDING);
            sp.setIsVerified(false);
            sp.setSellerLevel(SellerLevel.NEW);
            sp.setTrustScore(15.0);
            sp.setAverageRating(0.0);
            sp.setReviewCount(0);
            sp.setCompletedOrders(0);
            sp.setTotalSales(BigDecimal.ZERO);
            sp.setResponseRate(100.0);
            sp.setCancellationRate(0.0);
            sp.setSellerSince(java.time.LocalDateTime.now());
            sellerProfileRepository.save(sp);
            log.info("Seller profile provisioned for userId={}", user.getId());
        }
    }

    private AuthResponse buildAuthResponse(User user) {
        // Revoke old refresh tokens for this user (single-session strategy)
        refreshTokenRepository.deleteByUserId(user.getId());

        String accessToken  = jwtTokenProvider.generateAccessToken(user);
        String refreshValue = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshValue)
                .user(user)
                .expiresAt(Instant.now().plusSeconds(refreshTokenExpiryDays * 86_400L))
                .build();
        refreshTokenRepository.save(refreshToken);

        Wallet wallet = walletService.getWalletByUserId(user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshValue)
                .expiresIn(jwtTokenProvider.getAccessTokenExpirySeconds())
                .userInfo(buildUserInfo(user, wallet))
                .build();
    }

    private AuthResponse.UserInfo buildUserInfo(User user, Wallet wallet) {
        Set<String> roleNames = user.getRoles().stream()
                .map(r -> r.getName().name().replace("ROLE_", ""))
                .collect(Collectors.toSet());

        String primaryRole = "BUYER";
        if (roleNames.contains("ADMIN")) {
            primaryRole = "ADMIN";
        } else if (roleNames.contains("SELLER")) {
            primaryRole = "SELLER";
        }

        return AuthResponse.UserInfo.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(primaryRole)
                .profileImageUrl(user.getProfileImageUrl())
                .walletBalance(wallet != null ? wallet.getBalance() : BigDecimal.ZERO)
                .build();
    }
}
