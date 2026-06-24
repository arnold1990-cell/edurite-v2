package com.edurite.curriculum.service;

import com.edurite.curriculum.dto.CurriculumDtos;
import com.edurite.curriculum.entity.CurriculumAsset;
import com.edurite.curriculum.repository.CurriculumAssetRepository;
import com.edurite.school.portal.entity.School;
import com.edurite.school.portal.entity.SchoolSubject;
import com.edurite.school.portal.entity.TeacherAssignment;
import com.edurite.school.portal.repository.SchoolRepository;
import com.edurite.school.portal.repository.SchoolSubjectRepository;
import com.edurite.school.portal.repository.TeacherAssignmentRepository;
import com.edurite.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurriculumResourceServiceTest {

    @Mock private CurriculumAssetRepository curriculumAssetRepository;
    @Mock private SchoolRepository schoolRepository;
    @Mock private TeacherAssignmentRepository teacherAssignmentRepository;
    @Mock private SchoolSubjectRepository schoolSubjectRepository;
    @Mock private UserRepository userRepository;

    private CurriculumResourceService curriculumResourceService;

    @BeforeEach
    void setUp() {
        curriculumResourceService = new CurriculumResourceService(
                curriculumAssetRepository,
                schoolRepository,
                teacherAssignmentRepository,
                schoolSubjectRepository,
                userRepository
        );
    }

    @Test
    void teacherResourcesIncludeDistrictAssetWhenTeacherGradeFallsInsideRange() {
        UUID schoolId = UUID.randomUUID();
        UUID districtId = UUID.randomUUID();
        UUID teacherUserId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();

        School school = new School();
        school.setId(schoolId);
        school.setDistrictId(districtId);
        school.setProvince("Gauteng");

        TeacherAssignment assignment = new TeacherAssignment();
        assignment.setSchoolId(schoolId);
        assignment.setTeacherUserId(teacherUserId);
        assignment.setSubjectId(subjectId);
        assignment.setGrade("Grade 12");
        assignment.setActive(true);

        SchoolSubject subject = new SchoolSubject();
        subject.setId(subjectId);
        subject.setSchoolId(schoolId);
        subject.setSubjectName("Physical Sciences");
        subject.setGrade("Grade 12");
        subject.setGradeRange("Grade 10-12");
        subject.setActive(true);

        CurriculumAsset districtAsset = new CurriculumAsset();
        districtAsset.setId(UUID.randomUUID());
        districtAsset.setDistrictId(districtId);
        districtAsset.setOwnerScope(CurriculumResourceService.OWNER_DISTRICT);
        districtAsset.setRepositoryType("SYLLABUS");
        districtAsset.setContentSource("OFFICIAL");
        districtAsset.setSource(CurriculumResourceService.SOURCE_DISTRICT);
        districtAsset.setVisibility(CurriculumResourceService.VISIBILITY_DISTRICT_WIDE);
        districtAsset.setStatus(CurriculumResourceService.STATUS_ACTIVE);
        districtAsset.setTitle("Grade 12 Physical Sciences Syllabus");
        districtAsset.setSubject("Physical Sciences");
        districtAsset.setGrade("Grade 10-12");
        districtAsset.setProvince("Gauteng");
        districtAsset.setUploadDate(OffsetDateTime.now());
        districtAsset.setActive(true);
        districtAsset.setArchived(false);
        districtAsset.setDeleted(false);
        districtAsset.setPdfBase64("cGRm");

        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));
        when(teacherAssignmentRepository.findBySchoolIdAndTeacherUserIdAndActiveTrue(schoolId, teacherUserId)).thenReturn(List.of(assignment));
        when(schoolSubjectRepository.findBySchoolIdAndActiveTrue(schoolId)).thenReturn(List.of(subject));
        when(curriculumAssetRepository.findAll()).thenReturn(List.of(districtAsset));
        when(curriculumAssetRepository.findBySchoolIdAndArchivedFalseOrderByRepositoryTypeAscSubjectAscGradeAscUploadDateDesc(schoolId)).thenReturn(List.of());

        List<CurriculumDtos.CurriculumAssetDto> results = curriculumResourceService.getDistrictResourcesForTeacher(
                schoolId,
                teacherUserId,
                new CurriculumDtos.CurriculumResourceQuery(null, null, null, null, null, null, null)
        );

        assertThat(results).extracting(CurriculumDtos.CurriculumAssetDto::id).contains(districtAsset.getId());
    }

    @Test
    void schoolResourceFilterMatchesSingleGradeAgainstDistrictGradeRange() {
        UUID schoolId = UUID.randomUUID();
        UUID districtId = UUID.randomUUID();

        School school = new School();
        school.setId(schoolId);
        school.setDistrictId(districtId);
        school.setProvince("Gauteng");

        CurriculumAsset districtAsset = new CurriculumAsset();
        districtAsset.setId(UUID.randomUUID());
        districtAsset.setDistrictId(districtId);
        districtAsset.setOwnerScope(CurriculumResourceService.OWNER_DISTRICT);
        districtAsset.setRepositoryType("ATP");
        districtAsset.setContentSource("OFFICIAL");
        districtAsset.setSource(CurriculumResourceService.SOURCE_DISTRICT);
        districtAsset.setVisibility(CurriculumResourceService.VISIBILITY_DISTRICT_WIDE);
        districtAsset.setStatus(CurriculumResourceService.STATUS_ACTIVE);
        districtAsset.setTitle("Physical Sciences ATP");
        districtAsset.setSubject("Physical Sciences");
        districtAsset.setGrade("Grade 10-12");
        districtAsset.setProvince("Gauteng");
        districtAsset.setUploadDate(OffsetDateTime.now());
        districtAsset.setActive(true);
        districtAsset.setArchived(false);
        districtAsset.setDeleted(false);
        districtAsset.setPdfBase64("cGRm");

        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));
        when(curriculumAssetRepository.findAll()).thenReturn(List.of(districtAsset));
        when(curriculumAssetRepository.findBySchoolIdAndArchivedFalseOrderByRepositoryTypeAscSubjectAscGradeAscUploadDateDesc(schoolId)).thenReturn(List.of());

        List<CurriculumDtos.CurriculumAssetDto> results = curriculumResourceService.getDistrictResourcesForSchool(
                schoolId,
                new CurriculumDtos.CurriculumResourceQuery("ATP", "Physical Sciences", "Grade 12", null, null, null, null)
        );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).id()).isEqualTo(districtAsset.getId());
    }
}
