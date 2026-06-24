package com.edurite.institution.repository;

import com.edurite.institution.entity.Institution;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstitutionRepository extends JpaRepository<Institution, UUID> {

    Optional<Institution> findByNameIgnoreCase(String name);

    List<Institution> findByActiveTrueOrderByFeaturedDescNameAsc();
}

