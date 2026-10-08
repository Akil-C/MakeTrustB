package com.markettrust.wishlist.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Summary of a single product used within WishlistItemDto.
 * Kept as a standalone record so it can also be reused by Order / Review modules.
 */
public record ProductSummaryDto(
        Long id,
        String title,
        BigDecimal priceInCredits,
        String primaryImageUrl,
        String status,
        String sellerDisplayName,
        Long sellerId
) {}
