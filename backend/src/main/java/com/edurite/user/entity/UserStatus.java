package com.edurite.user.entity;

/**
 * This enum represents the possible states (statuses) of a user account.
 *
 * Instead of using random Strings like "active" or "pending",
 * we use an enum to:
 *  - Avoid typos (e.g., "actve")
 *  - Make code safer and easier to understand
 *  - Restrict values to only allowed ones
 */
public enum UserStatus {

    /**
     * The user is fully active and can use the system normally.
     */
    ACTIVE,

    /**
     * The user has registered but has not completed setup yet.
     * Example: email not verified.
     */
    PENDING,

    /**
     * The user is temporarily blocked.
     * Example: policy violation or admin action.
     */
    SUSPENDED,

    /**
     * The user is considered deleted (soft delete).
     * The data may still exist in DB but should not be used.
     */
    DELETED
}
