package com.markettrust.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight user projection used across multiple modules
 * (Chat, Report, Dispute, etc.) to avoid exposing full User entities.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDto {

    private Long id;
    private String name;
    private String profileImageUrl;
    private Double averageRating;
    private Boolean isVerified;
}
