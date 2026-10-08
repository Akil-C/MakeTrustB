package com.markettrust.admin.controller;

import com.markettrust.admin.dto.AdminDashboardDto;
import com.markettrust.admin.service.AdminService;
import com.markettrust.security.SecurityUtils;
import com.markettrust.user.dto.UserDto;
import com.markettrust.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Controls", description = "Platform management, user/seller controls, KYC moderation")
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get admin dashboard statistics (real database metrics)")
    public ResponseEntity<AdminDashboardDto> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    @GetMapping("/users")
    @Operation(summary = "Search/list users for admin management")
    public ResponseEntity<Page<UserDto>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(userService.searchUsers(keyword, role, status, pageable));
    }

    @PutMapping("/kyc/{sellerId}/approve")
    @Operation(summary = "Approve seller KYC verification")
    public ResponseEntity<Void> approveKyc(@PathVariable Long sellerId, @RequestParam(required = false) String notes) {
        Long adminId = SecurityUtils.getCurrentUserId();
        adminService.approveKyc(adminId, sellerId, notes);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/kyc/{sellerId}/reject")
    @Operation(summary = "Reject seller KYC verification with reason")
    public ResponseEntity<Void> rejectKyc(@PathVariable Long sellerId, @RequestParam String reason) {
        Long adminId = SecurityUtils.getCurrentUserId();
        adminService.rejectKyc(adminId, sellerId, reason);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{userId}/suspend")
    @Operation(summary = "Suspend user account")
    public ResponseEntity<Void> suspendUser(@PathVariable Long userId, @RequestParam String reason) {
        Long adminId = SecurityUtils.getCurrentUserId();
        adminService.suspendUser(adminId, userId, reason);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{userId}/unsuspend")
    @Operation(summary = "Unsuspend user account")
    public ResponseEntity<Void> unsuspendUser(@PathVariable Long userId) {
        Long adminId = SecurityUtils.getCurrentUserId();
        adminService.unsuspendUser(adminId, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{userId}/ban")
    @Operation(summary = "Ban user account")
    public ResponseEntity<Void> banUser(@PathVariable Long userId, @RequestParam String reason) {
        Long adminId = SecurityUtils.getCurrentUserId();
        adminService.banUser(adminId, userId, reason);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{userId}/unban")
    @Operation(summary = "Unban user account")
    public ResponseEntity<Void> unbanUser(@PathVariable Long userId) {
        Long adminId = SecurityUtils.getCurrentUserId();
        adminService.unbanUser(adminId, userId);
        return ResponseEntity.ok().build();
    }
}
