package com.markettrust.wallet.entity;

import com.markettrust.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "credit_holds")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditHold extends BaseEntity {

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Column(name = "order_id", length = 100)
    private String orderId;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private HoldStatus status = HoldStatus.ACTIVE;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    public Long getWalletId() { return walletId; }
    public void setWalletId(Long walletId) { this.walletId = walletId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public HoldStatus getStatus() { return status; }
    public void setStatus(HoldStatus status) { this.status = status; }

    public LocalDateTime getReleasedAt() { return releasedAt; }
    public void setReleasedAt(LocalDateTime releasedAt) { this.releasedAt = releasedAt; }
}
