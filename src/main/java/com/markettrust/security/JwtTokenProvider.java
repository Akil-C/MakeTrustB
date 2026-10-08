package com.markettrust.security;

import com.markettrust.user.entity.User;

/**
 * Contract for JWT generation and validation within MarketTrust.
 * The concrete implementation lives in {@code JwtTokenProviderImpl}.
 */
public interface JwtTokenProvider {

    /**
     * Generates a short-lived access token for the given user.
     *
     * @param user the authenticated user
     * @return signed JWT string
     */
    String generateAccessToken(User user);

    /**
     * Validates a JWT access token.
     *
     * @param token the raw JWT string
     * @return {@code true} if valid and not expired
     */
    boolean validateToken(String token);

    /**
     * Extracts the user's email (subject) from a validated JWT.
     *
     * @param token the raw JWT string
     * @return email address
     */
    String getEmailFromToken(String token);

    /**
     * Returns the configured access-token expiry duration in seconds.
     */
    long getAccessTokenExpirySeconds();
}
