package com.edurite.institution.universityinfo.entity;

import com.edurite.common.entity.BaseEntity;
import com.edurite.institution.entity.Institution;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

//noinspection JpaDataSourceORMInspection
@Entity
@Table(name = "university_retrieval_logs")
@Getter
@Setter
public class UniversityRetrievalLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @Column(length = 1200)
    private String sourceUrl;

    @Column(nullable = false, length = 40)
    private String status;

    @Column(length = 1000)
    private String message;

    @Column(length = 80)
    private String retrievalType;
    @Column(nullable = false)
    private OffsetDateTime retrievedAt;
}


