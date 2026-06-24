package com.edurite.compliance.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "consent_records")
@Getter
@Setter
public class ConsentRecord extends BaseEntity {

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String consentType;

    @Column(nullable = false)
    private String consentVersion;

    @Column(nullable = false)
    private Boolean consentAccepted = Boolean.TRUE;

    @Column(nullable = false)
    private OffsetDateTime acceptedAt;

    private String ipAddress;
    private String userAgent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String metadata = "{}";
}

