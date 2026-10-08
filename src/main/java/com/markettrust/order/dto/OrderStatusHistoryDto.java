package com.markettrust.order.dto;

import java.time.LocalDateTime;

public record OrderStatusHistoryDto(
        Long id,
        String status,
        String notes,
        Long changedBy,
        LocalDateTime createdAt
) {}
