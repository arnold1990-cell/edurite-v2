package com.edurite.user.repository;

// Importing the Role entity (the table we want to interact with)
import com.edurite.user.entity.Role;

// Optional is used when a result may or may not exist (avoids null errors)
import java.util.Optional;

// UUID is the type of the primary key (id) in your Role entity
import java.util.UUID;

// Spring Data JPA interface that gives us built-in database operations
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * This interface is responsible for interacting with the Role table in the database.
 *
 * It acts like a bridge between your application and the database.
 */
public interface RoleRepository extends JpaRepository<Role, UUID> {
    // JpaRepository already provides:
    // - save()
    // - findById()
    // - findAll()
    // - delete()
    // and many more methods automatically

    /**
     * Custom query method to find a role by its name.
     *
     * Spring automatically generates the SQL for this method based on the name.
     *
     * Example:
     * findByName("ADMIN") → SELECT * FROM roles WHERE name = 'ADMIN'
     *
     * Returns:
     * Optional<Role> → because the role may or may not exist
     */
    Optional<Role> findByName(String name);
}
