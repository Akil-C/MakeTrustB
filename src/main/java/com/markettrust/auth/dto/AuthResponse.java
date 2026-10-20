package com.markettrust.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Set;

/**
 * DTO returned after successful authentication (login / register / token refresh).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;

    @Builder.Default
    private String tokenType = "Bearer";

    /** Access token expiry in seconds. */
    private Long expiresIn;

    private UserInfo userInfo;

    @JsonProperty("user")
    public UserInfo getUser() {
        return userInfo;
    }

    // ---------------------------------------------------------------------------
    // Nested DTO
    // ---------------------------------------------------------------------------

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String name;
        private String email;
        private String role;
        private String profileImageUrl;
        private BigDecimal walletBalance;
    }
}
