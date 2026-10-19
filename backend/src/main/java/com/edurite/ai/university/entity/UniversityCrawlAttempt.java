package com.edurite.ai.university.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="university_crawl_attempts") @Getter @Setter
public class UniversityCrawlAttempt extends BaseEntity {
    @Column(nullable=false,length=200) private String universityName;
    @Column(nullable=false,length=1200) private String sourceUrl;
    @Column(nullable=false,length=40) private String status;
    @Column(length=500) private String reason;
}
