package com.markettrust.chat.service;

import com.markettrust.chat.dto.*;
import com.markettrust.chat.entity.ChatMessage;
import com.markettrust.chat.entity.ChatRoom;
import com.markettrust.chat.entity.MessageType;
import com.markettrust.chat.repository.ChatMessageRepository;
import com.markettrust.chat.repository.ChatRoomRepository;
import com.markettrust.product.dto.ProductSummaryDto;
import com.markettrust.common.UserSummaryDto;
import com.markettrust.exception.ResourceNotFoundException;
import com.markettrust.product.entity.Product;
import com.markettrust.product.repository.ProductRepository;
import com.markettrust.user.entity.User;
import com.markettrust.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Core business logic for chat rooms and messages.
 * <p>
 * Security contract: every public method receives the authenticated userId
 * and must validate that the caller is authorised to operate on the resource.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChatService.class);

    // ---------------------------------------------------------------------------
    // Suspicious off-platform payment keywords (lower-cased for comparison)
    // ---------------------------------------------------------------------------
    private static final Set<String> SUSPICIOUS_KEYWORDS = Set.of(
            "whatsapp", "upi", "gpay", "phonepe", "paytm",
            "bank transfer", "share otp", "outside", "direct payment"
    );

    private final ChatRoomRepository    chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository        userRepository;
    private final ProductRepository     productRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // ---------------------------------------------------------------------------
    // Room management
    // ---------------------------------------------------------------------------

    /**
     * Finds an existing chat room or creates a new one for the buyer → seller conversation
     * about a specific product.
     */
    @Transactional
    public ChatRoomDto startOrGetChatRoom(Long buyerId, Long sellerId, Long productId) {
        ChatRoom room = chatRoomRepository
                .findByBuyerIdAndSellerIdAndProductId(buyerId, sellerId, productId)
                .orElseGet(() -> {
                    ChatRoom newRoom = new ChatRoom();
                    newRoom.setBuyerId(buyerId);
                    newRoom.setSellerId(sellerId);
                    newRoom.setProductId(productId);
                    log.info("Creating new chat room buyerId={} sellerId={} productId={}", buyerId, sellerId, productId);
                    return chatRoomRepository.save(newRoom);
                });

        User buyer  = loadUser(buyerId);
        User seller = loadUser(sellerId);
        Product product = productId != null ? loadProduct(productId) : null;

        // Perspective: buyer is requesting, so "other" is seller
        return toChatRoomDto(room, seller, product, room.getBuyerUnread());
    }

    /**
     * Returns all chat rooms the user participates in, ordered by most recent message.
     */
    @Transactional(readOnly = true)
    public List<ChatRoomDto> getMyChatRooms(Long userId) {
        List<ChatRoom> rooms = chatRoomRepository
                .findByBuyerIdOrSellerIdOrderByLastMessageAtDesc(userId, userId);

        return rooms.stream()
                .map(room -> buildChatRoomDtoForUser(room, userId))
                .collect(Collectors.toList());
    }

    /**
     * Returns paginated messages for a room and marks them as read for the requesting user.
     */
    @Transactional
    public List<ChatMessageDto> getRoomMessages(Long userId, Long roomId, Pageable pageable) {
        ChatRoom room = loadRoom(roomId);
        assertMember(userId, room);

        // Mark messages from the other party as read
        markAsRead(userId, roomId);

        List<ChatMessage> messages = chatMessageRepository.findByRoomIdOrderByCreatedAtAsc(roomId);
        return messages.stream().map(this::toChatMessageDto).collect(Collectors.toList());
    }

    /**
     * Marks all unread messages in the room as read for the requesting user,
     * and resets the unread counter on the room entity.
     */
    @Transactional
    public void markAsRead(Long userId, Long roomId) {
        ChatRoom room = loadRoom(roomId);
        assertMember(userId, room);

        // Reset unread counter
        if (userId.equals(room.getBuyerId())) {
            room.setBuyerUnread(0);
        } else {
            room.setSellerUnread(0);
        }
        chatRoomRepository.save(room);

        // Mark individual messages
        chatMessageRepository.findByRoomIdOrderByCreatedAtAsc(roomId).stream()
                .filter(m -> !m.getSenderId().equals(userId) && Boolean.FALSE.equals(m.getIsRead()))
                .forEach(m -> {
                    m.setIsRead(true);
                    chatMessageRepository.save(m);
                });
    }

    /**
     * Blocks a chat room. Only participants can block.
     */
    @Transactional
    public void blockRoom(Long userId, Long roomId) {
        ChatRoom room = loadRoom(roomId);
        assertMember(userId, room);
        room.setIsBlocked(true);
        room.setBlockedBy(userId);
        chatRoomRepository.save(room);
        log.info("Chat room {} blocked by userId={}", roomId, userId);
    }

    /**
     * Unblocks a chat room. Admin-only operation.
     */
    @Transactional
    public void unblockRoom(Long adminId, Long roomId) {
        ChatRoom room = loadRoom(roomId);
        room.setIsBlocked(false);
        room.setBlockedBy(null);
        chatRoomRepository.save(room);
        log.info("Chat room {} unblocked by adminId={}", roomId, adminId);
    }

    // ---------------------------------------------------------------------------
    // Message operations
    // ---------------------------------------------------------------------------

    /**
     * Sends a chat message:
     * <ol>
     *   <li>Validates sender membership and room status</li>
     *   <li>Checks for suspicious off-platform content</li>
     *   <li>Persists the message</li>
     *   <li>Updates room metadata and unread counters</li>
     *   <li>Pushes the message over WebSocket to the recipient</li>
     * </ol>
     */
    @Transactional
    public ChatMessageDto sendMessage(Long senderId, SendMessageRequest req) {
        ChatRoom room = loadRoom(req.getRoomId());
        assertMember(senderId, room);

        if (Boolean.TRUE.equals(room.getIsBlocked())) {
            throw new IllegalStateException("This chat room is blocked and cannot receive messages");
        }

        boolean suspicious = checkSuspiciousContent(req.getContent());

        ChatMessage msg = new ChatMessage();
        msg.setRoomId(room.getId());
        msg.setSenderId(senderId);
        msg.setContent(req.getContent());
        msg.setMessageType(req.getType() != null ? req.getType() : MessageType.TEXT);
        msg.setIsRead(false);
        msg.setIsFlagged(suspicious);
        if (suspicious) {
            msg.setFlagReason("Auto-flagged: suspicious off-platform content detected");
        }
        msg = chatMessageRepository.save(msg);

        // Update room summary
        room.setLastMessage(truncate(req.getContent(), 200));
        room.setLastMessageAt(LocalDateTime.now());

        Long recipientId = senderId.equals(room.getBuyerId()) ? room.getSellerId() : room.getBuyerId();
        if (recipientId.equals(room.getBuyerId())) {
            room.setBuyerUnread(room.getBuyerUnread() + 1);
        } else {
            room.setSellerUnread(room.getSellerUnread() + 1);
        }
        chatRoomRepository.save(room);

        ChatMessageDto dto = toChatMessageDto(msg);

        // Real-time delivery
        messagingTemplate.convertAndSendToUser(
                recipientId.toString(),
                "/queue/messages",
                dto
        );

        if (suspicious) {
            log.warn("Suspicious message flagged: messageId={} senderId={} roomId={}", msg.getId(), senderId, room.getId());
        }

        return dto;
    }

    /**
     * Flags a message as inappropriate or suspicious.
     */
    @Transactional
    public ChatMessageDto flagMessage(Long reporterId, Long messageId, String reason) {
        ChatMessage msg = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("ChatMessage", "id", messageId));

        ChatRoom room = loadRoom(msg.getRoomId());
        assertMember(reporterId, room);

        msg.setIsFlagged(true);
        msg.setFlagReason(reason);
        msg = chatMessageRepository.save(msg);
        log.info("Message {} flagged by userId={} reason={}", messageId, reporterId, reason);
        return toChatMessageDto(msg);
    }

    /**
     * Scans message content for known off-platform payment keywords.
     *
     * @param content the raw message text
     * @return {@code true} if suspicious keywords are detected
     */
    public boolean checkSuspiciousContent(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }
        String lower = content.toLowerCase();
        return SUSPICIOUS_KEYWORDS.stream().anyMatch(lower::contains);
    }

    /**
     * Increments the inquiry count on a product (for analytics).
     */
    @Transactional
    public void incrementInquiryCount(Long productId) {
        productRepository.findById(productId).ifPresent(p -> {
            p.setInquiryCount(p.getInquiryCount() + 1);
            productRepository.save(p);
        });
    }

    // ---------------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------------

    private ChatRoom loadRoom(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("ChatRoom", "id", roomId));
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private Product loadProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
    }

    private void assertMember(Long userId, ChatRoom room) {
        if (!userId.equals(room.getBuyerId()) && !userId.equals(room.getSellerId())) {
            throw new SecurityException("User " + userId + " is not a member of chat room " + room.getId());
        }
    }

    private ChatRoomDto buildChatRoomDtoForUser(ChatRoom room, Long userId) {
        boolean isBuyer = userId.equals(room.getBuyerId());
        Long otherId     = isBuyer ? room.getSellerId() : room.getBuyerId();
        Integer unread   = isBuyer ? room.getBuyerUnread() : room.getSellerUnread();

        User other   = userRepository.findById(otherId).orElse(null);
        Product prod = room.getProductId() != null
                ? productRepository.findById(room.getProductId()).orElse(null)
                : null;

        return toChatRoomDto(room, other, prod, unread);
    }

    private ChatRoomDto toChatRoomDto(ChatRoom room, User other, Product product, Integer unread) {
        UserSummaryDto otherDto = other == null ? null : UserSummaryDto.builder()
                .id(other.getId())
                .name(other.getName())
                .profileImageUrl(other.getProfileImageUrl())
                .build();

        ProductSummaryDto productDto = product == null ? null : ProductSummaryDto.builder()
                .id(product.getId())
                .title(product.getTitle())
                .priceInCredits(product.getPriceInCredits())
                .status(product.getStatus())
                .condition(product.getCondition() != null ? product.getCondition().name() : null)
                .build();

        return ChatRoomDto.builder()
                .id(room.getId())
                .otherUser(otherDto)
                .product(productDto)
                .lastMessage(room.getLastMessage())
                .lastMessageAt(room.getLastMessageAt())
                .unreadCount(unread)
                .isBlocked(room.getIsBlocked())
                .build();
    }

    private ChatMessageDto toChatMessageDto(ChatMessage msg) {
        User sender = userRepository.findById(msg.getSenderId()).orElse(null);
        UserSummaryDto senderDto = sender == null ? null : UserSummaryDto.builder()
                .id(sender.getId())
                .name(sender.getName())
                .profileImageUrl(sender.getProfileImageUrl())
                .build();

        return ChatMessageDto.builder()
                .id(msg.getId())
                .roomId(msg.getRoomId())
                .sender(senderDto)
                .content(msg.getContent())
                .messageType(msg.getMessageType())
                .isRead(msg.getIsRead())
                .isFlagged(msg.getIsFlagged())
                .createdAt(msg.getCreatedAt())
                .build();
    }

    private String truncate(String text, int max) {
        if (text == null) return null;
        return text.length() <= max ? text : text.substring(0, max) + "…";
    }
}
