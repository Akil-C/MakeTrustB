package com.markettrust.chat.dto;

import lombok.Data;

/**
 * WebSocket typing-indicator payload.
 */
@Data
public class TypingIndicatorDto {

    private Long roomId;
    private Long senderId;
    private String senderName;
    private Boolean isTyping;
}
