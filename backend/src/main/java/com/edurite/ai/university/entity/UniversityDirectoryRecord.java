package com.edurite.ai.university.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "university_directory_records")
@Getter @Setter
public class UniversityDirectoryRecord extends BaseEntity {
    @Column(nullable = false, unique = true, length = 200)
    private String universityName;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String metadataJson;
}
