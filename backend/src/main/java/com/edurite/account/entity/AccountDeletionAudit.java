package com.edurite.account.entity;

// Base class that likely contains id, createdAt, updatedAt
import com.edurite.common.entity.BaseEntity;

// JPA annotations for database mapping
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

// Used to store date + time with timezone
import java.time.OffsetDateTime;

// UUID is used as an identifier (more secure than simple numbers)
import java.util.UUID;

// Lombok to generate getters and setters automatically
import lombok.Getter;
import lombok.Setter;

/**
 * This entity is used to store a history (audit) of deleted accounts.
 *
 * Instead of permanently losing information when a user is deleted,
 * we record important details here for tracking, debugging, or compliance.
 */
@Entity // Marks this class as a database table
@Table(name = "account_deletion_audit") // Table name in the database
@Getter
@Setter
public class AccountDeletionAudit extends BaseEntity {

    /**
     * Stores the ID of the user that was deleted.
     *
     * We use UUID instead of directly linking to User entity
     * to keep a simple record even if the user data changes later.
     */
    @Column(nullable = false)
    private UUID userId;

    /**
     * Stores the reason why the account was deleted.
     * Example:
     * - "User requested deletion"
     * - "Violation of policy"
     */
    private String reason;

    /**
     * Stores the exact time when the account was deleted.
     *
     * This field is required (cannot be null).
     */
    @Column(nullable = false)
    private OffsetDateTime deletedAt;
}
