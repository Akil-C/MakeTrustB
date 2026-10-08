package com.markettrust.order.dto;

import com.markettrust.order.entity.OrderStatus;
import com.markettrust.product.dto.ProductSummaryDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDto(
        Long id,
        String orderNumber,
        UserSummaryDto buyer,
        UserSummaryDto seller,
        ProductSummaryDto product,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        BigDecimal platformFee,
        BigDecimal sellerAmount,
        OrderStatus status,
        String deliveryAddress,
        String trackingNumber,
        String notes,
        List<OrderStatusHistoryDto> statusHistory,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        /** true when buyer can leave a review (BUYER_CONFIRMED/COMPLETED and no review yet) */
        boolean isReviewable
) {}
