package com.markettrust.order.dto;

public record UserSummaryDto(
        Long id,
        String name,
        String email,
        String profileImageUrl
) {}
