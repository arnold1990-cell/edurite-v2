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

@Entity
@Table(name = "university_retrieval_logs", schema = "public")
@Getter
@Setter
public class UniversityRetrievalLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @Column(name = "source_url", length = 1200)
    private String sourceUrl;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "retrieval_type", length = 80)
    private String retrievalType;

    @Column(name = "retrieved_at", nullable = false)
    private OffsetDateTime retrievedAt;
}