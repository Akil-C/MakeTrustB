package com.markettrust.order.dto;

import com.markettrust.order.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull OrderStatus newStatus,
        String notes,
        String trackingNumber
) {}
