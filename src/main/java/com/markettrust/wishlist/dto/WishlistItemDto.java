package com.markettrust.wishlist.dto;

import com.markettrust.product.dto.ProductSummaryDto;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WishlistItemDto(
        Long id,
        ProductSummaryDto product,
        BigDecimal priceAtAdd,
        BigDecimal currentPrice,
        boolean priceDrop,
        BigDecimal priceDropAmount,
        LocalDateTime addedAt
) {}
