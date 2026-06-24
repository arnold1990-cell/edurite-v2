package com.edurite.learning.repository;

import com.edurite.learning.entity.LearningCategory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningCategoryRepository extends JpaRepository<LearningCategory, UUID> {
    List<LearningCategory> findByActiveTrueOrderByNameAsc();
}

