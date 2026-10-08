package com.markettrust.wallet.dto;

import java.math.BigDecimal;

public record PlatformWalletStatsDto(
        BigDecimal totalPlatformRevenue,
        BigDecimal totalCreditsInCirculation,
        BigDecimal totalHeldCredits
) {}
