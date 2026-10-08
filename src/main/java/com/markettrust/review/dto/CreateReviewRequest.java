package com.markettrust.review.dto;

import jakarta.validation.constraints.*;

public record CreateReviewRequest(
        @NotNull Long orderId,
        @NotNull @Min(1) @Max(5) Integer rating,
        @NotBlank String title,
        @NotBlank @Size(min = 10, max = 1000) String comment,
        @Min(1) @Max(5) Integer productConditionRating,
        @Min(1) @Max(5) Integer sellerCommunicationRating,
        @Min(1) @Max(5) Integer accuracyRating
) {}
