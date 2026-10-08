package com.markettrust.chat.dto;

import com.markettrust.common.UserSummaryDto;
import com.markettrust.product.dto.ProductSummaryDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomDto {
    private Long id;
    private UserSummaryDto otherUser;
    private ProductSummaryDto product;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private Integer unreadCount;
    private Boolean isBlocked;
}
