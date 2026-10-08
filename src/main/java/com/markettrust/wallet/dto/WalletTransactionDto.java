package com.markettrust.wallet.dto;

import com.markettrust.wallet.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletTransactionDto(
        Long id,
        TransactionType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        Long referenceId,
        String referenceType,
        String description,
        LocalDateTime createdAt
) {}
