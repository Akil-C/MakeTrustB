package com.markettrust.wallet.repository;

import com.markettrust.wallet.entity.WalletTransaction;
import com.markettrust.wallet.entity.TransactionType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    List<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId, Pageable pageable);

    Optional<WalletTransaction> findByIdempotencyKey(String key);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM WalletTransaction t WHERE t.type = :type")
    BigDecimal sumByType(@Param("type") TransactionType type);

    long countByWalletId(Long walletId);

    List<WalletTransaction> findByReferenceIdAndReferenceType(Long referenceId, String referenceType);
}
