package com.edurite.school.portal.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "school_user_profiles")
@Getter
@Setter
public class SchoolUserProfile extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "role_name", nullable = false)
    private String roleName;

    @Column(name = "employee_or_student_no")
    private String employeeOrStudentNo;

    @Column(name = "portal_username")
    private String portalUsername;

    @Column(name = "initial_password")
    private String initialPassword;

    @Column(name = "guardian_name")
    private String guardianName;

    @Column(name = "guardian_phone")
    private String guardianPhone;

    @Column(name = "guardian_email")
    private String guardianEmail;

    @Column(name = "consent_status")
    private String consentStatus;

    @Column(name = "report_upload_status")
    private String reportUploadStatus;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;
}


