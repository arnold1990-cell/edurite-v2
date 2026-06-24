package com.edurite.course.repository;

import com.edurite.course.entity.Course;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    @Query("""
            SELECT c FROM Course c
            LEFT JOIN Institution i ON i.id = c.institutionId
            WHERE (
                :query = ''
                OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(COALESCE(c.level, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(COALESCE(i.name, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(COALESCE(i.location, '')) LIKE LOWER(CONCAT('%', :query, '%'))
              )
              AND (
                :level = ''
                OR LOWER(COALESCE(c.level, '')) LIKE LOWER(CONCAT('%', :level, '%'))
              )
              AND (
                :location = ''
                OR LOWER(COALESCE(i.location, '')) LIKE LOWER(CONCAT('%', :location, '%'))
                OR LOWER(COALESCE(i.name, '')) LIKE LOWER(CONCAT('%', :location, '%'))
                OR REPLACE(LOWER(COALESCE(i.location, '')), ' ', '') LIKE CONCAT('%', REPLACE(LOWER(:location), ' ', ''), '%')
              )
            """)
    Page<Course> search(
            @Param("query") String query,
            @Param("level") String level,
            @Param("location") String location,
            Pageable pageable
    );
}

