package com.markettrust.wallet.service;

import com.markettrust.wallet.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

/**
 * Core wallet operations used by other modules (Order, etc.) as well as
 * the wallet REST controller.
 */
public interface WalletServiceInterface {

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    void createWallet(Long userId);

    // -------------------------------------------------------------------------
    // User-facing queries
    // -------------------------------------------------------------------------

    WalletDto getWalletDtoByUserId(Long userId);

    Page<WalletTransactionDto> getTransactionHistory(Long userId, Pageable pageable);

    // -------------------------------------------------------------------------
    // Admin operations
    // -------------------------------------------------------------------------

    WalletDto adminGrantCredits(Long adminId, Long targetUserId, Long amount, String reason);

    WalletDto adminAdjustCredits(Long adminId, Long targetUserId, Long amount, String reason);

    void adminFreezeWallet(Long adminId, Long targetUserId, String reason);

    void adminUnfreezeWallet(Long adminId, Long targetUserId);

    WalletDto getWalletByUserIdForAdmin(Long adminId, Long targetUserId);

    PlatformWalletStatsDto getPlatformStats(Long adminId);

    // -------------------------------------------------------------------------
    // Internal purchase flow (called by OrderService)
    // -------------------------------------------------------------------------

    /**
     * Moves {@code amount} from availableCredits → heldCredits and creates a CreditHold.
     */
    void holdCreditsForPurchase(Long buyerId, BigDecimal amount, Long orderId, String idempotencyKey);

    /**
     * Called when buyer confirms delivery; releases held credits to seller.
     */
    void releaseHeldCreditsToSeller(Long orderId, Long sellerUserId, BigDecimal sellerAmount, BigDecimal platformFee);

    /**
     * Returns held credits to buyer's availableCredits on cancellation or dispute.
     */
    void refundHeldCredits(Long orderId);

    // -------------------------------------------------------------------------
    // Platform fee tracking
    // -------------------------------------------------------------------------

    void deductPlatformFee(BigDecimal amount, Long orderId);

    BigDecimal getTotalPlatformRevenue();

    BigDecimal getTotalCreditsInCirculation();
}
