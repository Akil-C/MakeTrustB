package com.markettrust.user.service;

import com.markettrust.exception.BadRequestException;
import com.markettrust.exception.ResourceNotFoundException;
import com.markettrust.user.dto.UserDto;
import com.markettrust.user.dto.UserProfileUpdateRequest;
import com.markettrust.user.entity.RoleName;
import com.markettrust.user.entity.User;
import com.markettrust.user.entity.UserStatus;
import com.markettrust.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.stream.Collectors;

/**
 * Service for user profile management and admin user operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ---------------------------------------------------------------------------
    // Self-service (authenticated user)
    // ---------------------------------------------------------------------------

    /**
     * Returns the DTO for the currently authenticated user.
     *
     * @param userId the authenticated user's ID
     * @return {@link UserDto}
     */
    @Transactional(readOnly = true)
    public UserDto getCurrentUser(Long userId) {
        User user = findUserById(userId);
        return mapToDto(user);
    }

    /**
     * Updates mutable profile fields for the authenticated user.
     * Only non-null request fields are applied (PATCH semantics).
     *
     * @param userId  the authenticated user's ID
     * @param request the update payload
     * @return updated {@link UserDto}
     */
    @Transactional
    public UserDto updateProfile(Long userId, UserProfileUpdateRequest request) {
        User user = findUserById(userId);

        if (StringUtils.hasText(request.getName())) {
            user.setName(request.getName());
        }
        if (StringUtils.hasText(request.getPhone())) {
            String phone = request.getPhone();
            if (!phone.equals(user.getPhone()) && userRepository.existsByPhone(phone)) {
                throw new BadRequestException("Phone number is already in use: " + phone);
            }
            user.setPhone(phone);
        }
        if (request.getProfileImageUrl() != null) {
            user.setProfileImageUrl(request.getProfileImageUrl());
        }

        User saved = userRepository.save(user);
        log.info("Profile updated for userId={}", userId);
        return mapToDto(saved);
    }

    /**
     * Changes the password for the authenticated user after verifying the old one.
     *
     * @param userId      the authenticated user's ID
     * @param oldPassword the current password (plain-text for verification)
     * @param newPassword the desired new password (plain-text, min 8 chars)
     */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 8) {
            throw new BadRequestException("New password must be at least 8 characters");
        }
        User user = findUserById(userId);

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (oldPassword.equals(newPassword)) {
            throw new BadRequestException("New password must be different from the current password");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed for userId={}", userId);
    }

    // ---------------------------------------------------------------------------
    // Admin operations
    // ---------------------------------------------------------------------------

    /**
     * Returns the DTO for any user by ID. For admin use.
     *
     * @param userId the target user's ID
     * @return {@link UserDto}
     */
    @Transactional(readOnly = true)
    public UserDto getUserById(Long userId) {
        return mapToDto(findUserById(userId));
    }

    /**
     * Searches users with optional keyword, role, and status filters.
     *
     * @param keyword  partial match on name / email (nullable)
     * @param role     role name string e.g. "BUYER" (nullable)
     * @param status   status string e.g. "ACTIVE" (nullable)
     * @param pageable pagination and sorting
     * @return page of {@link UserDto}
     */
    @Transactional(readOnly = true)
    public Page<UserDto> searchUsers(String keyword, String role, String status, Pageable pageable) {
        RoleName   roleName   = parseRoleName(role);
        UserStatus userStatus = parseStatus(status);

        return userRepository.searchUsers(keyword, roleName, userStatus, pageable)
                .map(this::mapToDto);
    }

    /**
     * Suspends a user account. Admin only.
     *
     * @param adminId the admin performing the action (for audit trail)
     * @param userId  the target user
     * @param reason  human-readable reason
     */
    @Transactional
    public void suspendUser(Long adminId, Long userId, String reason) {
        User user = findUserById(userId);
        assertNotSelf(adminId, userId, "suspend");
        user.setStatus(UserStatus.SUSPENDED);
        userRepository.save(user);
        log.warn("User SUSPENDED: targetUserId={} by adminId={} reason='{}'", userId, adminId, reason);
    }

    /**
     * Removes the suspension from a user account. Admin only.
     */
    @Transactional
    public void unsuspendUser(Long adminId, Long userId) {
        User user = findUserById(userId);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        log.info("User UNSUSPENDED: targetUserId={} by adminId={}", userId, adminId);
    }

    /**
     * Permanently bans a user. Admin only.
     */
    @Transactional
    public void banUser(Long adminId, Long userId, String reason) {
        User user = findUserById(userId);
        assertNotSelf(adminId, userId, "ban");
        user.setStatus(UserStatus.BANNED);
        userRepository.save(user);
        log.warn("User BANNED: targetUserId={} by adminId={} reason='{}'", userId, adminId, reason);
    }

    /**
     * Lifts a ban from a user. Admin only.
     */
    @Transactional
    public void unbanUser(Long adminId, Long userId) {
        User user = findUserById(userId);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        log.info("User UNBANNED: targetUserId={} by adminId={}", userId, adminId);
    }

    // ---------------------------------------------------------------------------
    // Mapping helpers
    // ---------------------------------------------------------------------------

    private UserDto mapToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus())
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .profileImageUrl(user.getProfileImageUrl())
                .roles(user.getRoles().stream()
                        .map(r -> r.getName().name())
                        .collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .build();
    }

    // ---------------------------------------------------------------------------
    // Internal utilities
    // ---------------------------------------------------------------------------

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private void assertNotSelf(Long adminId, Long targetId, String action) {
        if (adminId.equals(targetId)) {
            throw new BadRequestException("Admin cannot " + action + " their own account");
        }
    }

    private RoleName parseRoleName(String role) {
        if (!StringUtils.hasText(role)) return null;
        try {
            return RoleName.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role: " + role);
        }
    }

    private UserStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) return null;
        try {
            return UserStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + status);
        }
    }
}
