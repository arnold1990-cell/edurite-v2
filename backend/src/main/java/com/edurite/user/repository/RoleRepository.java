package com.edurite.user.repository;

import com.edurite.user.entity.Role;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for Role entities.
 */
public interface RoleRepository extends JpaRepository<Role, UUID> {

    // Looks up a role using its unique name (e.g. ADMIN or TEACHER).
    Optional<Role> findByName(String name);
}