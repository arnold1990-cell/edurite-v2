package com.edurite.user.repository;

import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * UserRepository is responsible for database operations related to users.
 * It connects the User entity to the database.
 * Because it extends JpaRepository, Spring automatically gives us methods like:
 * - save()
 * - findById()
 * - findAll()
 * - delete()
 * - count()
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds one user using their email.
     * Example: findByEmail("test@gmail.com")
     * This search is case-sensitive depending on the database.
     */
    Optional<User> findByEmail(String email);

    /**
     * Finds one user using their email, ignoring capital letters.
     * Example: "Test@gmail.com" and "test@gmail.com" are treated the same.
     */
    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByUsernameIgnoreCase(String username);

    /**
     * Finds one user using their phone number.
     * Returns Optional because the phone number may not exist.
     */
    Optional<User> findByPhoneNumber(String phoneNumber);

    /**
     * Finds all users whose email has the same part before the @ symbol.
     * Example: if email is "arnold@gmail.com", the local part is "arnold".
     * This query checks:
     * - The email contains @
     * - The part before @ matches the given localPart
     */
    @Query("""
            SELECT u
            FROM User u
            WHERE LOCATE('@', u.email) > 1
              AND LOWER(SUBSTRING(u.email, 1, LOCATE('@', u.email) - 1)) = LOWER(:localPart)
            """)
    List<User> findAllByEmailLocalPart(@Param("localPart") String localPart);

    /**
     * Finds the latest user where BOTH email and mobile number match.
     * This is a native SQL query using the users, students, and companies tables.
     * ORDER BY created_at DESC means newest user comes first.
     * LIMIT 1 means return only one result.
     */
    @Query(value = """
            SELECT u.*
            FROM public.users u
            LEFT JOIN public.students s ON s.user_id = u.id
            LEFT JOIN public.companies c ON c.user_id = u.id
            WHERE :email IS NOT NULL
              AND :mobileNumber IS NOT NULL
              AND LOWER(u.email) = LOWER(:email)
              AND (
                    (s.phone IS NOT NULL AND LOWER(s.phone) = LOWER(:mobileNumber))
                    OR (c.mobile_number IS NOT NULL AND LOWER(c.mobile_number) = LOWER(:mobileNumber))
              )
            ORDER BY u.created_at DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<User> findFirstByEmailAndMobileNumber(
            @Param("email") String email,
            @Param("mobileNumber") String mobileNumber
    );

    /**
     * Finds the latest user where EITHER email OR mobile number matches.
     * It searches phone numbers in students.phone and companies.mobile_number.
     */
    @Query(value = """
            SELECT u.*
            FROM public.users u
            LEFT JOIN public.students s ON s.user_id = u.id
            LEFT JOIN public.companies c ON c.user_id = u.id
            WHERE (:email IS NOT NULL AND LOWER(u.email) = LOWER(:email))
               OR (
                    :mobileNumber IS NOT NULL
                    AND (
                        (s.phone IS NOT NULL AND LOWER(s.phone) = LOWER(:mobileNumber))
                        OR (c.mobile_number IS NOT NULL AND LOWER(c.mobile_number) = LOWER(:mobileNumber))
                    )
               )
            ORDER BY u.created_at DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<User> findFirstByEmailOrMobileNumber(
            @Param("email") String email,
            @Param("mobileNumber") String mobileNumber
    );

    /**
     * Checks if a user exists with this exact email.
     * Returns true or false.
     */
    boolean existsByEmail(String email);

    /**
     * Checks if a user exists with this email, ignoring capital letters.
     * Example: "Admin@gmail.com" and "admin@gmail.com" are treated the same.
     */
    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    /**
     * Checks if a user exists with this phone number.
     */
    boolean existsByPhoneNumber(String phoneNumber);

    /**
     * Gets all users ordered from newest to oldest.
     */
    List<User> findAllByOrderByCreatedAtDesc();

    /**
     * Gets only the latest 10 users.
     * Useful for dashboards, admin panels, or recent registrations.
     */
    List<User> findTop10ByOrderByCreatedAtDesc();

    /**
     * Counts users by their account status.
     * Example: countByStatus(UserStatus.ACTIVE)
     */
    long countByStatus(UserStatus status);

    List<User> findByStatusAndDeletedAtIsNull(UserStatus status);

    /**
     * Counts how many users have a certain role.
     * Example: countDistinctByRoleName("ADMIN")
     * DISTINCT prevents counting the same user more than once.
     */
    @Query("""
            SELECT COUNT(DISTINCT u)
            FROM User u
            JOIN u.roles r
            WHERE UPPER(r.name) = UPPER(:roleName)
            """)
    long countDistinctByRoleName(@Param("roleName") String roleName);

    @Query(value = """
            SELECT DISTINCT u.id
            FROM public.users u
            LEFT JOIN public.user_roles ur ON ur.user_id = u.id
            LEFT JOIN public.roles r ON r.id = ur.role_id
            LEFT JOIN public.students s ON s.user_id = u.id
            LEFT JOIN public.school_students ss ON ss.student_id = s.id
            WHERE u.deleted_at IS NULL
              AND (:activeOnly = FALSE OR u.status = 'ACTIVE')
              AND (:roleName IS NULL OR :roleName = '' OR UPPER(r.name) = UPPER(:roleName) OR UPPER(r.name) = CONCAT('ROLE_', UPPER(:roleName)))
              AND (:status IS NULL OR :status = '' OR UPPER(u.status) = UPPER(:status))
              AND (:planType IS NULL OR :planType = '' OR UPPER(u.plan_type) = UPPER(:planType))
              AND (:grade IS NULL OR :grade = '' OR UPPER(s.selected_grade) = UPPER(:grade))
              AND (:schoolId IS NULL OR ss.school_id = :schoolId)
              AND (
                    :search IS NULL OR :search = '' OR
                    LOWER(u.email) LIKE CONCAT('%', LOWER(:search), '%') OR
                    LOWER(u.first_name) LIKE CONCAT('%', LOWER(:search), '%') OR
                    LOWER(u.last_name) LIKE CONCAT('%', LOWER(:search), '%')
              )
            ORDER BY u.created_at DESC
            """, nativeQuery = true)
    List<UUID> findUserIdsByNotificationFilter(
            @Param("roleName") String roleName,
            @Param("status") String status,
            @Param("planType") String planType,
            @Param("grade") String grade,
            @Param("schoolId") UUID schoolId,
            @Param("search") String search,
            @Param("activeOnly") boolean activeOnly,
            Pageable pageable
    );

    @Query(value = """
            SELECT COUNT(DISTINCT u.id)
            FROM public.users u
            LEFT JOIN public.user_roles ur ON ur.user_id = u.id
            LEFT JOIN public.roles r ON r.id = ur.role_id
            LEFT JOIN public.students s ON s.user_id = u.id
            LEFT JOIN public.school_students ss ON ss.student_id = s.id
            WHERE u.deleted_at IS NULL
              AND (:activeOnly = FALSE OR u.status = 'ACTIVE')
              AND (:roleName IS NULL OR :roleName = '' OR UPPER(r.name) = UPPER(:roleName) OR UPPER(r.name) = CONCAT('ROLE_', UPPER(:roleName)))
              AND (:status IS NULL OR :status = '' OR UPPER(u.status) = UPPER(:status))
              AND (:planType IS NULL OR :planType = '' OR UPPER(u.plan_type) = UPPER(:planType))
              AND (:grade IS NULL OR :grade = '' OR UPPER(s.selected_grade) = UPPER(:grade))
              AND (:schoolId IS NULL OR ss.school_id = :schoolId)
              AND (
                    :search IS NULL OR :search = '' OR
                    LOWER(u.email) LIKE CONCAT('%', LOWER(:search), '%') OR
                    LOWER(u.first_name) LIKE CONCAT('%', LOWER(:search), '%') OR
                    LOWER(u.last_name) LIKE CONCAT('%', LOWER(:search), '%')
              )
            """, nativeQuery = true)
    long countByNotificationFilter(
            @Param("roleName") String roleName,
            @Param("status") String status,
            @Param("planType") String planType,
            @Param("grade") String grade,
            @Param("schoolId") UUID schoolId,
            @Param("search") String search,
            @Param("activeOnly") boolean activeOnly
    );
}