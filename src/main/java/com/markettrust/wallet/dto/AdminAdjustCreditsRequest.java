package com.markettrust.wallet.dto;

public record AdminAdjustCreditsRequest(
        Long targetUserId,
        Long amount,   // may be negative
        String reason
) {}
