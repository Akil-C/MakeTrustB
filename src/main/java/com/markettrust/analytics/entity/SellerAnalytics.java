package com.markettrust.analytics.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "seller_analytics",
        uniqueConstraints = @UniqueConstraint(columnNames = {"seller_id", "date"}),
        indexes = {
                @Index(name = "idx_seller_analytics_seller_id", columnList = "seller_id"),
                @Index(name = "idx_seller_analytics_date", columnList = "date")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class SellerAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "views", nullable = false)
    private Long views = 0L;

    @Column(name = "unique_views", nullable = false)
    private Long uniqueViews = 0L;

    @Column(name = "wishlist_adds", nullable = false)
    private Long wishlistAdds = 0L;

    @Column(name = "inquiries", nullable = false)
    private Long inquiries = 0L;

    @Column(name = "orders", nullable = false)
    private Long orders = 0L;

    @Column(name = "completed_orders", nullable = false)
    private Long completedOrders = 0L;

    @Column(name = "cancelled_orders", nullable = false)
    private Long cancelledOrders = 0L;

    @Column(name = "revenue_credits", nullable = false, precision = 19, scale = 4)
    private BigDecimal revenueCredits = BigDecimal.ZERO;
}
