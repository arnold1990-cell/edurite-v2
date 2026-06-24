package com.edurite.admin.repository;

// Import the entity (role_permissions table)
import com.edurite.admin.entity.RolePermission;

// Java utilities
import java.util.List;
import java.util.UUID;

// Spring Data JPA
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * This repository handles database operations for role_permissions.
 *
 * It allows:
 * - Fetching permissions for a role
 * - Deleting permissions for a role
 */
public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {

    /**
     * Gets all permissions for a specific role.
     *
     * Orders them alphabetically by permissionCode.
     *
     * Example:
     * findByRoleIdOrderByPermissionCodeAsc(adminRoleId)
     *
     * Equivalent SQL:
     * SELECT * FROM role_permissions
     * WHERE role_id = ?
     * ORDER BY permission_code ASC;
     */
    List<RolePermission> findByRoleIdOrderByPermissionCodeAsc(UUID roleId);

    /**
     * Deletes all permissions assigned to a role.
     *
     * Example:
     * deleteByRoleId(adminRoleId)
     *
     * Equivalent SQL:
     * DELETE FROM role_permissions
     * WHERE role_id = ?;
     */
    void deleteByRoleId(UUID roleId);
}
