package com.edurite.admin.repository;

// Import the entity (platform settings table)
import com.edurite.admin.entity.PlatformSetting;

// Optional helps avoid null errors
import java.util.Optional;

// UUID = primary key type
import java.util.UUID;

// Spring Data JPA
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * This repository is used to interact with the platform_settings table.
 *
 * It allows us to:
 * - Save settings
 * - Retrieve settings
 */
public interface PlatformSettingRepository extends JpaRepository<PlatformSetting, UUID> {

    /**
     * Retrieves the FIRST (oldest) platform setting record.
     *
     * Why?
     * Because this system is designed to have ONLY ONE row
     * representing global platform configuration.
     *
     * Equivalent SQL:
     * SELECT * FROM platform_settings
     * ORDER BY created_at ASC
     * LIMIT 1;
     */
    Optional<PlatformSetting> findTopByOrderByCreatedAtAsc();
}
