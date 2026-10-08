package com.markettrust.wallet.entity;

import com.markettrust.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a user's in-platform wallet (credit balance).
 */
@Entity
@Table(name = "wallets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Column(name = "available_credits", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal availableCredits = BigDecimal.ZERO;

    @Column(name = "held_credits", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal heldCredits = BigDecimal.ZERO;

    @Column(name = "total_earned", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalEarned = BigDecimal.ZERO;

    @Column(name = "total_spent", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalSpent = BigDecimal.ZERO;

    @Column(name = "is_frozen", nullable = false)
    @Builder.Default
    private Boolean isFrozen = false;

    @Version
    private Long version;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public BigDecimal getAvailableCredits() { return availableCredits != null ? availableCredits : BigDecimal.ZERO; }
    public void setAvailableCredits(BigDecimal availableCredits) { this.availableCredits = availableCredits; }

    public BigDecimal getHeldCredits() { return heldCredits != null ? heldCredits : BigDecimal.ZERO; }
    public void setHeldCredits(BigDecimal heldCredits) { this.heldCredits = heldCredits; }

    public BigDecimal getTotalEarned() { return totalEarned != null ? totalEarned : BigDecimal.ZERO; }
    public void setTotalEarned(BigDecimal totalEarned) { this.totalEarned = totalEarned; }

    public BigDecimal getTotalSpent() { return totalSpent != null ? totalSpent : BigDecimal.ZERO; }
    public void setTotalSpent(BigDecimal totalSpent) { this.totalSpent = totalSpent; }

    public Boolean getIsFrozen() { return Boolean.TRUE.equals(isFrozen); }
    public void setIsFrozen(Boolean isFrozen) { this.isFrozen = isFrozen; }

    public BigDecimal getBalance() { return getAvailableCredits(); }
    public void setBalance(BigDecimal balance) { setAvailableCredits(balance); }
}
