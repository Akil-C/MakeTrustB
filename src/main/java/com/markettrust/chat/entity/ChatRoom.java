package com.markettrust.chat.entity;

import com.markettrust.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_rooms", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"buyer_id", "seller_id", "product_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom extends BaseEntity {

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "last_message", columnDefinition = "TEXT")
    private String lastMessage;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "buyer_unread", nullable = false)
    @Builder.Default
    private Integer buyerUnread = 0;

    @Column(name = "seller_unread", nullable = false)
    @Builder.Default
    private Integer sellerUnread = 0;

    @Column(name = "is_blocked", nullable = false)
    @Builder.Default
    private Boolean isBlocked = false;

    @Column(name = "blocked_by")
    private Long blockedBy;

    public Long getBuyerId() { return buyerId; }
    public void setBuyerId(Long buyerId) { this.buyerId = buyerId; }

    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(LocalDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public Integer getBuyerUnread() { return buyerUnread != null ? buyerUnread : 0; }
    public void setBuyerUnread(Integer buyerUnread) { this.buyerUnread = buyerUnread; }

    public Integer getSellerUnread() { return sellerUnread != null ? sellerUnread : 0; }
    public void setSellerUnread(Integer sellerUnread) { this.sellerUnread = sellerUnread; }

    public Boolean getIsBlocked() { return Boolean.TRUE.equals(isBlocked); }
    public void setIsBlocked(Boolean isBlocked) { this.isBlocked = isBlocked; }

    public Long getBlockedBy() { return blockedBy; }
    public void setBlockedBy(Long blockedBy) { this.blockedBy = blockedBy; }
}
