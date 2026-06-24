package com.edurite.school.portal.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "school_registration_requests")
@Getter
@Setter
public class SchoolRegistrationRequest extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "district_id", nullable = false)
    private UUID districtId;

    @Column(name = "province_id")
    private UUID provinceId;

    @Column(name = "circuit_id")
    private UUID circuitId;

    @Column(name = "school_id")
    private UUID schoolId;

    @Column(name = "school_name", nullable = false)
    private String schoolName;

    @Column(name = "emis_number", nullable = false, unique = true)
    private String emisNumber;

    @Column(name = "province", nullable = false)
    private String province;

    @Column(name = "district_name", nullable = false)
    private String districtName;

    @Column(name = "circuit")
    private String circuit;

    @Column(name = "school_type")
    private String schoolType;

    @Column(name = "principal_name", nullable = false)
    private String principalName;

    @Column(name = "principal_email", nullable = false)
    private String principalEmail;

    @Column(name = "school_email", nullable = false)
    private String schoolEmail;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "physical_address", nullable = false, length = 2000)
    private String physicalAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SchoolStatus status = SchoolStatus.PENDING_DISTRICT_APPROVAL;

    @Column(name = "rejection_reason", length = 2000)
    private String rejectionReason;

    @Column(name = "submitted_at", nullable = false)
    private OffsetDateTime submittedAt;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "rejected_at")
    private OffsetDateTime rejectedAt;
}
