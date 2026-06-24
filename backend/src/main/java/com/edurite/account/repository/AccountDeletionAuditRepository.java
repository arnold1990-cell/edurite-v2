package com.edurite.account.repository;

// Import the entity we want to work with (account deletion logs)
import com.edurite.account.entity.AccountDeletionAudit;

// UUID is the type of the primary key (id)
import java.util.UUID;

// Spring Data JPA repository
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * This repository is used to interact with the account_deletion_audit table.
 *
 * It allows us to:
 * - Save audit logs
 * - Retrieve audit records
 * - Delete audit records (if needed)
 *
 * Because it extends JpaRepository, Spring automatically provides
 * all basic database operations.
 */
public interface AccountDeletionAuditRepository extends JpaRepository<AccountDeletionAudit, UUID> {

    // No methods are defined here, but you still get:
    //
    // save(audit)
    // findById(id)
    // findAll()
    // delete(audit)
    // count()
}
