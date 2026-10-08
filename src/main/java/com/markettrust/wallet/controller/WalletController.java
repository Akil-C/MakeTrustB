package com.markettrust.wallet.controller;

import com.markettrust.security.SecurityUtils;
import com.markettrust.wallet.dto.*;
import com.markettrust.wallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for wallet operations.
 */
@RestController
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    // -------------------------------------------------------------------------
    // User endpoints
    // -------------------------------------------------------------------------

    /** GET /api/wallet — authenticated user views own wallet */
    @GetMapping("/api/wallet")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WalletDto> getMyWallet() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(walletService.getWalletDtoByUserId(userId));
    }

    /** GET /api/wallet/transactions — paginated transaction history */
    @GetMapping("/api/wallet/transactions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<WalletTransactionDto>> getMyTransactions(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(walletService.getTransactionHistory(userId, pageable));
    }

    // -------------------------------------------------------------------------
    // Admin endpoints
    // -------------------------------------------------------------------------

    /** POST /api/admin/wallet/grant — admin grants credits to a user */
    @PostMapping("/api/admin/wallet/grant")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WalletDto> grantCredits(
            @Valid @RequestBody AdminGrantCreditsRequest request) {
        Long adminId = SecurityUtils.getCurrentUserId();
        WalletDto result = walletService.adminGrantCredits(
                adminId, request.targetUserId(), request.amount(), request.reason());
        return ResponseEntity.ok(result);
    }

    /** POST /api/admin/wallet/adjust — admin adjusts credits (positive or negative) */
    @PostMapping("/api/admin/wallet/adjust")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WalletDto> adjustCredits(
            @Valid @RequestBody AdminAdjustCreditsRequest request) {
        Long adminId = SecurityUtils.getCurrentUserId();
        WalletDto result = walletService.adminAdjustCredits(
                adminId, request.targetUserId(), request.amount(), request.reason());
        return ResponseEntity.ok(result);
    }

    /** POST /api/admin/wallet/{userId}/freeze */
    @PostMapping("/api/admin/wallet/{userId}/freeze")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> freezeWallet(
            @PathVariable Long userId,
            @RequestParam(required = false, defaultValue = "Frozen by admin") String reason) {
        Long adminId = SecurityUtils.getCurrentUserId();
        walletService.adminFreezeWallet(adminId, userId, reason);
        return ResponseEntity.noContent().build();
    }

    /** POST /api/admin/wallet/{userId}/unfreeze */
    @PostMapping("/api/admin/wallet/{userId}/unfreeze")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> unfreezeWallet(
            @PathVariable Long userId) {
        Long adminId = SecurityUtils.getCurrentUserId();
        walletService.adminUnfreezeWallet(adminId, userId);
        return ResponseEntity.noContent().build();
    }

    /** GET /api/admin/wallet/{userId} — admin views a user's wallet */
    @GetMapping("/api/admin/wallet/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WalletDto> getUserWallet(
            @PathVariable Long userId) {
        Long adminId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(walletService.getWalletByUserIdForAdmin(adminId, userId));
    }

    /** GET /api/admin/wallet/stats — platform-wide credit statistics */
    @GetMapping("/api/admin/wallet/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlatformWalletStatsDto> getPlatformStats() {
        Long adminId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(walletService.getPlatformStats(adminId));
    }
}
