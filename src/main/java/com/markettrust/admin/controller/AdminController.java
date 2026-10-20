package com.markettrust.admin.controller;

import com.markettrust.admin.dto.AdminDashboardDto;
import com.markettrust.admin.service.AdminService;
import com.markettrust.product.dto.ProductSummaryDto;
import com.markettrust.product.service.ProductService;
import com.markettrust.security.SecurityUtils;
import com.markettrust.seller.dto.KycStatusDto;
import com.markettrust.seller.dto.SellerProfileDto;
import com.markettrust.seller.service.SellerService;
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

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Controls", description = "Platform management, user/seller controls, KYC moderation")
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;
    private final ProductService productService;
    private final SellerService sellerService;

    @GetMapping({"/dashboard", "/stats"})
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

    @GetMapping("/sellers")
    @Operation(summary = "Get all sellers")
    public ResponseEntity<Page<SellerProfileDto>> getSellers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(sellerService.getAllSellers(PageRequest.of(page, size)));
    }

    @GetMapping("/kyc")
    @Operation(summary = "Get pending KYC verification queue")
    public ResponseEntity<Page<KycStatusDto>> getPendingKyc(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getPendingKyc(PageRequest.of(page, size)));
    }

    @RequestMapping(value = "/kyc/{sellerId}/approve", method = {RequestMethod.PUT, RequestMethod.POST})
    @Operation(summary = "Approve seller KYC verification")
    public ResponseEntity<Map<String, String>> approveKyc(
            @PathVariable Long sellerId,
            @RequestParam(required = false) String notes,
            @RequestBody(required = false) Map<String, String> body) {
        Long adminId = SecurityUtils.getCurrentUserId();
        String noteText = notes != null ? notes : (body != null ? body.get("notes") : "Documents verified");
        adminService.approveKyc(adminId, sellerId, noteText);
        return ResponseEntity.ok(Map.of("message", "KYC approved successfully"));
    }

    @RequestMapping(value = "/kyc/{sellerId}/reject", method = {RequestMethod.PUT, RequestMethod.POST})
    @Operation(summary = "Reject seller KYC verification with reason")
    public ResponseEntity<Map<String, String>> rejectKyc(
            @PathVariable Long sellerId,
            @RequestParam(required = false) String reason,
            @RequestBody(required = false) Map<String, String> body) {
        Long adminId = SecurityUtils.getCurrentUserId();
        String reasonText = reason != null ? reason : (body != null ? body.get("reason") : "Documentation invalid");
        adminService.rejectKyc(adminId, sellerId, reasonText);
        return ResponseEntity.ok(Map.of("message", "KYC rejected"));
    }

    @RequestMapping(value = "/users/{userId}/suspend", method = {RequestMethod.PUT, RequestMethod.POST})
    @Operation(summary = "Suspend user account")
    public ResponseEntity<Map<String, String>> suspendUser(
            @PathVariable Long userId,
            @RequestParam(required = false) String reason,
            @RequestBody(required = false) Map<String, String> body) {
        Long adminId = SecurityUtils.getCurrentUserId();
        String reasonText = reason != null ? reason : (body != null ? body.get("reason") : "Policy violation");
        adminService.suspendUser(adminId, userId, reasonText);
        return ResponseEntity.ok(Map.of("message", "User suspended"));
    }

    @RequestMapping(value = "/users/{userId}/unsuspend", method = {RequestMethod.PUT, RequestMethod.POST})
    @Operation(summary = "Unsuspend user account")
    public ResponseEntity<Map<String, String>> unsuspendUser(@PathVariable Long userId) {
        Long adminId = SecurityUtils.getCurrentUserId();
        adminService.unsuspendUser(adminId, userId);
        return ResponseEntity.ok(Map.of("message", "User unsuspended"));
    }

    @RequestMapping(value = "/users/{userId}/ban", method = {RequestMethod.PUT, RequestMethod.POST})
    @Operation(summary = "Ban user account")
    public ResponseEntity<Map<String, String>> banUser(
            @PathVariable Long userId,
            @RequestParam(required = false) String reason,
            @RequestBody(required = false) Map<String, String> body) {
        Long adminId = SecurityUtils.getCurrentUserId();
        String reasonText = reason != null ? reason : (body != null ? body.get("reason") : "Violation of terms");
        adminService.banUser(adminId, userId, reasonText);
        return ResponseEntity.ok(Map.of("message", "User banned"));
    }

    @RequestMapping(value = "/users/{userId}/unban", method = {RequestMethod.PUT, RequestMethod.POST})
    @Operation(summary = "Unban user account")
    public ResponseEntity<Map<String, String>> unbanUser(@PathVariable Long userId) {
        Long adminId = SecurityUtils.getCurrentUserId();
        adminService.unbanUser(adminId, userId);
        return ResponseEntity.ok(Map.of("message", "User unbanned"));
    }

    @GetMapping("/products")
    @Operation(summary = "Get all products for admin catalog moderation")
    public ResponseEntity<Page<ProductSummaryDto>> getAdminProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productService.getAdminProducts(keyword, PageRequest.of(page, size)));
    }

    @DeleteMapping("/products/{id}")
    @Operation(summary = "Admin hide / remove product")
    public ResponseEntity<Map<String, String>> removeProduct(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Policy violation") String reason) {
        Long adminId = SecurityUtils.getCurrentUserId();
        productService.adminHideProduct(adminId, id, reason);
        return ResponseEntity.ok(Map.of("message", "Product removed by admin"));
    }
}
