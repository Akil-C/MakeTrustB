package com.markettrust.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO projection of a {@link com.markettrust.notification.entity.Notification}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {

    private Long id;
    private String type;
    private String title;
    private String message;

    /** ID of the linked entity (stored as String for flexibility). */
    private Long referenceId;
    private String referenceType;

    private Boolean isRead;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }

    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }

    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static NotificationDtoBuilder builder() { return new NotificationDtoBuilder(); }

    public static class NotificationDtoBuilder {
        private Long id;
        private String type;
        private String title;
        private String message;
        private Long referenceId;
        private String referenceType;
        private Boolean isRead;
        private LocalDateTime createdAt;

        public NotificationDtoBuilder id(Long id) { this.id = id; return this; }
        public NotificationDtoBuilder type(String type) { this.type = type; return this; }
        public NotificationDtoBuilder title(String title) { this.title = title; return this; }
        public NotificationDtoBuilder message(String message) { this.message = message; return this; }
        public NotificationDtoBuilder referenceId(Long referenceId) { this.referenceId = referenceId; return this; }
        public NotificationDtoBuilder referenceType(String referenceType) { this.referenceType = referenceType; return this; }
        public NotificationDtoBuilder isRead(Boolean isRead) { this.isRead = isRead; return this; }
        public NotificationDtoBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public NotificationDto build() {
            NotificationDto dto = new NotificationDto();
            dto.setId(id);
            dto.setType(type);
            dto.setTitle(title);
            dto.setMessage(message);
            dto.setReferenceId(referenceId);
            dto.setReferenceType(referenceType);
            dto.setIsRead(isRead);
            dto.setCreatedAt(createdAt);
            return dto;
        }
    }
}
