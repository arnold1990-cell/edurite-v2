package com.edurite.user.entity;

import com.edurite.common.entity.BaseEntity;
import com.edurite.subscription.entity.PlanType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@SuppressWarnings("JpaDataSourceORMInspection")
@Entity
@Table(name = "users", schema = "public")
@Getter
@Setter
public class User extends BaseEntity {

    // Primary login identifier.
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    // Optional contact number. Enforced as unique when provided.
    @Column(name = "phone_number", unique = true, length = 30)
    private String phoneNumber;

    // Display username used in some modules.
    @Column(name = "username", unique = true)
    private String username;

    // Stores the encoded password (never the raw password).
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    // New accounts remain pending until activation or approval.
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.PENDING;

    // Updated after the user verifies their email address.
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    // Used for first login or password reset flows.
    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    // Soft delete marker. Null means the account is still active.
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    // Keeps a record of why the account was removed.
    @Column(name = "deletion_reason")
    private String deletionReason;

    // Updated after each successful login.
    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    // Defaults to the free plan unless upgraded.
    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false)
    private PlanType planType = PlanType.BASIC;

    // A user can belong to multiple roles (e.g. Teacher + Parent).
    // Roles are loaded immediately because they're required during authentication.
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            schema = "public",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id")
    )
    private Set<Role> roles = new HashSet<>();
}