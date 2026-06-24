package com.edurite.curriculum.service;

import com.edurite.ai.service.AiProviderOrchestratorService;
import com.edurite.curriculum.dto.CurriculumDtos;
import com.edurite.curriculum.entity.CurriculumAsset;
import com.edurite.curriculum.repository.AtpCalendarItemRepository;
import com.edurite.curriculum.repository.AtpTeacherReminderRepository;
import com.edurite.curriculum.repository.CurriculumAssetRepository;
import com.edurite.curriculum.repository.CurriculumReminderDispatchRepository;
import com.edurite.curriculum.repository.CurriculumRiskAlertRepository;
import com.edurite.curriculum.repository.CurriculumWeekPlanRepository;
import com.edurite.curriculum.repository.TeacherAtpProgressRepository;
import com.edurite.curriculum.repository.TeacherCurriculumProgressRepository;
import com.edurite.district.entity.District;
import com.edurite.district.repository.DistrictAdminProfileRepository;
import com.edurite.district.repository.DistrictRepository;
import com.edurite.notification.service.NotificationService;
import com.edurite.school.portal.repository.SchoolClassRepository;
import com.edurite.school.portal.repository.SchoolRepository;
import com.edurite.school.portal.repository.SchoolSubjectRepository;
import com.edurite.school.portal.repository.SchoolUserProfileRepository;
import com.edurite.school.portal.repository.TeacherAssignmentRepository;
import com.edurite.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurriculumServiceFileFlowTest {

    @Mock private CurriculumAssetRepository curriculumAssetRepository;
    @Mock private AtpCalendarItemRepository atpCalendarItemRepository;
    @Mock private AtpTeacherReminderRepository atpTeacherReminderRepository;
    @Mock private TeacherAtpProgressRepository teacherAtpProgressRepository;
    @Mock private CurriculumWeekPlanRepository curriculumWeekPlanRepository;
    @Mock private TeacherCurriculumProgressRepository teacherCurriculumProgressRepository;
    @Mock private CurriculumReminderDispatchRepository curriculumReminderDispatchRepository;
    @Mock private CurriculumRiskAlertRepository curriculumRiskAlertRepository;
    @Mock private DistrictRepository districtRepository;
    @Mock private SchoolRepository schoolRepository;
    @Mock private SchoolClassRepository schoolClassRepository;
    @Mock private TeacherAssignmentRepository teacherAssignmentRepository;
    @Mock private SchoolSubjectRepository schoolSubjectRepository;
    @Mock private SchoolUserProfileRepository schoolUserProfileRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private DistrictAdminProfileRepository districtAdminProfileRepository;
    @Mock private CurriculumResourceService curriculumResourceService;
    @Mock private AiAtpExtractionService aiAtpExtractionService;
    @Mock private AiProviderOrchestratorService aiProviderOrchestratorService;

    private CurriculumService curriculumService;

    @BeforeEach
    void setUp() {
        curriculumService = new CurriculumService(
                curriculumAssetRepository,
                atpCalendarItemRepository,
                atpTeacherReminderRepository,
                teacherAtpProgressRepository,
                curriculumWeekPlanRepository,
                teacherCurriculumProgressRepository,
                curriculumReminderDispatchRepository,
                curriculumRiskAlertRepository,
                districtRepository,
                schoolRepository,
                schoolClassRepository,
                teacherAssignmentRepository,
                schoolSubjectRepository,
                schoolUserProfileRepository,
                userRepository,
                notificationService,
                districtAdminProfileRepository,
                curriculumResourceService,
                aiAtpExtractionService,
                aiProviderOrchestratorService,
                new ObjectMapper().findAndRegisterModules()
        );
    }

    @Test
    void uploadThenDownloadPdfPreservesOriginalPdfBytes() {
        UUID districtId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        byte[] originalPdf = """
                %PDF-1.4
                1 0 obj
                << /Type /Catalog >>
                endobj
                trailer
                <<>>
                %%EOF
                """.replace("\r", "").getBytes(StandardCharsets.US_ASCII);

        District district = new District();
        district.setId(districtId);
        district.setProvince("Gauteng");

        when(districtRepository.findById(districtId)).thenReturn(Optional.of(district));
        when(curriculumAssetRepository.save(any(CurriculumAsset.class))).thenAnswer(invocation -> {
            CurriculumAsset asset = invocation.getArgument(0);
            if (asset.getId() == null) {
                asset.setId(UUID.randomUUID());
            }
            return asset;
        });
        when(curriculumResourceService.toAssetDto(any(CurriculumAsset.class))).thenAnswer(invocation -> {
            CurriculumAsset asset = invocation.getArgument(0);
            return new CurriculumDtos.CurriculumAssetDto(
                    asset.getId(),
                    asset.getRepositoryType(),
                    asset.getContentSource(),
                    asset.getSource(),
                    asset.getVisibility(),
                    asset.getStatus(),
                    asset.getExtractionStatus(),
                    asset.getExtractionError(),
                    "District Approved",
                    asset.getTitle(),
                    asset.getSubject(),
                    asset.getGrade(),
                    asset.getCurriculumPhase(),
                    asset.getAcademicYear(),
                    asset.getProvince(),
                    asset.getVersionNumber(),
                    asset.getDescription(),
                    asset.getTerm(),
                    asset.getWeekNumber(),
                    "Uploader",
                    asset.getUploadDate(),
                    asset.getExtractedAt(),
                    asset.isArchived(),
                    asset.isActive(),
                    asset.isDeleted(),
                    asset.getPdfBytes() != null && asset.getPdfBytes().length > 0,
                    asset.getDocxBytes() != null && asset.getDocxBytes().length > 0,
                    asset.getExcelBytes() != null && asset.getExcelBytes().length > 0
            );
        });

        CurriculumDtos.CurriculumAssetUpsertRequest request = new CurriculumDtos.CurriculumAssetUpsertRequest(
                "SYLLABUS",
                "Physical Sciences Syllabus",
                "Physical Sciences",
                "Grade 12",
                "FET",
                2026,
                "Gauteng",
                "v1.0",
                "Official syllabus",
                "Term 1",
                null,
                new CurriculumDtos.FilePayload("physical-sciences-grade-12.pdf", "application/pdf", Base64.getEncoder().encodeToString(originalPdf)),
                null,
                null
        );

        curriculumService.saveDistrictAsset(districtId, userId, request);

        ArgumentCaptor<CurriculumAsset> assetCaptor = ArgumentCaptor.forClass(CurriculumAsset.class);
        verify(curriculumAssetRepository).save(assetCaptor.capture());
        CurriculumAsset storedAsset = assetCaptor.getValue();
        when(curriculumAssetRepository.findById(storedAsset.getId())).thenReturn(Optional.of(storedAsset));

        assertThat(storedAsset.getPdfBytes()).isEqualTo(originalPdf);
        assertThat(storedAsset.getPdfBase64()).isNull();
        assertThat(storedAsset.getPdfContentType()).isEqualTo("application/pdf");
        assertThat(storedAsset.getPdfFileName()).isEqualTo("physical-sciences-grade-12.pdf");

        CurriculumService.CurriculumFileResponse downloaded = curriculumService.downloadDistrictAsset(districtId, storedAsset.getId(), "PDF");

        assertThat(downloaded.contentType()).isEqualTo("application/pdf");
        assertThat(downloaded.fileName()).isEqualTo("physical-sciences-grade-12.pdf");
        assertThat(downloaded.content()).isEqualTo(originalPdf);
        assertThat(new String(downloaded.content(), 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }
}
