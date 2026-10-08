package com.markettrust.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Stores opaque refresh tokens tied to a user.
 * Single-session strategy: only one active token per user at a time.
 */
@Entity
@Table(
    name = "refresh_tokens",
    indexes = @Index(name = "idx_refresh_token_value", columnList = "token", unique = true)
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant expiresAt;
}
