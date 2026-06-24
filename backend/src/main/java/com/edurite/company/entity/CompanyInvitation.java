package com.edurite.company.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "company_invitations")
@Getter
@Setter
public class CompanyInvitation extends BaseEntity {
    @Column(nullable = false)
    private UUID companyId;
    @Column(nullable = false)
    private UUID studentId;
    @Column(nullable = false)
    private String invitationType;
    private UUID targetBursaryId;
    @Column(columnDefinition = "TEXT")
    private String message;
    @Column(nullable = false)
    private String invitationToken;
    private OffsetDateTime expiresAt;
}

