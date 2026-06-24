package com.edurite.curriculum.repository;

import com.edurite.curriculum.entity.CurriculumAsset;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurriculumAssetRepository extends JpaRepository<CurriculumAsset, UUID> {
    List<CurriculumAsset> findByDistrictIdAndArchivedFalseOrderByRepositoryTypeAscSubjectAscGradeAscUploadDateDesc(UUID districtId);
    List<CurriculumAsset> findByDistrictIdAndRepositoryTypeIgnoreCaseAndArchivedFalseOrderBySubjectAscGradeAscUploadDateDesc(UUID districtId, String repositoryType);
    List<CurriculumAsset> findBySchoolIdAndArchivedFalseOrderByRepositoryTypeAscSubjectAscGradeAscUploadDateDesc(UUID schoolId);
    List<CurriculumAsset> findBySchoolIdAndRepositoryTypeIgnoreCaseAndArchivedFalseOrderBySubjectAscGradeAscUploadDateDesc(UUID schoolId, String repositoryType);
    List<CurriculumAsset> findByRepositoryTypeIgnoreCaseAndArchivedFalseOrderBySubjectAscGradeAscUploadDateDesc(String repositoryType);
}
