package com.edurite.learning.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

//noinspection JpaDataSourceORMInspection
@Entity
@Table(name = "learning_resources")
@Getter
@Setter
public class LearningResource extends BaseEntity {

    private UUID categoryId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String resourceType = "LINK";

    private String difficulty;
    private Integer estimatedMinutes;
    private String tags;

    private String provider;
    private String category;
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "course_url", columnDefinition = "TEXT")
    private String courseUrl;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    private String level;
    private String language;

    @Column(nullable = false)
    private boolean isFree = true;

    private String sourceType;
    private OffsetDateTime lastFetchedAt;

    @Column(nullable = false)
    private boolean active = true;
}


