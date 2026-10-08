package com.markettrust.review.dto;

import com.markettrust.order.dto.UserSummaryDto;
import com.markettrust.review.entity.ReviewStatus;

import java.time.LocalDateTime;

public record ReviewDto(
        Long id,
        Long orderId,
        Long productId,
        String productTitle,
        UserSummaryDto buyer,
        Integer rating,
        String title,
        String comment,
        Integer productConditionRating,
        Integer sellerCommunicationRating,
        Integer accuracyRating,
        Boolean isVerifiedPurchase,
        ReviewStatus status,
        String sellerReply,
        LocalDateTime sellerRepliedAt,
        LocalDateTime createdAt
) {}
