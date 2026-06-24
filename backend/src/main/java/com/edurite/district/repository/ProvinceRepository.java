package com.edurite.district.repository;

import com.edurite.district.entity.Province;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProvinceRepository extends JpaRepository<Province, UUID> {
    List<Province> findByActiveTrueOrderByNameAsc();
    Optional<Province> findByIdAndActiveTrue(UUID id);
}
