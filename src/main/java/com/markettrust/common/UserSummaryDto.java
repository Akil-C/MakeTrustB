package com.markettrust.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDto {
    private Long id;
    private String name;
    private String email;
    private String profileImageUrl;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public static UserSummaryDtoBuilder builder() {
        return new UserSummaryDtoBuilder();
    }

    public static class UserSummaryDtoBuilder {
        private Long id;
        private String name;
        private String email;
        private String profileImageUrl;

        public UserSummaryDtoBuilder id(Long id) { this.id = id; return this; }
        public UserSummaryDtoBuilder name(String name) { this.name = name; return this; }
        public UserSummaryDtoBuilder email(String email) { this.email = email; return this; }
        public UserSummaryDtoBuilder profileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; return this; }

        public UserSummaryDto build() {
            UserSummaryDto dto = new UserSummaryDto();
            dto.setId(id);
            dto.setName(name);
            dto.setEmail(email);
            dto.setProfileImageUrl(profileImageUrl);
            return dto;
        }
    }
}
