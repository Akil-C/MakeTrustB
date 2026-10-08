package com.markettrust.notification.service;

import com.markettrust.chat.dto.ChatMessageDto;
import com.markettrust.exception.ResourceNotFoundException;
import com.markettrust.notification.dto.NotificationDto;
import com.markettrust.notification.entity.Notification;
import com.markettrust.notification.repository.NotificationRepository;
import com.markettrust.order.entity.Order;
import com.markettrust.order.entity.OrderStatus;
import com.markettrust.seller.entity.KycStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles creation, persistence, and real-time delivery of notifications.
 * <p>
 * Every notification is first saved to the database (durability), then an
 * attempt is made to push it over WebSocket. A failure to push does not
 * roll back the database save.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate  messagingTemplate;

    // ---------------------------------------------------------------------------
    // Core create / deliver
    // ---------------------------------------------------------------------------

    /**
     * Persists a new notification and attempts real-time delivery.
     */
    @Transactional
    public NotificationDto createNotification(
            Long userId, String type, String title, String message,
            Long referenceId, String referenceType) {

        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setReferenceId(referenceId);
        n.setReferenceType(referenceType);
        n.setIsRead(false);
        n = notificationRepository.save(n);

        NotificationDto dto = toDto(n);
        sendRealTimeNotification(userId, dto);
        return dto;
    }

    /**
     * Pushes a notification DTO over WebSocket to the user's private queue.
     * Errors are swallowed — the database record already guarantees delivery.
     */
    public void sendRealTimeNotification(Long userId, NotificationDto dto) {
        try {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications",
                    dto
            );
        } catch (Exception ex) {
            log.warn("Failed to push real-time notification to userId={}: {}", userId, ex.getMessage());
        }
    }

    // ---------------------------------------------------------------------------
    // Query
    // ---------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<NotificationDto> getMyNotifications(Long userId, Pageable pageable) {
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    // ---------------------------------------------------------------------------
    // Mark read
    // ---------------------------------------------------------------------------

    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        if (!n.getUserId().equals(userId)) {
            throw new SecurityException("User " + userId + " does not own notification " + notificationId);
        }
        n.setIsRead(true);
        notificationRepository.save(n);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    // ---------------------------------------------------------------------------
    // Domain-specific helpers
    // ---------------------------------------------------------------------------

    public void notifyOrderCreated(Order order) {
        createNotification(
                order.getBuyerId(),
                "ORDER_CREATED",
                "Order Placed",
                "Your order #" + order.getOrderNumber() + " has been placed successfully.",
                order.getId(), "ORDER"
        );
        createNotification(
                order.getSellerId(),
                "NEW_ORDER",
                "New Order Received",
                "You have received a new order #" + order.getOrderNumber() + ".",
                order.getId(), "ORDER"
        );
    }

    public void notifyOrderStatusChanged(Order order, OrderStatus newStatus) {
        String title = "Order Update";
        String msg   = "Your order #" + order.getOrderNumber() + " status changed to " + newStatus.name() + ".";
        createNotification(order.getBuyerId(), "ORDER_STATUS_CHANGED", title, msg, order.getId(), "ORDER");
    }

    public void notifyKycStatusChanged(Long userId, KycStatus status) {
        String title = "KYC Status Updated";
        String msg   = "Your KYC verification status has been updated to: " + status.name() + ".";
        createNotification(userId, "KYC_STATUS_CHANGED", title, msg, userId, "KYC");
    }

    public void notifyReviewUnlocked(Long userId, Long orderId) {
        createNotification(
                userId,
                "REVIEW_UNLOCKED",
                "You Can Now Leave a Review",
                "Your completed order is eligible for a review. Share your experience!",
                orderId, "ORDER"
        );
    }

    public void notifyNewMessage(Long userId, ChatMessageDto message) {
        createNotification(
                userId,
                "NEW_MESSAGE",
                "New Message",
                "You have a new message.",
                message.getRoomId(), "CHAT_ROOM"
        );
    }

    public void notifyPriceDrop(Long userId, Long productId, Long oldPrice, Long newPrice) {
        createNotification(
                userId,
                "PRICE_DROP",
                "Price Drop Alert",
                String.format("A product on your wishlist dropped from %d to %d credits!", oldPrice, newPrice),
                productId, "PRODUCT"
        );
    }

    public void notifyAiInsightReady(Long userId) {
        createNotification(
                userId,
                "AI_INSIGHT_READY",
                "Your AI Insights Are Ready",
                "New AI-powered insights are available for your seller dashboard.",
                userId, "SELLER"
        );
    }

    // ---------------------------------------------------------------------------
    // Mapping
    // ---------------------------------------------------------------------------

    private NotificationDto toDto(Notification n) {
        return NotificationDto.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .referenceId(n.getReferenceId())
                .referenceType(n.getReferenceType())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
