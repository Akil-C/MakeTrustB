package com.markettrust.advertisement.entity;

import com.markettrust.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "advertisements", indexes = {
        @Index(name = "idx_advertisement_status", columnList = "status"),
        @Index(name = "idx_advertisement_placement", columnList = "placement"),
        @Index(name = "idx_advertisement_target_category_id", columnList = "target_category_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Advertisement extends BaseEntity {

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "target_url", nullable = false)
    private String targetUrl;

    @Column(name = "target_category_id")
    private Long targetCategoryId;

    @Column(name = "target_city", length = 100)
    private String targetCity;

    @Column(name = "target_state", length = 100)
    private String targetState;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "priority", nullable = false)
    private Integer priority = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AdStatus status = AdStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "placement", nullable = false, length = 30)
    private AdPlacement placement;

    @Column(name = "impressions", nullable = false)
    private Long impressions = 0L;

    @Column(name = "clicks", nullable = false)
    private Long clicks = 0L;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;
}
