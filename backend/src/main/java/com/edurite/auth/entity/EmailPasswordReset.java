package com.edurite.auth.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "email_password_resets")
@Getter
@Setter
public class EmailPasswordReset extends BaseEntity {
    @Column(nullable = false, unique = true)
    private UUID userId;
    private String otpHash;
    private OffsetDateTime expiresAt;
    private String resetTokenHash;
    private OffsetDateTime resetTokenExpiresAt;
    private int attempts;
    private OffsetDateTime requestedAt;
    private OffsetDateTime windowStartedAt;
    private int requestCount;
}
