package com.edurite.school.portal.repository;

import com.edurite.school.portal.entity.AtpTopic;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AtpTopicRepository extends JpaRepository<AtpTopic, UUID> {
    List<AtpTopic> findByActiveTrueOrderByPhaseAscGradeAscAcademicYearAscTermAscWeekNumberAscTopicAsc();
}
