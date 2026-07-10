package com.edurite.learning.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "learning_resources", schema = "public")
@Getter
@Setter
public class LearningResource extends BaseEntity {

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "resource_type", nullable = false)
    private String resourceType = "LINK";

    @Column(name = "difficulty")
    private String difficulty;

    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;

    @Column(name = "tags")
    private String tags;

    @Column(name = "provider")
    private String provider;

    @Column(name = "category")
    private String category;

    @Column(name = "subject")
    private String subject;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "course_url", columnDefinition = "TEXT")
    private String courseUrl;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(name = "level")
    private String level;

    @Column(name = "language")
    private String language;

    @Column(name = "is_free", nullable = false)
    private boolean isFree = true;

    @Column(name = "source_type")
    private String sourceType;

    @Column(name = "last_fetched_at")
    private OffsetDateTime lastFetchedAt;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}