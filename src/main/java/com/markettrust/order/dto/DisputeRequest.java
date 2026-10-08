package com.markettrust.order.dto;

import jakarta.validation.constraints.NotBlank;

public record DisputeRequest(
        @NotBlank String reason,
        String evidenceDescription
) {}
