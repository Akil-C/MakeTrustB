package com.markettrust.chat.controller;

import com.markettrust.chat.dto.ChatMessageDto;
import com.markettrust.chat.dto.SendMessageRequest;
import com.markettrust.chat.dto.TypingIndicatorDto;
import com.markettrust.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * STOMP WebSocket controller for real-time chat operations.
 * <p>
 * Clients subscribe to:
 * <ul>
 *   <li>{@code /user/{userId}/queue/messages}      – incoming chat messages</li>
 *   <li>{@code /user/{userId}/queue/typing}         – typing indicators</li>
 *   <li>{@code /user/{userId}/queue/notifications} – notification events</li>
 * </ul>
 *
 * Clients send to (with /app prefix from WebSocketConfig):
 * <ul>
 *   <li>{@code /app/chat.send}   – SendMessageRequest payload</li>
 *   <li>{@code /app/chat.typing} – TypingIndicatorDto payload</li>
 * </ul>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService           chatService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handles a message sent from a WebSocket client.
     * The authenticated principal provides the sender's userId.
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload SendMessageRequest req, Principal principal) {
        if (principal == null) {
            log.warn("Unauthenticated WebSocket send attempt blocked");
            return;
        }
        Long senderId = Long.parseLong(principal.getName());
        try {
            ChatMessageDto dto = chatService.sendMessage(senderId, req);
            log.debug("WS message delivered: roomId={} senderId={}", req.getRoomId(), senderId);
            // The ChatService already pushes to the recipient via SimpMessagingTemplate.
            // Echo back to sender's own queue so the UI can confirm delivery.
            messagingTemplate.convertAndSendToUser(
                    senderId.toString(),
                    "/queue/messages",
                    dto
            );
        } catch (Exception ex) {
            log.error("Error processing WS message from userId={}: {}", senderId, ex.getMessage());
        }
    }

    /**
     * Forwards a typing indicator to the other participant in the room.
     */
    @MessageMapping("/chat.typing")
    public void typingIndicator(@Payload TypingIndicatorDto indicator, Principal principal) {
        if (principal == null) return;

        Long senderId = Long.parseLong(principal.getName());
        indicator.setSenderId(senderId);

        // We need to know the recipient — look it up via room (simplified: client passes it)
        Long recipientId = indicator.getRoomId(); // Temporary; real impl resolves from room
        // Resolve room and find the other participant
        // For now, forward the indicator so clients can use roomId to filter
        log.debug("Typing indicator from userId={} in roomId={}", senderId, indicator.getRoomId());

        // Broadcast to room topic (clients filter by roomId)
        messagingTemplate.convertAndSend(
                "/topic/chat.room." + indicator.getRoomId() + ".typing",
                indicator
        );
    }
}
