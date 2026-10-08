package com.markettrust.user.dto;

import com.markettrust.user.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO representing a user — never exposes password or sensitive internals.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private UserStatus status;
    private boolean emailVerified;
    private boolean phoneVerified;
    private String profileImageUrl;

    /** Set of role names, e.g. ["BUYER", "SELLER"]. */
    private Set<String> roles;

    private LocalDateTime createdAt;
}
