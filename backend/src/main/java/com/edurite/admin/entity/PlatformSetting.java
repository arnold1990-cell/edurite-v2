package com.edurite.admin.entity;

// Base class (likely contains id, createdAt, updatedAt)
import com.edurite.common.entity.BaseEntity;

// JPA annotations
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

// Lombok (auto getters & setters)
import lombok.Getter;
import lombok.Setter;

/**
 * This entity stores platform-wide settings.
 *
 * Instead of hardcoding features in code,
 * we store them in the database so admins can turn features ON/OFF.
 */
@Entity
@Table(name = "platform_settings") // Table name in database
@Getter
@Setter
public class PlatformSetting extends BaseEntity {

    /**
     * Allows companies to register themselves without admin intervention.
     * Default = true
     */
    @Column(nullable = false)
    private boolean companySelfRegistrationEnabled = true;

    /**
     * If true, admin must approve companies before they become active.
     */
    @Column(nullable = false)
    private boolean manualCompanyApprovalRequired = true;

    /**
     * Controls whether bursary posting feature is enabled.
     */
    @Column(nullable = false)
    private boolean bursaryPostingEnabled = true;

    /**
     * Allows students to register accounts.
     */
    @Column(nullable = false)
    private boolean studentRegistrationEnabled = true;

    /**
     * If true, bursaries must be reviewed before being published.
     */
    @Column(nullable = false)
    private boolean bursaryModerationRequired = false;

    /**
     * Enables or disables AI guidance feature.
     */
    @Column(nullable = false)
    private boolean aiGuidanceEnabled = true;

    /**
     * If true, the whole platform is in maintenance mode.
     * Users may be blocked or shown a maintenance message.
     */
    @Column(nullable = false)
    private boolean maintenanceModeEnabled = false;

    /**
     * Support email shown to users.
     */
    @Column(length = 255)
    private String supportEmail;

    /**
     * Contact info (can be long text).
     * Example:
     * Address, phone numbers, social links
     */
    @Column(columnDefinition = "TEXT")
    private String platformContactInfo;

    /**
     * Maximum number of rows allowed in CSV uploads.
     * Helps prevent system overload.
     */
    @Column(nullable = false)
    private int maxCsvBulkUploadRows = 500;
}
