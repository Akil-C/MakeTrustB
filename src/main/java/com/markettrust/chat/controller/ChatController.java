package com.markettrust.chat.controller;

import com.markettrust.chat.dto.*;
import com.markettrust.chat.service.ChatService;
import com.markettrust.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for chat-room and message management.
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ChatController {

    private final ChatService chatService;

    // ---------------------------------------------------------------------------
    // Chat rooms
    // ---------------------------------------------------------------------------

    /**
     * POST /api/chat/rooms
     * Start or retrieve a chat room between the authenticated buyer and a seller.
     */
    @PostMapping("/rooms")
    public ResponseEntity<ChatRoomDto> startChat(
            @Valid @RequestBody StartChatRequest req) {

        Long buyerId = SecurityUtils.getCurrentUserId();
        ChatRoomDto dto = chatService.startOrGetChatRoom(buyerId, req.getSellerId(), req.getProductId());
        return ResponseEntity.ok(dto);
    }

    /**
     * GET /api/chat/rooms
     * List all chat rooms for the authenticated user.
     */
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomDto>> getMyChatRooms() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(chatService.getMyChatRooms(userId));
    }

    // ---------------------------------------------------------------------------
    // Messages
    // ---------------------------------------------------------------------------

    /**
     * GET /api/chat/rooms/{roomId}/messages
     * Fetch messages for a room (marks them as read).
     */
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatMessageDto>> getRoomMessages(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {

        Long userId = SecurityUtils.getCurrentUserId();
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        return ResponseEntity.ok(chatService.getRoomMessages(userId, roomId, pageable));
    }

    /**
     * POST /api/chat/rooms/{roomId}/messages
     * Send a message via REST (fallback for non-WebSocket clients).
     */
    @PostMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ChatMessageDto> sendMessage(
            @PathVariable Long roomId,
            @Valid @RequestBody SendMessageRequest req) {

        req.setRoomId(roomId);
        Long senderId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(chatService.sendMessage(senderId, req));
    }

    /**
     * POST /api/chat/rooms/{roomId}/read
     * Mark all messages in the room as read for the requesting user.
     */
    @PostMapping("/rooms/{roomId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long roomId) {
        Long userId = SecurityUtils.getCurrentUserId();
        chatService.markAsRead(userId, roomId);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/chat/rooms/{roomId}/block
     * Block a chat room.
     */
    @PostMapping("/rooms/{roomId}/block")
    public ResponseEntity<Void> blockRoom(@PathVariable Long roomId) {
        Long userId = SecurityUtils.getCurrentUserId();
        chatService.blockRoom(userId, roomId);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/chat/messages/flag
     * Flag a message as suspicious or inappropriate.
     */
    @PostMapping("/messages/flag")
    public ResponseEntity<ChatMessageDto> flagMessage(@Valid @RequestBody FlagMessageRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(chatService.flagMessage(userId, req.getMessageId(), req.getReason()));
    }
}
