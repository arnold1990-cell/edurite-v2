package com.edurite.admin.repository;

// Import the AuditLog entity (the table we are working with)
import com.edurite.admin.entity.AuditLog;

// Java collection
import java.util.Collection;
import java.util.List;

// Spring Data JPA repository
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * This repository is used to interact with the audit_logs table.
 *
 * It allows you to:
 * - Save audit logs
 * - Retrieve logs
 * - Delete logs (if needed)
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, java.util.UUID> {

    /**
     * Retrieves the latest 100 audit logs ordered by creation time (newest first).
     *
     * Spring automatically generates the query from the method name.
     *
     * Equivalent SQL:
     * SELECT * FROM audit_logs
     * ORDER BY created_at DESC
     * LIMIT 100;
     */
    List<AuditLog> findTop100ByOrderByCreatedAtDesc();

    List<AuditLog> findTop100ByActorIdInOrderByCreatedAtDesc(Collection<java.util.UUID> actorIds);
}
