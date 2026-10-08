package com.markettrust.auth.controller;

import com.markettrust.auth.dto.*;
import com.markettrust.auth.service.AuthService;
import com.markettrust.security.SecurityUtils;
import com.markettrust.user.dto.UserDto;
import com.markettrust.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for all authentication and identity operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for registration, login, token management, and password reset")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    // ---------------------------------------------------------------------------
    // Registration
    // ---------------------------------------------------------------------------

    @Operation(summary = "Register a new user",
               description = "Creates a BUYER or SELLER account, provisions a wallet, and returns JWT tokens.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User registered successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error or duplicate email/phone"),
        @ApiResponse(responseCode = "409", description = "Email or phone already registered")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ---------------------------------------------------------------------------
    // Login
    // ---------------------------------------------------------------------------

    @Operation(summary = "Authenticate and obtain JWT tokens",
               description = "Validates credentials and returns an access token + refresh token pair.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials, banned, or suspended account"),
        @ApiResponse(responseCode = "400", description = "Validation error")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    // ---------------------------------------------------------------------------
    // Token refresh
    // ---------------------------------------------------------------------------

    @Operation(summary = "Refresh access token",
               description = "Issues a new access token using a valid refresh token.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token refreshed"),
        @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    // ---------------------------------------------------------------------------
    // Logout
    // ---------------------------------------------------------------------------

    @Operation(summary = "Revoke refresh token (logout)",
               description = "Invalidates the provided refresh token so it cannot be reused.")
    @ApiResponse(responseCode = "200", description = "Logged out successfully")
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    // ---------------------------------------------------------------------------
    // Password reset flow
    // ---------------------------------------------------------------------------

    @Operation(summary = "Request a password reset",
               description = "Triggers a simulated password-reset email (demo mode logs the link).")
    @ApiResponse(responseCode = "200", description = "Reset link sent if account exists")
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(Map.of(
                "message", "If an account with that email exists, a reset link has been sent."));
    }

    @Operation(summary = "Reset password using token",
               description = "Applies the new password after validating the reset token (demo simulation).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Password reset successful"),
        @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password has been reset successfully."));
    }

    // ---------------------------------------------------------------------------
    // Current user (me)
    // ---------------------------------------------------------------------------

    @Operation(summary = "Get the currently authenticated user",
               description = "Returns full profile information for the authenticated user.",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Current user profile"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> me() {
        Long userId = SecurityUtils.getCurrentUserId();
        UserDto dto = userService.getCurrentUser(userId);
        return ResponseEntity.ok(dto);
    }
}
