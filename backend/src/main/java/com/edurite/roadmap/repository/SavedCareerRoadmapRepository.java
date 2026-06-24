package com.edurite.roadmap.repository;

import com.edurite.roadmap.entity.SavedCareerRoadmap;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedCareerRoadmapRepository extends JpaRepository<SavedCareerRoadmap, UUID> {

    List<SavedCareerRoadmap> findByLearnerIdOrderByUpdatedAtDesc(UUID learnerId);
}
