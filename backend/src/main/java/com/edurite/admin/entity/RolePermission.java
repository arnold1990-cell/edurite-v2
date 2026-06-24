package com.edurite.admin.entity;

// Base class (id, createdAt, updatedAt, etc.)
import com.edurite.common.entity.BaseEntity;

// JPA annotations
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

// UUID for identifying roles
import java.util.UUID;

// Lombok (auto getters & setters)
import lombok.Getter;
import lombok.Setter;

/**
 * This entity represents permissions assigned to a role.
 *
 * It answers the question:
 * "What is this role allowed to do?"
 *
 * Example:
 * Role: ADMIN → can DELETE_USER, CREATE_USER
 * Role: STUDENT → can VIEW_PROFILE only
 */
@Entity
@Table(name = "role_permissions")
@Getter
@Setter
public class RolePermission extends BaseEntity {

    /**
     * The ID of the role this permission belongs to.
     *
     * Example:
     * ADMIN role ID
     */
    @Column(nullable = false)
    private UUID roleId;

    /**
     * The permission code.
     *
     * Example values:
     * - "USER_CREATE"
     * - "USER_DELETE"
     * - "BURSARY_POST"
     */
    @Column(nullable = false)
    private String permissionCode;

    /**
     * Whether this permission is currently active.
     *
     * Allows enabling/disabling permissions without deleting them.
     */
    @Column(nullable = false)
    private boolean active = true;
}
