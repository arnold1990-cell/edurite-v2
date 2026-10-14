package com.edurite.roadmap.repository;

import com.edurite.roadmap.entity.SavedCareerRoadmap;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedCareerRoadmapRepository extends JpaRepository<SavedCareerRoadmap, UUID> {

    List<SavedCareerRoadmap> findByLearnerIdOrderByUpdatedAtDesc(UUID learnerId);
    java.util.Optional<SavedCareerRoadmap> findByLearnerIdAndRequestKey(UUID learnerId, UUID requestKey);
    java.util.Optional<SavedCareerRoadmap> findByIdAndLearnerId(UUID id, UUID learnerId);
    List<SavedCareerRoadmap> findByLearnerIdAndStudyPlanTrueOrderByCreatedAtDesc(UUID learnerId);
}
