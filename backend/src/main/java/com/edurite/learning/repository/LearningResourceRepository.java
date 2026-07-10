package com.edurite.learning.repository;

import com.edurite.learning.entity.LearningResource;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningResourceRepository extends JpaRepository<LearningResource, UUID> {

    List<LearningResource> findByActiveTrueAndIsFreeTrueOrderByLastFetchedAtDescCreatedAtDesc();

    List<LearningResource> findByIdInAndActiveTrue(Collection<UUID> ids);

    Optional<LearningResource> findFirstByTitleIgnoreCaseAndProviderIgnoreCaseAndCourseUrlIgnoreCase(
            String title,
            String provider,
            String courseUrl
    );
}

