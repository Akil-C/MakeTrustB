package com.markettrust.user.controller;

import com.markettrust.security.SecurityUtils;
import com.markettrust.user.dto.UserDto;
import com.markettrust.user.dto.UserProfileUpdateRequest;
import com.markettrust.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for authenticated-user profile operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Profile management endpoints for authenticated users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    // ---------------------------------------------------------------------------
    // Get own profile
    // ---------------------------------------------------------------------------

    @Operation(summary = "Get my profile",
               description = "Returns the full profile of the currently authenticated user.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User profile returned"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> getMyProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(userService.getCurrentUser(userId));
    }

    // ---------------------------------------------------------------------------
    // Update own profile
    // ---------------------------------------------------------------------------

    @Operation(summary = "Update my profile",
               description = "Updates name, phone, or profile image URL. Only provided (non-null) fields are changed.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile updated"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> updateMyProfile(
            @Valid @RequestBody UserProfileUpdateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        UserDto updated = userService.updateProfile(userId, request);
        return ResponseEntity.ok(updated);
    }

    // ---------------------------------------------------------------------------
    // Change password
    // ---------------------------------------------------------------------------

    @Operation(summary = "Change my password",
               description = "Changes the authenticated user's password after verifying the current one.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Password changed successfully"),
        @ApiResponse(responseCode = "400", description = "Old password incorrect or new password too short"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PutMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> changePassword(
            @Parameter(description = "Current (old) password", required = true)
            @RequestParam @NotBlank String oldPassword,

            @Parameter(description = "New password (min 8 characters)", required = true)
            @RequestParam @NotBlank @Size(min = 8) String newPassword) {

        Long userId = SecurityUtils.getCurrentUserId();
        userService.changePassword(userId, oldPassword, newPassword);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }
}
