package com.edurite.roadmap.repository;

import com.edurite.roadmap.entity.CareerRoadmap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareerRoadmapRepository extends JpaRepository<CareerRoadmap, UUID> {
    List<CareerRoadmap> findByActiveTrueOrderByTitleAsc();
    Optional<CareerRoadmap> findBySlugAndActiveTrue(String slug);
    Optional<CareerRoadmap> findTopByTitleContainingIgnoreCaseAndActiveTrue(String title);
}

