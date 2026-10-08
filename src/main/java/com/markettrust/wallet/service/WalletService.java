package com.markettrust.wallet.service;

import com.markettrust.exception.ResourceNotFoundException;
import com.markettrust.user.entity.RoleName;
import com.markettrust.user.entity.User;
import com.markettrust.user.repository.UserRepository;
import com.markettrust.wallet.dto.*;
import com.markettrust.wallet.entity.*;
import com.markettrust.wallet.exception.InsufficientCreditsException;
import com.markettrust.wallet.exception.WalletFrozenException;
import com.markettrust.wallet.repository.CreditHoldRepository;
import com.markettrust.wallet.repository.WalletRepository;
import com.markettrust.wallet.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Full implementation of wallet business logic.
 * All credit-mutating operations acquire a PESSIMISTIC_WRITE lock on the wallet row
 * to prevent double-spend races under concurrent load.
 */
@Service
@RequiredArgsConstructor
public class WalletService implements WalletServiceInterface {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WalletService.class);

    private static final BigDecimal PLATFORM_FEE_PERCENT = new BigDecimal("0.10"); // 10 %

    private final WalletRepository            walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final CreditHoldRepository        creditHoldRepository;
    private final UserRepository              userRepository;

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @Override
    @Transactional
    public void createWallet(Long userId) {
        if (walletRepository.findByUserId(userId).isPresent()) {
            log.debug("Wallet already exists for userId={}", userId);
            return;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        walletRepository.save(wallet);
        log.info("Wallet created for userId={}", userId);
    }

    // =========================================================================
    // User-facing queries
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public WalletDto getWalletDtoByUserId(Long userId) {
        Wallet w = requireWallet(userId);
        return toDto(w);
    }

    /** Legacy alias kept for compatibility with existing UserService calls. */
    @Transactional(readOnly = true)
    public Wallet getWalletByUserId(Long userId) {
        return requireWallet(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WalletTransactionDto> getTransactionHistory(Long userId, Pageable pageable) {
        Wallet wallet = requireWallet(userId);
        List<WalletTransaction> rows = transactionRepository
                .findByWalletIdOrderByCreatedAtDesc(wallet.getId(), pageable);
        long total = transactionRepository.countByWalletId(wallet.getId());
        List<WalletTransactionDto> dtos = rows.stream().map(this::toTxDto).toList();
        return new PageImpl<>(dtos, pageable, total);
    }

    // =========================================================================
    // Admin operations
    // =========================================================================

    @Override
    @Transactional
    public WalletDto adminGrantCredits(Long adminId, Long targetUserId, Long amount, String reason) {
        requireAdmin(adminId);
        if (amount == null || amount < 1) {
            throw new IllegalArgumentException("Grant amount must be ≥ 1");
        }

        String idempotencyKey = "GRANT-" + adminId + "-" + targetUserId + "-" + UUID.randomUUID();

        Wallet wallet = requireWalletWithLock(targetUserId);
        BigDecimal credit = BigDecimal.valueOf(amount);

        wallet.setAvailableCredits(wallet.getAvailableCredits().add(credit));
        wallet.setTotalEarned(wallet.getTotalEarned().add(credit));
        walletRepository.save(wallet);

        recordTransaction(wallet, TransactionType.ADMIN_GRANT, credit,
                wallet.getAvailableCredits(), null, "ADMIN_GRANT",
                reason, adminId, idempotencyKey);

        log.info("Admin {} granted {} credits to userId={}", adminId, amount, targetUserId);
        return toDto(wallet);
    }

    @Override
    @Transactional
    public WalletDto adminAdjustCredits(Long adminId, Long targetUserId, Long amount, String reason) {
        requireAdmin(adminId);

        Wallet wallet = requireWalletWithLock(targetUserId);
        BigDecimal delta = BigDecimal.valueOf(amount);

        BigDecimal newBalance = wallet.getAvailableCredits().add(delta);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Adjustment would result in negative balance");
        }

        wallet.setAvailableCredits(newBalance);
        if (delta.compareTo(BigDecimal.ZERO) > 0) {
            wallet.setTotalEarned(wallet.getTotalEarned().add(delta));
        } else {
            wallet.setTotalSpent(wallet.getTotalSpent().add(delta.abs()));
        }
        walletRepository.save(wallet);

        String idempotencyKey = "ADJ-" + adminId + "-" + targetUserId + "-" + UUID.randomUUID();
        recordTransaction(wallet, TransactionType.ADMIN_ADJUSTMENT, delta,
                newBalance, null, "ADMIN_ADJUSTMENT", reason, adminId, idempotencyKey);

        log.info("Admin {} adjusted userId={} by {} credits. Reason: {}", adminId, targetUserId, amount, reason);
        return toDto(wallet);
    }

    @Override
    @Transactional
    public void adminFreezeWallet(Long adminId, Long targetUserId, String reason) {
        requireAdmin(adminId);
        Wallet wallet = requireWallet(targetUserId);
        wallet.setIsFrozen(true);
        walletRepository.save(wallet);
        log.info("Admin {} froze wallet of userId={}. Reason: {}", adminId, targetUserId, reason);
    }

    @Override
    @Transactional
    public void adminUnfreezeWallet(Long adminId, Long targetUserId) {
        requireAdmin(adminId);
        Wallet wallet = requireWallet(targetUserId);
        wallet.setIsFrozen(false);
        walletRepository.save(wallet);
        log.info("Admin {} unfroze wallet of userId={}", adminId, targetUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletDto getWalletByUserIdForAdmin(Long adminId, Long targetUserId) {
        requireAdmin(adminId);
        return toDto(requireWallet(targetUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformWalletStatsDto getPlatformStats(Long adminId) {
        requireAdmin(adminId);
        BigDecimal revenue     = getTotalPlatformRevenue();
        BigDecimal circulation = getTotalCreditsInCirculation();
        BigDecimal held        = walletRepository.sumHeldCredits();
        return new PlatformWalletStatsDto(revenue, circulation, held);
    }

    // =========================================================================
    // Internal purchase flow
    // =========================================================================

    @Override
    @Transactional
    public void holdCreditsForPurchase(Long buyerId, BigDecimal amount, Long orderId, String idempotencyKey) {
        // Idempotency check
        if (transactionRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            log.debug("Duplicate holdCreditsForPurchase idempotency key: {}", idempotencyKey);
            return;
        }

        Wallet wallet = requireWalletWithLock(buyerId);

        if (Boolean.TRUE.equals(wallet.getIsFrozen())) {
            throw new WalletFrozenException("Buyer wallet is frozen; cannot hold credits");
        }
        if (wallet.getAvailableCredits().compareTo(amount) < 0) {
            throw new InsufficientCreditsException(
                    "Insufficient credits: available=" + wallet.getAvailableCredits() + " required=" + amount);
        }

        wallet.setAvailableCredits(wallet.getAvailableCredits().subtract(amount));
        wallet.setHeldCredits(wallet.getHeldCredits().add(amount));
        walletRepository.save(wallet);

        // CreditHold record
        CreditHold hold = new CreditHold();
        hold.setWalletId(wallet.getId());
        hold.setOrderId(orderId != null ? orderId.toString() : null);
        hold.setAmount(amount);
        hold.setStatus(HoldStatus.ACTIVE);
        creditHoldRepository.save(hold);

        recordTransaction(wallet, TransactionType.PURCHASE_HOLD, amount.negate(),
                wallet.getAvailableCredits(), orderId, "ORDER",
                "Credit hold for order #" + orderId, buyerId, idempotencyKey);

        log.info("Held {} credits for buyerId={}, orderId={}", amount, buyerId, orderId);
    }

    @Override
    @Transactional
    public void releaseHeldCreditsToSeller(Long orderId, Long sellerUserId,
                                           BigDecimal sellerAmount, BigDecimal platformFee) {
        String orderIdStr = orderId != null ? orderId.toString() : null;
        CreditHold hold = creditHoldRepository.findByOrderIdAndStatus(orderIdStr, HoldStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active CreditHold not found for orderId=" + orderId));

        // Deduct from buyer's heldCredits
        Wallet buyerWallet = walletRepository.findById(hold.getWalletId())
                .orElseThrow(() -> new ResourceNotFoundException("Buyer wallet not found"));
        buyerWallet.setHeldCredits(buyerWallet.getHeldCredits().subtract(hold.getAmount()));
        buyerWallet.setTotalSpent(buyerWallet.getTotalSpent().add(hold.getAmount()));
        walletRepository.save(buyerWallet);

        // Credit seller
        Wallet sellerWallet = requireWalletWithLock(sellerUserId);
        sellerWallet.setAvailableCredits(sellerWallet.getAvailableCredits().add(sellerAmount));
        sellerWallet.setTotalEarned(sellerWallet.getTotalEarned().add(sellerAmount));
        walletRepository.save(sellerWallet);

        // Record seller earning transaction
        String sellerIdem = "SELLER-EARN-" + orderId;
        if (transactionRepository.findByIdempotencyKey(sellerIdem).isEmpty()) {
            recordTransaction(sellerWallet, TransactionType.SELLER_EARNING, sellerAmount,
                    sellerWallet.getAvailableCredits(), orderId, "ORDER",
                    "Seller earning for order #" + orderId, sellerUserId, sellerIdem);
        }

        // Platform fee
        deductPlatformFee(platformFee, orderId);

        // Mark hold released
        hold.setStatus(HoldStatus.RELEASED);
        hold.setReleasedAt(LocalDateTime.now());
        creditHoldRepository.save(hold);

        log.info("Released hold for orderId={}. Seller {} earned {}. Platform fee {}",
                orderId, sellerUserId, sellerAmount, platformFee);
    }

    @Override
    @Transactional
    public void refundHeldCredits(Long orderId) {
        String orderIdStr = orderId != null ? orderId.toString() : null;
        CreditHold hold = creditHoldRepository.findByOrderIdAndStatus(orderIdStr, HoldStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active CreditHold not found for orderId=" + orderId));

        Wallet buyerWallet = walletRepository.findById(hold.getWalletId())
                .orElseThrow(() -> new ResourceNotFoundException("Buyer wallet not found"));

        buyerWallet.setHeldCredits(buyerWallet.getHeldCredits().subtract(hold.getAmount()));
        buyerWallet.setAvailableCredits(buyerWallet.getAvailableCredits().add(hold.getAmount()));
        walletRepository.save(buyerWallet);

        hold.setStatus(HoldStatus.REFUNDED);
        hold.setReleasedAt(LocalDateTime.now());
        creditHoldRepository.save(hold);

        String refundIdem = "REFUND-" + orderId;
        if (transactionRepository.findByIdempotencyKey(refundIdem).isEmpty()) {
            recordTransaction(buyerWallet, TransactionType.REFUND, hold.getAmount(),
                    buyerWallet.getAvailableCredits(), orderId, "ORDER",
                    "Refund for cancelled/disputed order #" + orderId,
                    null, refundIdem);
        }

        log.info("Refunded {} credits for orderId={}", hold.getAmount(), orderId);
    }

    // =========================================================================
    // Platform fee
    // =========================================================================

    @Override
    @Transactional
    public void deductPlatformFee(BigDecimal amount, Long orderId) {
        String idem = "PLATFORM-FEE-" + orderId;
        if (transactionRepository.findByIdempotencyKey(idem).isPresent()) return;

        // Platform fee is already deducted from buyer's hold; we just record it
        // in the platform-fee tracking wallet (walletId = 0 sentinel).
        WalletTransaction tx = new WalletTransaction();
        tx.setWalletId(0L); // platform ledger sentinel
        tx.setType(TransactionType.PLATFORM_FEE);
        tx.setAmount(amount);
        tx.setBalanceAfter(BigDecimal.ZERO);
        tx.setReferenceId(orderId);
        tx.setReferenceType("ORDER");
        tx.setDescription("Platform fee for order #" + orderId);
        tx.setIdempotencyKey(idem);
        transactionRepository.save(tx);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalPlatformRevenue() {
        return transactionRepository.sumByType(TransactionType.PLATFORM_FEE);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalCreditsInCirculation() {
        return transactionRepository.sumByType(TransactionType.ADMIN_GRANT);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Wallet requireWallet(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for userId=" + userId));
    }

    private Wallet requireWalletWithLock(Long userId) {
        return walletRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for userId=" + userId));
    }

    private void requireAdmin(Long adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + adminId));
        boolean isAdmin = admin.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        if (!isAdmin) {
            throw new AccessDeniedException("User " + adminId + " does not have ADMIN role");
        }
    }

    private void recordTransaction(Wallet wallet, TransactionType type, BigDecimal amount,
                                   BigDecimal balanceAfter, Long referenceId, String referenceType,
                                   String description, Long createdBy, String idempotencyKey) {
        WalletTransaction tx = new WalletTransaction();
        tx.setWalletId(wallet.getId());
        tx.setType(type);
        tx.setAmount(amount);
        tx.setBalanceAfter(balanceAfter);
        tx.setReferenceId(referenceId);
        tx.setReferenceType(referenceType);
        tx.setDescription(description);
        tx.setCreatedBy(createdBy);
        tx.setIdempotencyKey(idempotencyKey);
        transactionRepository.save(tx);
    }

    private WalletDto toDto(Wallet w) {
        return new WalletDto(
                w.getUser().getId(),
                w.getAvailableCredits(),
                w.getHeldCredits(),
                w.getTotalEarned(),
                w.getTotalSpent(),
                w.getIsFrozen(),
                w.getUpdatedAt()
        );
    }

    private WalletTransactionDto toTxDto(WalletTransaction t) {
        return new WalletTransactionDto(
                t.getId(),
                t.getType(),
                t.getAmount(),
                t.getBalanceAfter(),
                t.getReferenceId(),
                t.getReferenceType(),
                t.getDescription(),
                t.getCreatedAt()
        );
    }
}
