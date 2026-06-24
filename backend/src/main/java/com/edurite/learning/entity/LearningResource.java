package com.edurite.learning.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

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

    @Column(nullable = false)
    private boolean active = true;
}

