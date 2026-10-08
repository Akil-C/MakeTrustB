package com.markettrust.wallet.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletDto(
        Long userId,
        BigDecimal availableCredits,
        BigDecimal heldCredits,
        BigDecimal totalEarned,
        BigDecimal totalSpent,
        Boolean isFrozen,
        LocalDateTime lastUpdated
) {}
