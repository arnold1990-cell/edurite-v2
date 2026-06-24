package com.edurite.district.repository;

import com.edurite.district.entity.SchoolVisitSchedule;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolVisitScheduleRepository extends JpaRepository<SchoolVisitSchedule, UUID> {
    List<SchoolVisitSchedule> findByCircuitManagerIdOrderByVisitDateDesc(UUID circuitManagerId);
}
