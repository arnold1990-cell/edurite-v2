package com.edurite.user.entity;

// BaseEntity likely contains common fields like id, createdAt, updatedAt
import com.edurite.common.entity.BaseEntity;

// Enum representing subscription plans (e.g., BASIC, PREMIUM)
import com.edurite.subscription.entity.PlanType;

// JPA annotations for database mapping
import jakarta.persistence.*;

// Used for date & time with timezone support
import java.time.OffsetDateTime;

// Collections to store multiple roles
import java.util.HashSet;
import java.util.Set;

// Lombok to auto-generate getters and setters
import lombok.Getter;
import lombok.Setter;

/**
 * This class represents a USER in the system.
 *
 * It stores authentication details (email, password)
 * and profile information (name, phone, roles, etc).
 */
@SuppressWarnings("JpaDataSourceORMInspection")
@Entity // Marks this class as a database entity
@Table(name = "users", schema = "public") // Maps to "users" table
@Getter
@Setter
public class User extends BaseEntity {

    /**
     * User's email address (used for login).
     * Must be unique and cannot be null.
     */
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    /**
     * Optional phone number.
     * Must be unique if provided.
     */
    @Column(name = "phone_number", unique = true, length = 30)
    private String phoneNumber;

    @Column(name = "username", unique = true)
    private String username;

    /**
     * Stores the hashed password (NOT plain text).
     * Always required.
     */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /**
     * User's first name.
     */
    @Column(name = "first_name", nullable = false)
    private String firstName;

    /**
     * User's last name.
     */
    @Column(name = "last_name", nullable = false)
    private String lastName;

    /**
     * User account status.
     * Stored as STRING in DB (e.g., PENDING, ACTIVE, DISABLED)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.PENDING;
    // Default value: PENDING (when user is newly created)

    /**
     * Indicates if the user's email is verified.
     * Default = false
     */
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = false;

    /**
     * Soft delete field.
     * Instead of deleting user from DB, we mark when they were deleted.
     */
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    /**
     * Reason why the user was deleted.
     */
    @Column(name = "deletion_reason")
    private String deletionReason;

    /**
     * Stores the last login time of the user.
     */
    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    /**
     * Subscription plan of the user (BASIC, PREMIUM, etc).
     * Stored as STRING in DB.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false)
    private PlanType planType = PlanType.BASIC;
    // Default plan = BASIC

    /**
     * Relationship: A user can have multiple roles
     * (e.g., ADMIN + TEACHER)
     *
     * Many-to-Many relationship:
     * - One user → many roles
     * - One role → many users
     */
    @ManyToMany(fetch = FetchType.EAGER)
    // EAGER means roles are loaded immediately when user is fetched

    @JoinTable(
            name = "user_roles", // Join table name
            schema = "public",

            // Column linking to User table
            joinColumns = @JoinColumn(
                    name = "user_id",
                    referencedColumnName = "id"
            ),

            // Column linking to Role table
            inverseJoinColumns = @JoinColumn(
                    name = "role_id",
                    referencedColumnName = "id"
            )
    )
    private Set<Role> roles = new HashSet<>();
    // Using Set to avoid duplicate roles
}
