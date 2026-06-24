package com.edurite.company.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "company_bookmarks")
@Getter
@Setter
public class CompanyBookmark extends BaseEntity {
    @Column(nullable = false)
    private UUID companyId;
    @Column(nullable = false)
    private UUID studentId;
    @Column(columnDefinition = "TEXT")
    private String notes;
}

