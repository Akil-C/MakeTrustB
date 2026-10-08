package com.markettrust.chat.dto;

import com.markettrust.chat.entity.MessageType;
import com.markettrust.common.UserSummaryDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private Long id;
    private Long roomId;
    private UserSummaryDto sender;
    private String content;
    private MessageType messageType;
    private Boolean isRead;
    private Boolean isFlagged;
    private LocalDateTime createdAt;
}
