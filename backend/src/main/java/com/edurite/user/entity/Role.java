package com.edurite.user.entity;

// Importing a base class that likely contains common fields like id, createdAt, updatedAt
import com.edurite.common.entity.BaseEntity;

// JPA annotations used to map this class to a database table
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

// Lombok annotations to automatically generate getters and setters (no need to write them manually)
import lombok.Getter;
import lombok.Setter;

/**
 * This class represents a Role in the system.
 * Example roles: ADMIN, STUDENT, TEACHER
 *
 * It is an "Entity", meaning it will be stored in the database as a table.
 */
@Entity // Marks this class as a JPA entity (linked to a database table)
@Table(name = "roles", schema = "public")
// Maps this entity to the "roles" table in the "public" schema of the database

@Getter // Lombok: automatically creates getter methods (e.g., getName())
@Setter // Lombok: automatically creates setter methods (e.g., setName())
public class Role extends BaseEntity {
    // This class extends BaseEntity, so it likely already has:
    // - id (primary key)
    // - createdAt (timestamp)
    // - updatedAt (timestamp)

    /**
     * This field stores the name of the role.
     * Example values: "ADMIN", "USER", "TEACHER"
     */
    @Column(
            name = "name",        // Column name in the database
            nullable = false,     // This field cannot be null (must have a value)
            unique = true         // No two roles can have the same name
    )
    private String name;

    /*
     * Because of Lombok (@Getter and @Setter),
     * the following methods are automatically created for you:
     *
     * public String getName() {
     *     return name;
     * }
     *
     * public void setName(String name) {
     *     this.name = name;
     * }
     */
}
