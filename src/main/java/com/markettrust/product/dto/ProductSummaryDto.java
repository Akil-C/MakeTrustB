package com.markettrust.product.dto;

import com.markettrust.product.entity.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Lightweight product card DTO for search results and listing pages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSummaryDto {

    private Long             id;
    private String           title;
    private Long             priceInCredits;
    private String           condition;
    private String           city;
    private ProductStatus    status;
    private String           primaryImageUrl;
    private SellerSummaryDto seller;

    /** Populated only when result is from a nearby query (kilometres). */
    private Double           distance;

    private LocalDateTime    createdAt;
}
