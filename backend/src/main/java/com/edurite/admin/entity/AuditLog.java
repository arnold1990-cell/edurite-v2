package com.edurite.admin.entity;

// Base class (likely contains id, createdAt, updatedAt)
import com.edurite.common.entity.BaseEntity;

// Used to store JSON data (flexible structure)
import com.fasterxml.jackson.databind.JsonNode;

// JPA annotations
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

// UUID for identifying users/entities
import java.util.UUID;

// Lombok (auto getters & setters)
import lombok.Getter;
import lombok.Setter;

// Hibernate-specific annotation for JSON column support
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * This entity represents an audit log entry.
 *
 * It records important actions performed in the system,
 * such as:
 * - User deleted account
 * - Admin updated user
 * - Password changed
 *
 * This is useful for:
 * - Debugging
 * - Security tracking
 * - Compliance (who did what and when)
 */
@Entity
@Table(name = "audit_logs") // Table name in database
@Getter
@Setter
public class AuditLog extends BaseEntity {

    /**
     * The ID of the user who performed the action.
     * Example: admin ID or normal user ID
     */
    private UUID actorId;

    /**
     * The action that was performed.
     * Example:
     * - "DELETE_ACCOUNT"
     * - "UPDATE_USER"
     */
    @Column(nullable = false)
    private String action;
    /**
     * The type of entity affected.
     * Example:
     * - "User"
     * - "Order"
     */
    private String entityType;
    /**
     * The ID of the entity affected.
     * Example:
     * - userId
     * - orderId
     */
    private UUID entityId;

    /**
     * Additional details stored as JSON.
     *
     * This allows flexible data like:
     * {
     *   "oldEmail": "old@mail.com",
     *   "newEmail": "new@mail.com"
     * }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode details;
}
