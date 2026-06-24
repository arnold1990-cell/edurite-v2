package com.edurite.learning.repository;

import com.edurite.learning.entity.LearningResource;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningResourceRepository extends JpaRepository<LearningResource, UUID> {
    List<LearningResource> findByActiveTrueOrderByCreatedAtDesc();

    List<LearningResource> findByIdInAndActiveTrue(Collection<UUID> ids);
}

