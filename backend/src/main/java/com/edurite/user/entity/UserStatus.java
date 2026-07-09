package com.edurite.user.entity;

/**
 * Lifecycle states for a user account.
 */
public enum UserStatus {

    // Account is active and can access the system.
    ACTIVE,

    // Account has been created but is awaiting activation or verification.
    PENDING,

    // Account access has been temporarily disabled.
    SUSPENDED,

    // Soft-deleted account retained for auditing purposes.
    DELETED
}