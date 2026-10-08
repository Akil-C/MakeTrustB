package com.markettrust.buyer.entity;

import com.markettrust.common.BaseEntity;
import com.markettrust.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "buyer_profiles", indexes = {
        @Index(name = "idx_buyer_profile_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
public class BuyerProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "display_name", length = 150)
    private String displayName;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "buyer_level", nullable = false, length = 30)
    private BuyerLevel buyerLevel = BuyerLevel.NEW_BUYER;

    @Column(name = "total_purchases", nullable = false)
    private Integer totalPurchases = 0;

    @Column(name = "total_spent", precision = 19, scale = 4, nullable = false)
    private BigDecimal totalSpent = BigDecimal.ZERO;
}
