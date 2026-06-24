package com.edurite.company.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "company_messages")
@Getter
@Setter
public class CompanyMessage extends BaseEntity {
    @Column(nullable = false)
    private UUID companyId;
    @Column(nullable = false)
    private UUID studentId;
    @Column(nullable = false)
    private String subject;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    @Column(nullable = false)
    private UUID sentByUserId;
}

