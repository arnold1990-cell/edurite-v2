package com.edurite.district.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "subject_advisor_assignments")
@Getter
@Setter
public class SubjectAdvisorAssignment extends BaseEntity {

    @Column(name = "advisor_user_id", nullable = false)
    private UUID advisorUserId;

    @Column(nullable = false)
    private String subject;

    @Column
    private String grade;

    @Column
    private String phase;

    @Column(name = "district_id", nullable = false)
    private UUID districtId;

    @Column(nullable = false)
    private boolean active = true;
}
