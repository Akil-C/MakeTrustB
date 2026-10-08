package com.markettrust.user.entity;

/**
 * Lifecycle status of a user account.
 */
public enum UserStatus {
    /** Account is fully operational. */
    ACTIVE,
    /** Temporarily restricted by an admin. */
    SUSPENDED,
    /** Permanently restricted by an admin. */
    BANNED,
    /** Email address has not yet been confirmed. */
    PENDING_VERIFICATION
}
