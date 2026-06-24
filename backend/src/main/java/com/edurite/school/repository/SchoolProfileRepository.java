package com.edurite.school.repository;

import com.edurite.school.entity.SchoolProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface SchoolProfileRepository extends JpaRepository<SchoolProfile, UUID> {
}


