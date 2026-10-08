package com.markettrust.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
        @NotNull Long productId,
        @NotNull @Min(1) Integer quantity,
        @NotBlank String deliveryAddress,
        String notes,
        @NotBlank String idempotencyKey
) {}
