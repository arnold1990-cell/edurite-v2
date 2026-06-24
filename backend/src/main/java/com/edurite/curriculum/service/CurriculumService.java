package com.edurite.curriculum.service;

import com.edurite.ai.service.AiProviderOrchestratorService;
import com.edurite.common.exception.ResourceConflictException;
import com.edurite.curriculum.dto.CurriculumDtos;
import com.edurite.curriculum.entity.AtpCalendarItem;
import com.edurite.curriculum.entity.AtpTeacherReminder;
import com.edurite.curriculum.entity.CurriculumAsset;
import com.edurite.curriculum.entity.CurriculumReminderDispatch;
import com.edurite.curriculum.entity.CurriculumRiskAlert;
import com.edurite.curriculum.entity.CurriculumWeekPlan;
import com.edurite.curriculum.entity.TeacherAtpProgress;
import com.edurite.curriculum.entity.TeacherCurriculumProgress;
import com.edurite.curriculum.repository.AtpCalendarItemRepository;
import com.edurite.curriculum.repository.AtpTeacherReminderRepository;
import com.edurite.curriculum.repository.CurriculumAssetRepository;
import com.edurite.curriculum.repository.CurriculumAssetSummaryView;
import com.edurite.curriculum.repository.CurriculumReminderDispatchRepository;
import com.edurite.curriculum.repository.CurriculumRiskAlertRepository;
import com.edurite.curriculum.repository.CurriculumWeekPlanRepository;
import com.edurite.curriculum.repository.TeacherAtpProgressRepository;
import com.edurite.curriculum.repository.TeacherCurriculumProgressRepository;
import com.edurite.district.dto.DistrictDtos;
import com.edurite.district.entity.District;
import com.edurite.district.repository.DistrictAdminProfileRepository;
import com.edurite.district.repository.DistrictRepository;
import com.edurite.notification.service.NotificationService;
import com.edurite.school.portal.entity.School;
import com.edurite.school.portal.entity.SchoolClass;
import com.edurite.school.portal.entity.SchoolSubject;
import com.edurite.school.portal.entity.SchoolUserProfile;
import com.edurite.school.portal.entity.TeacherAssignment;
import com.edurite.school.portal.repository.SchoolClassRepository;
import com.edurite.school.portal.repository.SchoolRepository;
import com.edurite.school.portal.repository.SchoolSubjectRepository;
import com.edurite.school.portal.repository.SchoolUserProfileRepository;
import com.edurite.school.portal.repository.TeacherAssignmentRepository;
import com.edurite.user.entity.User;
import com.edurite.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurriculumService {
    private static final Logger log = LoggerFactory.getLogger(CurriculumService.class);

    public static final String OWNER_DISTRICT = "DISTRICT";
    public static final String OWNER_SCHOOL = "SCHOOL";
    public static final String CONTENT_OFFICIAL = "OFFICIAL";
    public static final String CONTENT_SUPPLEMENTARY = "SUPPLEMENTARY";
    public static final String ROLE_SUBJECT_ADVISOR = "ROLE_SUBJECT_ADVISOR";
    private static final Set<String> PDF_CONTENT_TYPES = Set.of("application/pdf", "application/octet-stream");
    private static final Set<String> DOC_CONTENT_TYPES = Set.of(
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/octet-stream"
    );
    private static final Set<String> EXCEL_CONTENT_TYPES = Set.of(
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/csv",
            "application/csv",
            "application/octet-stream"
    );

    private final CurriculumAssetRepository curriculumAssetRepository;
    private final AtpCalendarItemRepository atpCalendarItemRepository;
    private final AtpTeacherReminderRepository atpTeacherReminderRepository;
    private final TeacherAtpProgressRepository teacherAtpProgressRepository;
    private final CurriculumWeekPlanRepository curriculumWeekPlanRepository;
    private final TeacherCurriculumProgressRepository teacherCurriculumProgressRepository;
    private final CurriculumReminderDispatchRepository curriculumReminderDispatchRepository;
    private final CurriculumRiskAlertRepository curriculumRiskAlertRepository;
    private final DistrictRepository districtRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final TeacherAssignmentRepository teacherAssignmentRepository;
    private final SchoolSubjectRepository schoolSubjectRepository;
    private final SchoolUserProfileRepository schoolUserProfileRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final DistrictAdminProfileRepository districtAdminProfileRepository;
    private final CurriculumResourceService curriculumResourceService;
    private final AiAtpExtractionService aiAtpExtractionService;
    private final AiProviderOrchestratorService aiProviderOrchestratorService;
    private final ObjectMapper objectMapper;

    public record CurriculumFileResponse(
            String fileName,
            String contentType,
            byte[] content,
            boolean inline
    ) {}

    public CurriculumService(
            CurriculumAssetRepository curriculumAssetRepository,
            AtpCalendarItemRepository atpCalendarItemRepository,
            AtpTeacherReminderRepository atpTeacherReminderRepository,
            TeacherAtpProgressRepository teacherAtpProgressRepository,
            CurriculumWeekPlanRepository curriculumWeekPlanRepository,
            TeacherCurriculumProgressRepository teacherCurriculumProgressRepository,
            CurriculumReminderDispatchRepository curriculumReminderDispatchRepository,
            CurriculumRiskAlertRepository curriculumRiskAlertRepository,
            DistrictRepository districtRepository,
            SchoolRepository schoolRepository,
            SchoolClassRepository schoolClassRepository,
            TeacherAssignmentRepository teacherAssignmentRepository,
            SchoolSubjectRepository schoolSubjectRepository,
            SchoolUserProfileRepository schoolUserProfileRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            DistrictAdminProfileRepository districtAdminProfileRepository,
            CurriculumResourceService curriculumResourceService,
            AiAtpExtractionService aiAtpExtractionService,
            AiProviderOrchestratorService aiProviderOrchestratorService,
            ObjectMapper objectMapper
    ) {
        this.curriculumAssetRepository = curriculumAssetRepository;
        this.atpCalendarItemRepository = atpCalendarItemRepository;
        this.atpTeacherReminderRepository = atpTeacherReminderRepository;
        this.teacherAtpProgressRepository = teacherAtpProgressRepository;
        this.curriculumWeekPlanRepository = curriculumWeekPlanRepository;
        this.teacherCurriculumProgressRepository = teacherCurriculumProgressRepository;
        this.curriculumReminderDispatchRepository = curriculumReminderDispatchRepository;
        this.curriculumRiskAlertRepository = curriculumRiskAlertRepository;
        this.districtRepository = districtRepository;
        this.schoolRepository = schoolRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.teacherAssignmentRepository = teacherAssignmentRepository;
        this.schoolSubjectRepository = schoolSubjectRepository;
        this.schoolUserProfileRepository = schoolUserProfileRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.districtAdminProfileRepository = districtAdminProfileRepository;
        this.curriculumResourceService = curriculumResourceService;
        this.aiAtpExtractionService = aiAtpExtractionService;
        this.aiProviderOrchestratorService = aiProviderOrchestratorService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<CurriculumDtos.CurriculumAssetDto> districtAssets(UUID districtId, String repositoryType) {
        List<CurriculumAssetSummaryView> assets = (repositoryType == null || repositoryType.isBlank())
                ? curriculumAssetRepository.findActiveDistrictAssetSummaries(districtId)
                : curriculumAssetRepository.findActiveDistrictAssetSummariesByRepositoryType(districtId, normalizeRepositoryType(repositoryType));
        Map<UUID, String> uploaderNames = uploadedByNames(assets.stream()
                .map(CurriculumAssetSummaryView::getUploadedByUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        return assets.stream()
                .map(asset -> curriculumResourceService.toAssetDto(asset, uploaderNames.get(asset.getUploadedByUserId())))
                .toList();
    }

    @Transactional
    public CurriculumDtos.CurriculumAssetDto saveDistrictAsset(UUID districtId, UUID actorUserId, CurriculumDtos.CurriculumAssetUpsertRequest request) {
        try {
            District district = districtRepository.findById(districtId).orElseThrow(() -> new ResourceConflictException("District not found"));
            CurriculumAsset asset = new CurriculumAsset();
            populateAsset(asset, request, actorUserId, OWNER_DISTRICT, CONTENT_OFFICIAL);
            asset.setDistrictId(districtId);
            if (asset.getProvince() == null || asset.getProvince().isBlank()) {
                asset.setProvince(district.getProvince());
            }
            CurriculumAsset saved = curriculumAssetRepository.save(asset);
            logStoredFiles(saved);
            triggerAtpExtraction(saved, actorUserId);
            return toAssetDto(saved);
        } catch (ResourceConflictException ex) {
            log.warn("District curriculum upload rejected districtId={} actorUserId={} repositoryType={} title={} reason={}",
                    districtId, actorUserId, request.repositoryType(), request.title(), ex.getMessage());
            throw ex;
        } catch (RuntimeException ex) {
            log.error("District curriculum upload failed districtId={} actorUserId={} repositoryType={} title={} pdf={} docx={} excel={}",
                    districtId,
                    actorUserId,
                    request.repositoryType(),
                    request.title(),
                    request.pdf() == null ? null : request.pdf().fileName(),
                    request.docx() == null ? null : request.docx().fileName(),
                    request.excel() == null ? null : request.excel().fileName(),
                    ex);
            throw new ResourceConflictException("Unable to save the curriculum asset right now. Confirm the uploaded file is valid and try again.");
        }
    }

    @Transactional
    public CurriculumDtos.CurriculumAssetDto updateDistrictAsset(UUID districtId, UUID actorUserId, UUID assetId, CurriculumDtos.CurriculumAssetUpsertRequest request) {
        try {
            CurriculumAsset asset = requireDistrictAsset(districtId, assetId);
            populateAsset(asset, request, actorUserId, asset.getOwnerScope(), asset.getContentSource());
            CurriculumAsset saved = curriculumAssetRepository.save(asset);
            logStoredFiles(saved);
            triggerAtpExtraction(saved, actorUserId);
            return toAssetDto(saved);
        } catch (ResourceConflictException ex) {
            log.warn("District curriculum update rejected districtId={} actorUserId={} assetId={} repositoryType={} title={} reason={}",
                    districtId, actorUserId, assetId, request.repositoryType(), request.title(), ex.getMessage());
            throw ex;
        } catch (RuntimeException ex) {
            log.error("District curriculum update failed districtId={} actorUserId={} assetId={} repositoryType={} title={}",
                    districtId, actorUserId, assetId, request.repositoryType(), request.title(), ex);
            throw new ResourceConflictException("Unable to update the curriculum asset right now. Confirm the uploaded file is valid and try again.");
        }
    }

    @Transactional
    public CurriculumDtos.CurriculumAssetDto archiveDistrictAsset(UUID districtId, UUID assetId) {
        CurriculumAsset asset = requireDistrictAsset(districtId, assetId);
        asset.setArchived(true);
        asset.setActive(false);
        return toAssetDto(curriculumAssetRepository.save(asset));
    }

    @Transactional
    public void deleteDistrictAsset(UUID districtId, UUID assetId) {
        CurriculumAsset asset = requireDistrictAsset(districtId, assetId);
        curriculumAssetRepository.delete(asset);
    }

    @Transactional(readOnly = true)
    public CurriculumFileResponse downloadDistrictAsset(UUID districtId, UUID assetId, String format) {
        return downloadAsset(requireDistrictAsset(districtId, assetId), format);
    }

    @Transactional
    public CurriculumDtos.CurriculumAssetDto extractDistrictAtp(UUID districtId, UUID actorUserId, UUID assetId) {
        CurriculumAsset asset = requireDistrictAsset(districtId, assetId);
        aiAtpExtractionService.extractAtpCalendarFromPdf(asset.getId(), actorUserId);
        return toAssetDto(curriculumAssetRepository.findById(assetId).orElse(asset));
    }

    @Transactional
    public CurriculumDtos.CurriculumCalendarItemDto saveCalendarItem(UUID districtId, UUID actorUserId, UUID itemId, CurriculumDtos.CurriculumCalendarItemUpsertRequest request) {
        CurriculumAsset asset = requireDistrictAsset(districtId, request.curriculumResourceId());
        AtpCalendarItem item = itemId == null
                ? new AtpCalendarItem()
                : atpCalendarItemRepository.findById(itemId).orElseThrow(() -> new ResourceConflictException("ATP calendar item not found."));
        item.setCurriculumResourceId(asset.getId());
        item.setSubject(request.subject() == null || request.subject().isBlank() ? asset.getSubject() : request.subject().trim());
        item.setGrade(normalizeGrade(request.grade() == null || request.grade().isBlank() ? asset.getGrade() : request.grade()));
        item.setPhase(trim(firstNonBlank(request.phase(), asset.getCurriculumPhase())));
        item.setAcademicYear(request.academicYear() == null ? asset.getAcademicYear() : request.academicYear());
        item.setTerm(request.term().trim());
        item.setWeekNumber(request.weekNumber());
        item.setStartDate(request.startDate());
        item.setEndDate(request.endDate());
        item.setTopic(request.topic().trim());
        item.setSubtopic(trim(request.subtopic()));
        item.setLearningObjectives(trim(request.learningObjectives()));
        item.setResources(trim(request.resources()));
        item.setAssessmentTask(trim(request.assessmentTask()));
        item.setLessonFocus(trim(request.lessonFocus()));
        item.setNotes(trim(request.notes()));
        item.setCreatedBy(actorUserId);
        if (item.getStatus() == null || item.getStatus().isBlank()) {
            item.setStatus("DRAFT");
        }
        AtpCalendarItem saved = atpCalendarItemRepository.save(item);
        return toCalendarDto(saved, asset);
    }

    @Transactional
    public CurriculumDtos.CurriculumCalendarItemDto publishCalendarItem(UUID districtId, UUID actorUserId, UUID itemId) {
        AtpCalendarItem item = requireDistrictCalendarItem(districtId, itemId);
        validateCalendarItemForPublish(item);
        item.setStatus("PUBLISHED");
        AtpCalendarItem saved = atpCalendarItemRepository.save(item);
        CurriculumAsset asset = requireDistrictAsset(districtId, saved.getCurriculumResourceId());
        asset.setExtractionStatus("PUBLISHED");
        curriculumAssetRepository.save(asset);
        syncPublishedAssetWeekPlans(asset);
        createTeacherRemindersForAsset(asset);
        return toCalendarDto(saved, asset);
    }

    @Transactional
    public CurriculumDtos.CurriculumCalendarItemDto archiveCalendarItem(UUID districtId, UUID itemId) {
        AtpCalendarItem item = requireDistrictCalendarItem(districtId, itemId);
        item.setStatus("ARCHIVED");
        AtpCalendarItem saved = atpCalendarItemRepository.save(item);
        CurriculumAsset asset = requireDistrictAsset(districtId, saved.getCurriculumResourceId());
        curriculumWeekPlanRepository.findByCurriculumAssetIdAndTermIgnoreCaseAndWeekNumberAndTopicIgnoreCase(
                asset.getId(), saved.getTerm(), saved.getWeekNumber(), saved.getTopic()
        ).ifPresent(plan -> {
            plan.setActive(false);
            plan.setStatus("ARCHIVED");
            curriculumWeekPlanRepository.save(plan);
        });
        return toCalendarDto(saved, asset);
    }

    @Transactional
    public CurriculumDtos.CurriculumPublishRepairResponse syncPublishedCalendarToSchools(UUID districtId) {
        sanitizePublishedCalendarForDistrict(districtId);
        assetsForDistrict(districtId, "ATP").stream()
                .filter(this::assetVisibleToSchools)
                .forEach(asset -> atpCalendarItemRepository.findByCurriculumResourceIdAndStatusIgnoreCaseOrderByTermAscWeekNumberAsc(asset.getId(), "DRAFT")
                        .stream()
                        .filter(this::isPublishableCalendarItem)
                        .forEach(item -> {
                            item.setStatus("PUBLISHED");
                            atpCalendarItemRepository.save(item);
                            if (!"PUBLISHED".equalsIgnoreCase(asset.getExtractionStatus())) {
                                asset.setExtractionStatus("PUBLISHED");
                                curriculumAssetRepository.save(asset);
                            }
                        }));
        ensurePublishedMappingsForDistrict(districtId);
        long publishedItems = publishedItemsForDistrict(districtId).size();
        long schoolRemindersQueued = schoolRepository.findByDistrictIdOrderBySchoolNameAsc(districtId).stream()
                .mapToLong(school -> atpTeacherReminderRepository.countBySchoolIdAndTeacherIdIsNullAndStatusIgnoreCase(school.getId(), "QUEUED"))
                .sum();
        long teacherRemindersQueued = atpTeacherReminderRepository.countByStatusIgnoreCase("QUEUED") - schoolRemindersQueued;
        long weekPlansSynced = curriculumWeekPlanRepository.findByDistrictIdAndActiveTrueOrderBySubjectAscGradeAscTermAscWeekNumberAsc(districtId).size();
        return new CurriculumDtos.CurriculumPublishRepairResponse(
                publishedItems,
                weekPlansSynced,
                schoolRemindersQueued,
                Math.max(0, teacherRemindersQueued),
                "Published ATP calendar items have been synced to schools and reminders have been recalculated."
        );
    }

    @Transactional
    public CurriculumDtos.DistrictCurriculumCalendarResponse districtCalendar(UUID districtId) {
        sanitizePublishedCalendarForDistrict(districtId);
        List<CurriculumAsset> atpAssets = assetsForDistrict(districtId, "ATP");
        Map<UUID, CurriculumAsset> assetById = atpAssets.stream().collect(Collectors.toMap(CurriculumAsset::getId, item -> item));
        List<CurriculumDtos.CurriculumCalendarItemDto> items = atpCalendarItemRepository.findAll().stream()
                .filter(item -> assetById.containsKey(item.getCurriculumResourceId()))
                .filter(item -> !"ARCHIVED".equalsIgnoreCase(item.getStatus()))
                .filter(item -> !"PUBLISHED".equalsIgnoreCase(item.getStatus()) || isPublishableCalendarItem(item))
                .sorted(Comparator.comparing(AtpCalendarItem::getAcademicYear, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AtpCalendarItem::getTerm)
                        .thenComparing(AtpCalendarItem::getWeekNumber))
                .map(item -> toCalendarDto(item, assetById.get(item.getCurriculumResourceId())))
                .toList();
        List<CurriculumDtos.CurriculumAssetDto> errors = atpAssets.stream()
                .filter(asset -> "EXTRACTION_FAILED".equalsIgnoreCase(asset.getExtractionStatus()))
                .map(this::toAssetDto)
                .toList();
        CurriculumDtos.CurriculumCalendarStatsDto stats = new CurriculumDtos.CurriculumCalendarStatsDto(
                atpAssets.stream().filter(asset -> "EXTRACTED".equalsIgnoreCase(asset.getExtractionStatus()) || "PUBLISHED".equalsIgnoreCase(asset.getExtractionStatus())).count(),
                items.size(),
                items.stream().filter(item -> "PUBLISHED".equalsIgnoreCase(item.status())).count(),
                items.stream().filter(item -> "DRAFT".equalsIgnoreCase(item.status())).count(),
                atpTeacherReminderRepository.count(),
                errors.size()
        );
        return new CurriculumDtos.DistrictCurriculumCalendarResponse(stats, items, errors);
    }

    @Transactional(readOnly = true)
    public CurriculumDtos.CurriculumComplianceResponse districtCompliance(UUID districtId) {
        ComplianceSnapshot snapshot = complianceSnapshot(districtId);
        return new CurriculumDtos.CurriculumComplianceResponse(
                List.of(
                        new DistrictDtos.MetricCardDto("Schools on track", String.valueOf(snapshot.onTrackSchools), "Schools matching expected coverage", "positive"),
                        new DistrictDtos.MetricCardDto("Schools behind ATP", String.valueOf(snapshot.behindSchools), "Schools needing intervention", snapshot.behindSchools > 0 ? "warning" : "positive"),
                        new DistrictDtos.MetricCardDto("Schools ahead of ATP", String.valueOf(snapshot.aheadSchools), "Schools exceeding weekly pace", snapshot.aheadSchools > 0 ? "neutral" : "positive"),
                        new DistrictDtos.MetricCardDto("District compliance", snapshot.districtCompliancePercent + "%", "Average weekly curriculum completion", snapshot.districtCompliancePercent >= 80 ? "positive" : "warning")
                ),
                snapshot.schoolRows,
                snapshot.heatMap,
                snapshot.subjectsBehind,
                snapshot.teachersBehind,
                snapshot.riskAlerts
        );
    }

    @Transactional(readOnly = true)
    public List<CurriculumDtos.CurriculumAssetDto> schoolAssets(UUID schoolId, String repositoryType) {
        return schoolAssetsMerged(schoolId, repositoryType).stream().map(this::toAssetDto).toList();
    }

    @Transactional
    public CurriculumDtos.CurriculumAssetDto saveSchoolAsset(UUID schoolId, UUID actorUserId, CurriculumDtos.CurriculumAssetUpsertRequest request) {
        School school = schoolRepository.findById(schoolId).orElseThrow(() -> new ResourceConflictException("School not found"));
        CurriculumAsset asset = new CurriculumAsset();
        populateAsset(asset, request, actorUserId, OWNER_SCHOOL, CONTENT_SUPPLEMENTARY);
        asset.setSchoolId(schoolId);
        asset.setDistrictId(school.getDistrictId());
        if (asset.getProvince() == null || asset.getProvince().isBlank()) {
            asset.setProvince(school.getProvince());
        }
        CurriculumAsset saved = curriculumAssetRepository.save(asset);
        logStoredFiles(saved);
        if ("ATP".equalsIgnoreCase(saved.getRepositoryType())) {
            triggerAtpExtraction(saved, actorUserId);
        }
        return toAssetDto(saved);
    }

    @Transactional(readOnly = true)
    public CurriculumFileResponse downloadSchoolAsset(UUID schoolId, UUID assetId, String format) {
        CurriculumAsset asset = curriculumAssetRepository.findById(assetId)
                .filter(item -> Objects.equals(item.getSchoolId(), schoolId) || Objects.equals(item.getDistrictId(), schoolRepository.findById(schoolId).map(School::getDistrictId).orElse(null)))
                .orElseThrow(() -> new ResourceConflictException("Curriculum asset not found"));
        return downloadAsset(asset, format);
    }

    @Transactional
    public CurriculumDtos.TeacherCurriculumWidgetResponse teacherWidgets(UUID schoolId, UUID teacherUserId) {
        List<CurriculumDtos.TeacherCoverageItemDto> visibleTopics = teacherVisibleTopics(schoolId, teacherUserId);
        TeacherWeekContext visibleContext = coverageContext(visibleTopics);
        List<CurriculumDtos.CurriculumAssetDto> resources = curriculumResourceService.getDistrictResourcesForTeacher(
                schoolId,
                teacherUserId,
                new CurriculumDtos.CurriculumResourceQuery(null, null, null, null, null, null, null)
        ).stream().limit(5).toList();
        List<CurriculumDtos.CurriculumAssetDto> syllabuses = schoolAssetsMerged(schoolId, "SYLLABUS").stream().map(this::toAssetDto).limit(6).toList();
        List<CurriculumDtos.CurriculumAssetDto> lessonPlans = schoolAssetsMerged(schoolId, "LESSON_PLAN").stream().map(this::toAssetDto).limit(6).toList();
        List<CurriculumDtos.TeacherReminderDto> reminders = teacherReminders(schoolId, teacherUserId);
        long totalPublishedItems = countPublishedItemsForDistrict(schoolId);
        long currentMatches = visibleContext.currentPlans.size();
        return new CurriculumDtos.TeacherCurriculumWidgetResponse(
                visibleContext.currentTopic,
                visibleContext.currentTopic,
                visibleContext.behindTopics,
                visibleTopics,
                visibleContext.upcomingTopics,
                reminders,
                resources,
                syllabuses,
                lessonPlans,
                totalPublishedItems,
                currentMatches,
                visibleContext.currentTopic
        );
    }

    @Transactional
    public CurriculumDtos.SchoolCurriculumCalendarResponse schoolCalendar(UUID schoolId) {
        ensurePublishedMappingsForSchool(schoolId);
        List<CurriculumDtos.TeacherCoverageItemDto> visibleTopics = schoolVisibleTopics(schoolId);
        TeacherWeekContext visibleContext = coverageContext(visibleTopics);
        List<TeacherAssignment> assignments = teacherAssignmentRepository.findBySchoolIdAndActiveTrue(schoolId);
        List<TeacherWeekContext> contexts = assignments.stream()
                .map(assignment -> coverageContext(teacherVisibleTopics(schoolId, assignment.getTeacherUserId())))
                .toList();
        List<CurriculumDtos.TeacherCoverageItemDto> behind = contexts.stream()
                .flatMap(context -> context.behindTopics.stream())
                .distinct()
                .limit(8)
                .toList();
        List<CurriculumDtos.TeacherReminderDto> reminders = schoolReminders(schoolId);
        List<CurriculumDtos.CurriculumAssetDto> resources = curriculumResourceService.getDistrictResourcesForSchool(
                schoolId,
                new CurriculumDtos.CurriculumResourceQuery("ATP", null, null, null, null, null, null)
        ).stream().limit(6).toList();
        long totalPublishedItems = countPublishedItemsForDistrict(schoolId);
        CurriculumDtos.TeacherCoverageItemDto currentWeekAtpItem = visibleContext.currentTopic;
        long itemsWithNullDates = visibleTopics.stream().filter(item -> item.startDate() == null && item.endDate() == null).count();
        long districtWideItems = districtWideVisibleItemCount(schoolId);
        long remindersQueued = atpTeacherReminderRepository.countBySchoolIdAndTeacherIdIsNullAndStatusIgnoreCase(schoolId, "QUEUED");
        return new CurriculumDtos.SchoolCurriculumCalendarResponse(
                currentWeekAtpItem,
                visibleContext.upcomingTopics,
                visibleTopics,
                behind,
                reminders,
                resources,
                totalPublishedItems,
                currentWeekAtpItem,
                visibleContext.currentPlans.size(),
                visibleTopics.size(),
                visibleContext.currentPlans.size(),
                itemsWithNullDates,
                districtWideItems,
                remindersQueued
        );
    }

    @Transactional
    public CurriculumDtos.TeacherCoverageItemDto updateTeacherProgress(UUID schoolId, UUID teacherUserId, UUID weekPlanId, CurriculumDtos.TeacherProgressUpdateRequest request) {
        TeacherWeekContext context = teacherWeekContext(schoolId, teacherUserId);
        CurriculumWeekPlan plan = context.planById.get(weekPlanId);
        if (plan == null) {
            throw new ResourceConflictException("Curriculum week is not assigned to this teacher.");
        }
        TeacherCurriculumProgress progress = teacherCurriculumProgressRepository.findByWeekPlanIdAndTeacherUserId(weekPlanId, teacherUserId).orElseGet(() -> {
            TeacherCurriculumProgress created = new TeacherCurriculumProgress();
            created.setWeekPlanId(weekPlanId);
            created.setTeacherUserId(teacherUserId);
            created.setSchoolId(schoolId);
            created.setSubjectId(context.subjectIdByWeekPlan.get(weekPlanId));
            return created;
        });
        progress.setStatus(request.status().trim().toUpperCase(Locale.ROOT));
        progress.setCompletionPercent(request.completionPercent() == null ? 0 : request.completionPercent());
        progress.setNotes(trim(request.notes()));
        progress.setCompletedAt("COMPLETED".equals(progress.getStatus()) ? OffsetDateTime.now() : null);
        TeacherCurriculumProgress saved = teacherCurriculumProgressRepository.save(progress);
        AtpCalendarItem atpItem = context.atpItemByWeekPlanId.get(weekPlanId);
        if (atpItem != null) {
            TeacherAtpProgress atpProgress = teacherAtpProgressRepository.findByTeacherIdAndSchoolIdAndAtpCalendarItemId(teacherUserId, schoolId, atpItem.getId())
                    .orElseGet(() -> {
                        TeacherAtpProgress created = new TeacherAtpProgress();
                        created.setTeacherId(teacherUserId);
                        created.setSchoolId(schoolId);
                        created.setAtpCalendarItemId(atpItem.getId());
                        return created;
                    });
            atpProgress.setStatus(progress.getStatus());
            atpProgress.setCompletionPercentage(progress.getCompletionPercent());
            atpProgress.setComment(progress.getNotes());
            atpProgress.setCompletedAt(progress.getCompletedAt());
            teacherAtpProgressRepository.save(atpProgress);
        }
        return toCoverageItem(plan, saved, atpItem);
    }

    @Transactional
    public List<CurriculumDtos.TeacherReminderDto> teacherReminders(UUID schoolId, UUID teacherUserId) {
        List<CurriculumDtos.TeacherReminderDto> scheduled = atpTeacherReminderRepository.findByTeacherIdAndReminderDateBetweenOrderByReminderDateAsc(
                        teacherUserId,
                        OffsetDateTime.now().minusDays(1),
                        OffsetDateTime.now().plusDays(7))
                .stream()
                .map(this::toReminderDto)
                .toList();
        if (scheduled.isEmpty()) {
            scheduled = atpTeacherReminderRepository.findBySchoolIdAndTeacherIdIsNullAndReminderDateBetweenOrderByReminderDateAsc(
                            schoolId,
                            OffsetDateTime.now().minusDays(1),
                            OffsetDateTime.now().plusDays(7))
                    .stream()
                    .map(this::toReminderDto)
                    .toList();
        }
        if (!scheduled.isEmpty()) {
            return scheduled;
        }
        TeacherWeekContext context = coverageContext(teacherVisibleTopics(schoolId, teacherUserId));
        return remindersForCoverageItems(teacherUserId, context.currentPlans, LocalDate.now(), false);
    }

    @Transactional
    public CurriculumDtos.TeacherLessonPlanResponse generateLessonPlan(UUID schoolId, UUID teacherUserId, UUID weekPlanId) {
        CurriculumDtos.TeacherCoverageItemDto coverageItem = teacherVisibleTopics(schoolId, teacherUserId).stream()
                .filter(item -> Objects.equals(item.weekPlanId(), weekPlanId))
                .findFirst()
                .orElse(null);
        if (coverageItem == null) {
            throw new ResourceConflictException("Curriculum week is not assigned to this teacher.");
        }
        AtpCalendarItem atpItem = coverageItem.atpCalendarItemId() == null
                ? null
                : atpCalendarItemRepository.findById(coverageItem.atpCalendarItemId()).orElse(null);
        if (atpItem != null) {
            return createLessonPlanFromCalendarItem(schoolId, teacherUserId, atpItem.getId(), false);
        }
        return createLessonPlanFromCoverageItem(schoolId, teacherUserId, coverageItem, null, false);
    }

    @Transactional
    public CurriculumDtos.TeacherLessonPlanResponse createLessonPlanFromCalendarItem(UUID schoolId, UUID teacherUserId, UUID calendarItemId, boolean regenerate) {
        CurriculumDtos.TeacherCoverageItemDto coverageItem = teacherVisibleTopics(schoolId, teacherUserId).stream()
                .filter(item -> Objects.equals(item.atpCalendarItemId(), calendarItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceConflictException("Published ATP calendar item is not available in this teacher's assigned scope."));
        AtpCalendarItem calendarItem = atpCalendarItemRepository.findById(calendarItemId)
                .orElseThrow(() -> new ResourceConflictException("ATP calendar item not found."));
        if (!"PUBLISHED".equalsIgnoreCase(calendarItem.getStatus())) {
            throw new ResourceConflictException("Only published ATP calendar items can be converted into lesson plans.");
        }
        return createLessonPlanFromCoverageItem(schoolId, teacherUserId, coverageItem, calendarItem, regenerate);
    }

    @Transactional
    public void dispatchScheduledRemindersForToday() {
        OffsetDateTime start = LocalDate.now().atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime end = start.plusDays(1).minusSeconds(1);
        atpTeacherReminderRepository.findByStatusIgnoreCaseAndReminderDateBetweenOrderByReminderDateAsc("QUEUED", start, end)
                .forEach(reminder -> {
                    if (reminder.getTeacherId() != null) {
                        notificationService.createInApp(reminder.getTeacherId(), "CURRICULUM_REMINDER", reminder.getSubject() + " ATP Reminder", reminder.getReminderMessage());
                    }
                    reminder.setStatus("SENT");
                    reminder.setSentAt(OffsetDateTime.now());
                    atpTeacherReminderRepository.save(reminder);
                });
    }

    @Transactional
    public void evaluateCurriculumRiskAlerts() {
        for (District district : districtRepository.findAll()) {
            ComplianceSnapshot snapshot = complianceSnapshot(district.getId());
            snapshot.openAlertContexts.forEach(this::ensureRiskAlert);
        }
    }

    private CurriculumAsset requireDistrictAsset(UUID districtId, UUID assetId) {
        return curriculumAssetRepository.findById(assetId)
                .filter(item -> Objects.equals(item.getDistrictId(), districtId))
                .orElseThrow(() -> new ResourceConflictException("Curriculum asset not found"));
    }

    private void populateAsset(CurriculumAsset asset, CurriculumDtos.CurriculumAssetUpsertRequest request, UUID actorUserId, String ownerScope, String contentSource) {
        asset.setOwnerScope(ownerScope);
        asset.setContentSource(contentSource);
        asset.setSource(resolveAssetSource(ownerScope, actorUserId));
        asset.setVisibility(OWNER_DISTRICT.equalsIgnoreCase(ownerScope) ? CurriculumResourceService.VISIBILITY_DISTRICT_WIDE : CurriculumResourceService.VISIBILITY_SCHOOL_ONLY);
        asset.setStatus(CurriculumResourceService.STATUS_ACTIVE);
        asset.setRepositoryType(normalizeRepositoryType(request.repositoryType()));
        asset.setTitle(request.title().trim());
        asset.setSubject(request.subject().trim());
        asset.setGrade(request.grade().trim());
        asset.setCurriculumPhase(trim(request.curriculumPhase()));
        asset.setAcademicYear(request.academicYear());
        asset.setProvince(trim(request.province()));
        asset.setVersionNumber(trim(request.versionNumber()));
        asset.setDescription(trim(request.description()));
        asset.setTerm(trim(request.term()));
        asset.setWeekNumber(request.weekNumber());
        asset.setUploadedByUserId(actorUserId);
        asset.setUploadDate(OffsetDateTime.now());
        asset.setArchived(false);
        asset.setActive(true);
        asset.setDeleted(false);
        applyFile(asset, request.pdf(), "PDF");
        applyFile(asset, request.docx(), "DOCX");
        applyFile(asset, request.excel(), "EXCEL");
    }

    private void applyFile(CurriculumAsset asset, CurriculumDtos.FilePayload file, String slot) {
        if (file == null) {
            return;
        }
        StoredFile storedFile = decodeAndValidateFile(file, slot);
        switch (slot) {
            case "PDF" -> {
                asset.setPdfFileName(storedFile.fileName());
                asset.setPdfContentType(storedFile.contentType());
                asset.setPdfBytes(storedFile.bytes());
                asset.setPdfBase64(null);
            }
            case "DOCX" -> {
                asset.setDocxFileName(storedFile.fileName());
                asset.setDocxContentType(storedFile.contentType());
                asset.setDocxBytes(storedFile.bytes());
                asset.setDocxBase64(null);
            }
            case "EXCEL" -> {
                asset.setExcelFileName(storedFile.fileName());
                asset.setExcelContentType(storedFile.contentType());
                asset.setExcelBytes(storedFile.bytes());
                asset.setExcelBase64(null);
            }
            default -> {
            }
        }
    }

    private CurriculumFileResponse downloadAsset(CurriculumAsset asset, String format) {
        String normalized = format == null ? "PDF" : format.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "PDF" -> requireFile(asset, asset.getPdfFileName(), asset.getPdfContentType(), asset.getPdfBytes(), asset.getPdfBase64(), false, "PDF");
            case "DOC", "DOCX" -> requireFile(asset, asset.getDocxFileName(), asset.getDocxContentType(), asset.getDocxBytes(), asset.getDocxBase64(), false, "DOCX");
            case "EXCEL", "XLS", "XLSX" -> requireFile(asset, asset.getExcelFileName(), asset.getExcelContentType(), asset.getExcelBytes(), asset.getExcelBase64(), false, "Excel");
            default -> throw new ResourceConflictException("Unsupported format requested.");
        };
    }

    private CurriculumFileResponse requireFile(CurriculumAsset asset, String fileName, String contentType, byte[] fileBytes, String legacyBase64Content, boolean inline, String label) {
        byte[] content = resolveFileBytes(fileBytes, legacyBase64Content);
        if (content == null || content.length == 0) {
            log.warn("Curriculum download failed assetId={} label={} fileName={} contentType={} status=MISSING_FILE", asset.getId(), label, fileName, contentType);
            throw new ResourceConflictException(label + " file is not available for this asset.");
        }
        log.info("Curriculum download assetId={} fileName={} contentType={} fileSize={} status=SUCCESS", asset.getId(), firstNonBlank(fileName, label.toLowerCase(Locale.ROOT) + "-resource"), firstNonBlank(contentType, defaultContentType(label)), content.length);
        return new CurriculumFileResponse(
                firstNonBlank(fileName, label.toLowerCase(Locale.ROOT) + "-resource"),
                firstNonBlank(contentType, defaultContentType(label)),
                content,
                inline
        );
    }

    private StoredFile decodeAndValidateFile(CurriculumDtos.FilePayload file, String slot) {
        String fileName = trim(file.fileName());
        String contentType = normalizeContentType(file.contentType());
        String base64Content = trim(file.base64Content());
        if (fileName == null || fileName.isBlank()) {
            throw new ResourceConflictException(slot + " upload is missing a file name.");
        }
        if (base64Content == null || base64Content.isBlank()) {
            throw new ResourceConflictException(fileName + " is empty.");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64Content);
        } catch (IllegalArgumentException ex) {
            throw new ResourceConflictException(fileName + " is not a valid uploaded file.");
        }
        if (bytes.length == 0) {
            throw new ResourceConflictException(fileName + " is empty.");
        }
        validateSupportedFile(fileName, contentType, slot);
        return new StoredFile(fileName, contentType, bytes);
    }

    private void validateSupportedFile(String fileName, String contentType, String slot) {
        String extension = fileExtension(fileName);
        boolean supported = switch (slot) {
            case "PDF" -> "pdf".equals(extension) && PDF_CONTENT_TYPES.contains(contentType);
            case "DOCX" -> Set.of("doc", "docx").contains(extension) && DOC_CONTENT_TYPES.contains(contentType);
            case "EXCEL" -> Set.of("xls", "xlsx", "csv").contains(extension) && EXCEL_CONTENT_TYPES.contains(contentType);
            default -> false;
        };
        if (!supported) {
            throw new ResourceConflictException("Unsupported file format for " + fileName + ".");
        }
    }

    private String normalizeContentType(String value) {
        String normalized = trim(value);
        return normalized == null || normalized.isBlank() ? "application/octet-stream" : normalized.trim().toLowerCase(Locale.ROOT);
    }

    private String fileExtension(String fileName) {
        int index = fileName == null ? -1 : fileName.lastIndexOf('.');
        return index < 0 ? "" : fileName.substring(index + 1).trim().toLowerCase(Locale.ROOT);
    }

    private String defaultContentType(String label) {
        return switch (label.toUpperCase(Locale.ROOT)) {
            case "PDF" -> "application/pdf";
            case "DOCX" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "EXCEL" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default -> "application/octet-stream";
        };
    }

    private byte[] resolveFileBytes(byte[] binaryContent, String legacyBase64Content) {
        if (binaryContent != null && binaryContent.length > 0) {
            return binaryContent;
        }
        if (legacyBase64Content == null || legacyBase64Content.isBlank()) {
            return null;
        }
        try {
            return Base64.getDecoder().decode(legacyBase64Content);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean hasFileContent(byte[] binaryContent, String legacyBase64Content) {
        byte[] content = resolveFileBytes(binaryContent, legacyBase64Content);
        return content != null && content.length > 0;
    }

    private void logStoredFiles(CurriculumAsset asset) {
        logStoredFile(asset.getId(), asset.getPdfFileName(), asset.getPdfContentType(), resolveFileBytes(asset.getPdfBytes(), asset.getPdfBase64()));
        logStoredFile(asset.getId(), asset.getDocxFileName(), asset.getDocxContentType(), resolveFileBytes(asset.getDocxBytes(), asset.getDocxBase64()));
        logStoredFile(asset.getId(), asset.getExcelFileName(), asset.getExcelContentType(), resolveFileBytes(asset.getExcelBytes(), asset.getExcelBase64()));
    }

    private void logStoredFile(UUID resourceId, String fileName, String contentType, byte[] bytes) {
        if (bytes == null || bytes.length == 0 || fileName == null || fileName.isBlank()) {
            return;
        }
        log.info("Curriculum upload stored resourceId={} fileName={} contentType={} fileSize={}", resourceId, fileName, contentType, bytes.length);
    }

    private record StoredFile(String fileName, String contentType, byte[] bytes) {}

    private List<CurriculumAsset> assetsForDistrict(UUID districtId, String repositoryType) {
        return (repositoryType == null || repositoryType.isBlank()
                ? curriculumAssetRepository.findByDistrictIdAndArchivedFalseOrderByRepositoryTypeAscSubjectAscGradeAscUploadDateDesc(districtId)
                : curriculumAssetRepository.findByDistrictIdAndRepositoryTypeIgnoreCaseAndArchivedFalseOrderBySubjectAscGradeAscUploadDateDesc(districtId, normalizeRepositoryType(repositoryType))).stream()
                .filter(asset -> asset.isActive() && !asset.isDeleted() && CurriculumResourceService.STATUS_ACTIVE.equalsIgnoreCase(nullSafe(asset.getStatus(), CurriculumResourceService.STATUS_ACTIVE)))
                .toList();
    }

    private List<CurriculumAsset> schoolAssetsMerged(UUID schoolId, String repositoryType) {
        School school = schoolRepository.findById(schoolId).orElseThrow(() -> new ResourceConflictException("School not found"));
        List<CurriculumAsset> districtAssets = school.getDistrictId() == null ? List.of() : assetsForDistrict(school.getDistrictId(), repositoryType);
        List<CurriculumAsset> schoolAssets = repositoryType == null || repositoryType.isBlank()
                ? curriculumAssetRepository.findBySchoolIdAndArchivedFalseOrderByRepositoryTypeAscSubjectAscGradeAscUploadDateDesc(schoolId)
                : curriculumAssetRepository.findBySchoolIdAndRepositoryTypeIgnoreCaseAndArchivedFalseOrderBySubjectAscGradeAscUploadDateDesc(schoolId, normalizeRepositoryType(repositoryType));
        Map<UUID, CurriculumAsset> merged = new LinkedHashMap<>();
        districtAssets.stream()
                .sorted(Comparator.comparing((CurriculumAsset item) -> !"OFFICIAL".equals(item.getContentSource())))
                .forEach(asset -> merged.put(asset.getId(), asset));
        schoolAssets.stream()
                .filter(asset -> asset.isActive() && !asset.isDeleted() && CurriculumResourceService.STATUS_ACTIVE.equalsIgnoreCase(nullSafe(asset.getStatus(), CurriculumResourceService.STATUS_ACTIVE)))
                .forEach(asset -> merged.put(asset.getId(), asset));
        return new ArrayList<>(merged.values());
    }

    private CurriculumDtos.CurriculumAssetDto toAssetDto(CurriculumAsset asset) {
        return curriculumResourceService.toAssetDto(asset);
    }

    private String uploadedByName(UUID userId) {
        if (userId == null) {
            return "System";
        }
        return userRepository.findById(userId)
                .map(user -> {
                    String fullName = (nullSafe(user.getFirstName(), "") + " " + nullSafe(user.getLastName(), "")).trim();
                    return fullName.isBlank() ? user.getEmail() : fullName;
                })
                .orElse("System");
    }

    private Map<UUID, String> uploadedByNames(Set<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, this::fullName));
    }

    private void regenerateWeekPlans(CurriculumAsset asset) {
        if (!"ATP".equalsIgnoreCase(asset.getRepositoryType())) {
            return;
        }
        curriculumWeekPlanRepository.deleteByCurriculumAssetId(asset.getId());
        for (GeneratedWeek item : generateWeeks(asset)) {
            CurriculumWeekPlan weekPlan = new CurriculumWeekPlan();
            weekPlan.setCurriculumAssetId(asset.getId());
            weekPlan.setDistrictId(asset.getDistrictId());
            weekPlan.setSchoolId(asset.getSchoolId());
            weekPlan.setSubject(asset.getSubject());
            weekPlan.setGrade(asset.getGrade());
            weekPlan.setCurriculumPhase(asset.getCurriculumPhase());
            weekPlan.setAcademicYear(asset.getAcademicYear());
            weekPlan.setProvince(asset.getProvince());
            weekPlan.setTerm(item.term);
            weekPlan.setWeekNumber(item.weekNumber);
            weekPlan.setTopic(item.topic);
            weekPlan.setSubtopic(item.subtopic);
            weekPlan.setLearningOutcomes(item.learningOutcomes);
            weekPlan.setAssessmentActivities(item.assessmentActivities);
            weekPlan.setExpectedCompletionLabel(item.term + " Week " + item.weekNumber);
            curriculumWeekPlanRepository.save(weekPlan);
        }
    }

    private List<GeneratedWeek> generateWeeks(CurriculumAsset asset) {
        List<String> seedTopics = topicSeed(asset.getSubject());
        List<GeneratedWeek> items = new ArrayList<>();
        for (int term = 1; term <= 4; term++) {
            for (int week = 1; week <= 10; week++) {
                int index = ((term - 1) * 10 + week - 1) % seedTopics.size();
                String topic = seedTopics.get(index);
                String subtopic = topic + " practice focus " + week;
                items.add(new GeneratedWeek(
                        "Term " + term,
                        week,
                        topic,
                        subtopic,
                        "Learners should demonstrate competency in " + topic + " for " + asset.getGrade() + ".",
                        "Short assessment, workbook task, and teacher observation aligned to " + topic + "."
                ));
            }
        }
        return items;
    }

    private List<String> topicSeed(String subject) {
        String normalized = subject == null ? "" : subject.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("math")) {
            return List.of("Whole Numbers", "Place Value", "Fractions", "Decimals", "Measurement", "Geometry", "Patterns", "Data Handling");
        }
        if (normalized.contains("english") || normalized.contains("language")) {
            return List.of("Reading Comprehension", "Vocabulary", "Grammar", "Writing", "Listening and Speaking", "Literature");
        }
        if (normalized.contains("science") || normalized.contains("natural")) {
            return List.of("Scientific Inquiry", "Matter and Materials", "Energy and Change", "Life and Living", "Planet Earth");
        }
        if (normalized.contains("history") || normalized.contains("social")) {
            return List.of("Historical Sources", "Time and Chronology", "Community Studies", "Civic Responsibility", "Change Over Time");
        }
        return List.of("Core Concepts", "Foundational Skills", "Applied Practice", "Revision and Enrichment", "Assessment Readiness");
    }

    private ComplianceSnapshot complianceSnapshot(UUID districtId) {
        List<School> schools = schoolRepository.findByDistrictIdOrderBySchoolNameAsc(districtId);
        Map<UUID, User> userById = userRepository.findAll().stream().collect(Collectors.toMap(User::getId, item -> item));
        Map<UUID, SchoolSubject> subjectById = schoolSubjectRepository.findAll().stream().collect(Collectors.toMap(SchoolSubject::getId, item -> item));
        int districtPercentAccumulator = 0;
        int schoolCount = 0;
        long onTrack = 0;
        long behind = 0;
        long ahead = 0;
        List<CurriculumDtos.CurriculumComplianceSchoolDto> schoolRows = new ArrayList<>();
        List<CurriculumDtos.CurriculumHeatMapItemDto> heatMap = new ArrayList<>();
        Map<String, Long> subjectBehind = new LinkedHashMap<>();
        List<DistrictDtos.InsightItemDto> teacherBehind = new ArrayList<>();
        List<AlertContext> openAlerts = new ArrayList<>();

        for (School school : schools) {
            List<TeacherAssignment> assignments = teacherAssignmentRepository.findBySchoolIdAndActiveTrue(school.getId());
            if (assignments.isEmpty()) {
                continue;
            }
            Map<UUID, List<TeacherAssignment>> assignmentByTeacher = assignments.stream().collect(Collectors.groupingBy(TeacherAssignment::getTeacherUserId));
            int schoolPercentAccumulator = 0;
            int teacherCount = 0;
            Set<String> schoolSubjectsBehind = new HashSet<>();
            long schoolTeachersBehind = 0;
            for (Map.Entry<UUID, List<TeacherAssignment>> entry : assignmentByTeacher.entrySet()) {
                TeacherWeekContext context = teacherWeekContext(school.getId(), entry.getKey());
                int completionPercent = context.currentTopic == null ? 100 : context.currentTopic.progressPercent();
                schoolPercentAccumulator += completionPercent;
                teacherCount++;
                if (!context.behindTopics.isEmpty()) {
                    schoolTeachersBehind++;
                    User teacher = userById.get(entry.getKey());
                    teacherBehind.add(new DistrictDtos.InsightItemDto(
                            teacher == null ? "Teacher" : fullName(teacher),
                            school.getSchoolName() + " | " + context.behindTopics.size() + " topics behind schedule",
                            "warning"
                    ));
                    for (CurriculumDtos.TeacherCoverageItemDto behindTopic : context.behindTopics) {
                        schoolSubjectsBehind.add(behindTopic.subject());
                        subjectBehind.merge(behindTopic.subject(), 1L, Long::sum);
                        CurriculumWeekPlan plan = context.planById.get(behindTopic.weekPlanId());
                        if (plan != null) {
                            openAlerts.add(new AlertContext(districtId, school, entry.getKey(), plan, context.subjectIdByWeekPlan.get(plan.getId()), teacher));
                        }
                    }
                }
                String status = completionPercent >= 85 ? "On Track" : completionPercent >= 60 ? "Slightly Behind" : "Critical";
                String tone = completionPercent >= 85 ? "positive" : completionPercent >= 60 ? "warning" : "critical";
                for (TeacherAssignment assignment : entry.getValue()) {
                    SchoolSubject subject = subjectById.get(assignment.getSubjectId());
                    heatMap.add(new CurriculumDtos.CurriculumHeatMapItemDto(
                            school.getSchoolName(),
                            subject == null ? "Subject" : subject.getSubjectName(),
                            status,
                            tone,
                            completionPercent
                    ));
                }
            }
            int schoolCompliance = teacherCount == 0 ? 100 : Math.round((float) schoolPercentAccumulator / teacherCount);
            schoolRows.add(new CurriculumDtos.CurriculumComplianceSchoolDto(
                    school.getId(),
                    school.getSchoolName(),
                    schoolCompliance,
                    schoolCompliance >= 85 ? "On Track" : schoolCompliance >= 60 ? "Slightly Behind" : "Critical",
                    schoolTeachersBehind,
                    schoolSubjectsBehind.size()
            ));
            districtPercentAccumulator += schoolCompliance;
            schoolCount++;
            if (schoolCompliance >= 85) {
                onTrack++;
            } else if (schoolCompliance >= 100) {
                ahead++;
            } else {
                behind++;
            }
        }

        int districtCompliancePercent = schoolCount == 0 ? 100 : Math.round((float) districtPercentAccumulator / schoolCount);
        List<DistrictDtos.InsightItemDto> subjectRows = subjectBehind.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(8)
                .map(entry -> new DistrictDtos.InsightItemDto(entry.getKey(), entry.getValue() + " classes behind ATP pacing", "warning"))
                .toList();
        List<CurriculumDtos.CurriculumRiskAlertDto> alertRows = curriculumRiskAlertRepository.findByDistrictIdAndStatusIgnoreCaseOrderByCreatedAtDesc(districtId, "OPEN").stream()
                .map(alert -> toRiskAlertDto(alert, schools, userById, subjectById))
                .toList();

        return new ComplianceSnapshot(
                districtCompliancePercent,
                onTrack,
                behind,
                ahead,
                schoolRows.stream().sorted(Comparator.comparingInt(CurriculumDtos.CurriculumComplianceSchoolDto::compliancePercent).reversed()).toList(),
                heatMap.stream().sorted(Comparator.comparing(CurriculumDtos.CurriculumHeatMapItemDto::schoolName).thenComparing(CurriculumDtos.CurriculumHeatMapItemDto::subject)).toList(),
                subjectRows,
                teacherBehind.stream().limit(10).toList(),
                alertRows,
                openAlerts
        );
    }

    private CurriculumDtos.CurriculumRiskAlertDto toRiskAlertDto(CurriculumRiskAlert alert, List<School> schools, Map<UUID, User> userById, Map<UUID, SchoolSubject> subjectById) {
        School school = schools.stream().filter(item -> item.getId().equals(alert.getSchoolId())).findFirst().orElse(null);
        User teacher = userById.get(alert.getTeacherUserId());
        SchoolSubject subject = alert.getSubjectId() == null ? null : subjectById.get(alert.getSubjectId());
        CurriculumWeekPlan plan = curriculumWeekPlanRepository.findById(alert.getWeekPlanId()).orElse(null);
        return new CurriculumDtos.CurriculumRiskAlertDto(
                alert.getId(),
                alert.getSchoolId(),
                school == null ? "School" : school.getSchoolName(),
                alert.getTeacherUserId(),
                teacher == null ? "Teacher" : fullName(teacher),
                subject == null ? (plan == null ? "Subject" : plan.getSubject()) : subject.getSubjectName(),
                plan == null ? "" : plan.getGrade(),
                alert.getTitle(),
                alert.getDetail(),
                alert.getSeverity(),
                alert.getCreatedAt()
        );
    }

    private TeacherWeekContext teacherWeekContext(UUID schoolId, UUID teacherUserId) {
        ensurePublishedMappingsForSchool(schoolId);
        List<TeacherAssignment> assignments = teacherAssignmentRepository.findBySchoolIdAndTeacherUserIdAndActiveTrue(schoolId, teacherUserId);
        Map<UUID, SchoolSubject> subjectById = schoolSubjectRepository.findBySchoolIdAndActiveTrue(schoolId).stream().collect(Collectors.toMap(SchoolSubject::getId, item -> item));
        List<CurriculumWeekPlan> plans = new ArrayList<>();
        Map<UUID, UUID> subjectIdByWeekPlan = new HashMap<>();
        Map<UUID, AtpCalendarItem> atpItemByWeekPlan = new HashMap<>();
        WeekMarker marker = weekMarker(LocalDate.now());
        UUID districtId = schoolRepository.findById(schoolId).map(School::getDistrictId).orElse(null);
        List<AtpCalendarItem> publishedDistrictItems = districtId == null
                ? List.of()
                : publishedItemsForDistrict(districtId);
        for (TeacherAssignment assignment : assignments) {
            SchoolSubject subject = subjectById.get(assignment.getSubjectId());
            if (subject == null) {
                continue;
            }
            if (districtId == null) {
                continue;
            }
            List<CurriculumWeekPlan> subjectPlans = curriculumWeekPlanRepository.findByDistrictIdAndActiveTrueOrderBySubjectAscGradeAscTermAscWeekNumberAsc(districtId).stream()
                    .filter(plan -> subjectMatchesPlan(subject.getSubjectName(), plan.getSubject()))
                    .filter(plan -> gradeMatchesPlan(firstNonBlank(assignment.getGrade(), firstNonBlank(subject.getGrade(), subject.getGradeRange())), plan.getGrade()))
                    .toList();
            for (CurriculumWeekPlan plan : subjectPlans) {
                if (plans.stream().noneMatch(item -> item.getId().equals(plan.getId()))) {
                    plans.add(plan);
                    subjectIdByWeekPlan.put(plan.getId(), subject.getId());
                    publishedDistrictItems.stream()
                            .filter(item -> subjectMatchesPlan(item.getSubject(), plan.getSubject()))
                            .filter(item -> gradeMatchesPlan(item.getGrade(), plan.getGrade()))
                            .filter(item -> item.getWeekNumber() != null && item.getWeekNumber().equals(plan.getWeekNumber()))
                            .filter(item -> normalize(item.getTerm()).equals(normalize(plan.getTerm())))
                            .findFirst()
                            .ifPresent(item -> atpItemByWeekPlan.put(plan.getId(), item));
                }
            }
        }
        Map<UUID, TeacherCurriculumProgress> progressByWeekPlan = teacherCurriculumProgressRepository.findByWeekPlanIdInAndTeacherUserId(plans.stream().map(CurriculumWeekPlan::getId).toList(), teacherUserId)
                .stream()
                .collect(Collectors.toMap(TeacherCurriculumProgress::getWeekPlanId, item -> item));
        Map<UUID, CurriculumWeekPlan> planById = plans.stream().collect(Collectors.toMap(CurriculumWeekPlan::getId, item -> item));
        List<CurriculumWeekPlan> currentPlans = plans.stream()
                .filter(plan -> marker.term.equalsIgnoreCase(plan.getTerm()) && plan.getWeekNumber() != null && marker.weekNumber == plan.getWeekNumber())
                .toList();
        CurriculumDtos.TeacherCoverageItemDto currentTopic = currentPlans.stream()
                .findFirst()
                .map(plan -> toCoverageItem(plan, progressByWeekPlan.get(plan.getId()), atpItemByWeekPlan.get(plan.getId())))
                .orElse(null);
        List<CurriculumDtos.TeacherCoverageItemDto> behindTopics = plans.stream()
                .filter(plan -> isEarlierThanCurrent(plan, marker))
                .map(plan -> Map.entry(plan, progressByWeekPlan.get(plan.getId())))
                .filter(entry -> entry.getValue() == null || !"COMPLETED".equalsIgnoreCase(entry.getValue().getStatus()))
                .map(entry -> toCoverageItem(entry.getKey(), entry.getValue(), atpItemByWeekPlan.get(entry.getKey().getId())))
                .sorted(Comparator.comparing(CurriculumDtos.TeacherCoverageItemDto::term).thenComparing(CurriculumDtos.TeacherCoverageItemDto::weekNumber))
                .toList();
        List<CurriculumDtos.TeacherCoverageItemDto> upcomingTopics = plans.stream()
                .filter(plan -> isCurrentOrFuturePlan(plan, marker))
                .map(plan -> toCoverageItem(plan, progressByWeekPlan.get(plan.getId()), atpItemByWeekPlan.get(plan.getId())))
                .filter(item -> currentTopic == null || !item.weekPlanId().equals(currentTopic.weekPlanId()))
                .sorted(Comparator.comparing(CurriculumDtos.TeacherCoverageItemDto::term).thenComparing(CurriculumDtos.TeacherCoverageItemDto::weekNumber))
                .limit(8)
                .toList();

        return new TeacherWeekContext(currentTopic, currentPlans, behindTopics, upcomingTopics, planById, subjectIdByWeekPlan, atpItemByWeekPlan);
    }

    private List<CurriculumDtos.TeacherCoverageItemDto> schoolVisibleTopics(UUID schoolId) {
        ensurePublishedMappingsForSchool(schoolId);
        UUID districtId = schoolRepository.findById(schoolId).map(School::getDistrictId).orElse(null);
        if (districtId == null) {
            return List.of();
        }
        List<AtpCalendarItem> publishedItems = publishedItemsForDistrict(districtId);
        Map<String, AtpCalendarItem> atpItemByKey = publishedItems.stream()
                .collect(Collectors.toMap(this::coverageKey, item -> item, (left, right) -> choosePreferredItem(left, right), LinkedHashMap::new));
        Map<UUID, CurriculumAsset> assetById = assetsForDistrict(districtId, "ATP").stream()
                .collect(Collectors.toMap(CurriculumAsset::getId, item -> item, (left, right) -> right));
        List<CurriculumWeekPlan> visiblePlans = curriculumWeekPlanRepository.findByDistrictIdAndActiveTrueOrderBySubjectAscGradeAscTermAscWeekNumberAsc(districtId).stream()
                .filter(plan -> plan.getSchoolId() == null || Objects.equals(plan.getSchoolId(), schoolId))
                .toList();
        Integer preferredAcademicYear = resolvePreferredAcademicYear(
                visiblePlans.stream().map(CurriculumWeekPlan::getAcademicYear).toList(),
                publishedItems.stream().map(AtpCalendarItem::getAcademicYear).toList()
        );
        return visiblePlans.stream()
                .filter(plan -> matchesAcademicYear(plan.getAcademicYear(), preferredAcademicYear))
                .map(plan -> toCoverageItem(plan, null, atpItemByKey.get(coverageKey(plan)), assetById.get(plan.getCurriculumAssetId())))
                .filter(this::isUsableCoverageItem)
                .sorted(Comparator.comparing(CurriculumDtos.TeacherCoverageItemDto::subject)
                        .thenComparing(CurriculumDtos.TeacherCoverageItemDto::grade)
                        .thenComparing(item -> item.academicYear() == null ? 0 : item.academicYear(), Comparator.reverseOrder())
                        .thenComparing(CurriculumDtos.TeacherCoverageItemDto::term)
                        .thenComparing(CurriculumDtos.TeacherCoverageItemDto::weekNumber))
                .toList();
    }

    private List<CurriculumDtos.TeacherCoverageItemDto> teacherVisibleTopics(UUID schoolId, UUID teacherUserId) {
        List<CurriculumDtos.TeacherCoverageItemDto> visibleTopics = schoolVisibleTopics(schoolId);
        List<TeacherAssignment> assignments = teacherAssignmentRepository.findBySchoolIdAndTeacherUserIdAndActiveTrue(schoolId, teacherUserId);
        if (assignments.isEmpty()) {
            return visibleTopics;
        }
        Map<UUID, SchoolSubject> subjectById = schoolSubjectRepository.findBySchoolIdAndActiveTrue(schoolId).stream()
                .collect(Collectors.toMap(SchoolSubject::getId, item -> item));
        List<CurriculumDtos.TeacherCoverageItemDto> matched = visibleTopics.stream()
                .filter(item -> assignments.stream().anyMatch(assignment -> {
                    SchoolSubject subject = subjectById.get(assignment.getSubjectId());
                    if (subject == null) {
                        return false;
                    }
                    return subjectMatchesPlan(subject.getSubjectName(), item.subject())
                            && gradeMatchesPlan(firstNonBlank(assignment.getGrade(), firstNonBlank(subject.getGrade(), subject.getGradeRange())), item.grade())
                            && phaseMatchesPlan(firstNonBlank(assignment.getPhase(), subject.getPhase()), item.phase());
                }))
                .toList();
        return matched.isEmpty() ? visibleTopics : matched;
    }

    private TeacherWeekContext coverageContext(List<CurriculumDtos.TeacherCoverageItemDto> visibleTopics) {
        WeekMarker marker = weekMarker(LocalDate.now());
        List<CurriculumDtos.TeacherCoverageItemDto> currentMatches = visibleTopics.stream()
                .filter(item -> matchesCurrentWeek(item, marker))
                .toList();
        CurriculumDtos.TeacherCoverageItemDto currentTopic = currentMatches.stream().findFirst().orElse(null);
        List<CurriculumDtos.TeacherCoverageItemDto> behindTopics = visibleTopics.stream()
                .filter(item -> isEarlierThanCurrent(item, marker))
                .toList();
        List<CurriculumDtos.TeacherCoverageItemDto> upcomingTopics = visibleTopics.stream()
                .filter(item -> isCurrentOrFuture(item, marker))
                .filter(item -> currentTopic == null || !item.weekPlanId().equals(currentTopic.weekPlanId()))
                .limit(12)
                .toList();
        List<CurriculumWeekPlan> currentPlans = currentMatches.stream()
                .map(item -> {
                    CurriculumWeekPlan plan = new CurriculumWeekPlan();
                    plan.setId(item.weekPlanId());
                    plan.setSubject(item.subject());
                    plan.setGrade(item.grade());
                    plan.setTerm(item.term());
                    plan.setWeekNumber(item.weekNumber());
                    return plan;
                })
                .toList();
        return new TeacherWeekContext(currentTopic, currentPlans, behindTopics, upcomingTopics, Map.of(), Map.of(), Map.of());
    }

    private boolean isEarlierThanCurrent(CurriculumWeekPlan plan, WeekMarker marker) {
        int termNumber = extractTermNumber(plan.getTerm());
        int currentTerm = extractTermNumber(marker.term);
        if (termNumber < currentTerm) {
            return true;
        }
        return termNumber == currentTerm && plan.getWeekNumber() != null && plan.getWeekNumber() < marker.weekNumber;
    }

    private boolean isEarlierThanCurrent(CurriculumDtos.TeacherCoverageItemDto item, WeekMarker marker) {
        int termNumber = extractTermNumber(item.term());
        int currentTerm = extractTermNumber(marker.term);
        if (termNumber < currentTerm) {
            return true;
        }
        return termNumber == currentTerm && item.weekNumber() != null && item.weekNumber() < marker.weekNumber;
    }

    private boolean isCurrentOrFuturePlan(CurriculumWeekPlan plan, WeekMarker marker) {
        return !isEarlierThanCurrent(plan, marker);
    }

    private boolean isCurrentOrFuture(CurriculumDtos.TeacherCoverageItemDto item, WeekMarker marker) {
        return !isEarlierThanCurrent(item, marker);
    }

    private boolean matchesCurrentWeek(CurriculumDtos.TeacherCoverageItemDto item, WeekMarker marker) {
        LocalDate today = LocalDate.now();
        if (item.startDate() != null && item.endDate() != null) {
            return (today.isEqual(item.startDate()) || today.isAfter(item.startDate()))
                    && (today.isEqual(item.endDate()) || today.isBefore(item.endDate()));
        }
        return normalize(item.term()).equals(normalize(marker.term)) && Objects.equals(item.weekNumber(), marker.weekNumber);
    }

    private CurriculumDtos.TeacherCoverageItemDto toCoverageItem(CurriculumWeekPlan plan, TeacherCurriculumProgress progress, AtpCalendarItem atpItem) {
        return toCoverageItem(plan, progress, atpItem, null);
    }

    private CurriculumDtos.TeacherCoverageItemDto toCoverageItem(
            CurriculumWeekPlan plan,
            TeacherCurriculumProgress progress,
            AtpCalendarItem atpItem,
            CurriculumAsset asset
    ) {
        String status = progress == null ? "NOT_STARTED" : progress.getStatus();
        int completion = progress == null || progress.getCompletionPercent() == null ? ("COMPLETED".equalsIgnoreCase(status) ? 100 : 0) : progress.getCompletionPercent();
        return new CurriculumDtos.TeacherCoverageItemDto(
                plan.getId(),
                plan.getCurriculumAssetId(),
                atpItem == null ? null : atpItem.getId(),
                plan.getSubject(),
                plan.getGrade(),
                firstNonBlank(plan.getCurriculumPhase(), atpItem == null ? null : atpItem.getPhase()),
                firstNonNull(plan.getAcademicYear(), atpItem == null ? null : atpItem.getAcademicYear()),
                plan.getTerm(),
                plan.getWeekNumber(),
                firstNonNull(plan.getStartDate(), atpItem == null ? null : atpItem.getStartDate()),
                firstNonNull(plan.getEndDate(), atpItem == null ? null : atpItem.getEndDate()),
                plan.getTopic(),
                plan.getSubtopic(),
                firstNonBlank(plan.getLearningOutcomes(), atpItem == null ? null : atpItem.getLearningObjectives()),
                firstNonBlank(plan.getResourcesMaterials(), atpItem == null ? null : atpItem.getResources()),
                firstNonBlank(plan.getAssessmentActivities(), atpItem == null ? null : atpItem.getAssessmentTask()),
                firstNonBlank(plan.getLessonFocus(), atpItem == null ? null : atpItem.getLessonFocus()),
                firstNonBlank(plan.getNotes(), atpItem == null ? null : atpItem.getNotes()),
                asset == null ? null : asset.getTitle(),
                status,
                completion,
                plan.getExpectedCompletionLabel()
        );
    }

    private List<CurriculumDtos.TeacherReminderDto> remindersForDate(UUID teacherUserId, List<CurriculumWeekPlan> currentPlans, LocalDate date, boolean dispatch) {
        if (currentPlans.isEmpty()) {
            return List.of();
        }
        String reminderType = switch (date.getDayOfWeek()) {
            case MONDAY -> "MONDAY";
            case WEDNESDAY -> "WEDNESDAY";
            case FRIDAY -> "FRIDAY";
            default -> "INFO";
        };
        List<CurriculumDtos.TeacherReminderDto> reminders = currentPlans.stream().map(plan -> {
            String title;
            String message;
            if ("MONDAY".equals(reminderType)) {
                title = "Weekly curriculum start";
                message = "Week " + plan.getWeekNumber() + " " + plan.getSubject() + ": " + plan.getTopic() + " should be covered this week.";
            } else if ("WEDNESDAY".equals(reminderType)) {
                title = "Curriculum progress check";
                message = "Curriculum progress check for " + plan.getSubject() + " " + plan.getTerm() + " Week " + plan.getWeekNumber() + ".";
            } else if ("FRIDAY".equals(reminderType)) {
                title = "Mark topic as completed";
                message = "Before close of week, mark " + plan.getSubject() + " topic " + plan.getTopic() + " as completed.";
            } else {
                title = "Current curriculum week";
                message = plan.getSubject() + " " + plan.getTerm() + " Week " + plan.getWeekNumber() + ": " + plan.getTopic() + ".";
            }
            if (dispatch && !"INFO".equals(reminderType)
                    && !curriculumReminderDispatchRepository.existsByTeacherUserIdAndWeekPlanIdAndReminderTypeAndReminderDate(teacherUserId, plan.getId(), reminderType, date)) {
                notificationService.createInApp(teacherUserId, "CURRICULUM_REMINDER", title, message);
                CurriculumReminderDispatch dispatchRecord = new CurriculumReminderDispatch();
                dispatchRecord.setTeacherUserId(teacherUserId);
                dispatchRecord.setWeekPlanId(plan.getId());
                dispatchRecord.setReminderType(reminderType);
                dispatchRecord.setReminderDate(date);
                curriculumReminderDispatchRepository.save(dispatchRecord);
            }
            return new CurriculumDtos.TeacherReminderDto(reminderType, title, message, "warning", date.atStartOfDay().atOffset(ZoneOffset.UTC), "QUEUED");
        }).toList();
        return "INFO".equals(reminderType) ? reminders.subList(0, Math.min(reminders.size(), 3)) : reminders;
    }

    private List<CurriculumDtos.TeacherReminderDto> remindersForCoverageItems(UUID teacherUserId, List<CurriculumWeekPlan> currentPlans, LocalDate date, boolean dispatch) {
        return remindersForDate(teacherUserId, currentPlans, date, dispatch);
    }

    private void ensureRiskAlert(AlertContext context) {
        CurriculumRiskAlert alert = curriculumRiskAlertRepository.findByTeacherUserIdAndWeekPlanId(context.teacherUserId, context.plan.getId()).orElseGet(() -> {
            CurriculumRiskAlert created = new CurriculumRiskAlert();
            created.setDistrictId(context.districtId);
            created.setSchoolId(context.school.getId());
            created.setTeacherUserId(context.teacherUserId);
            created.setWeekPlanId(context.plan.getId());
            created.setSubjectId(context.subjectId);
            created.setSeverity("HIGH");
            created.setStatus("OPEN");
            created.setTitle("Curriculum risk: " + context.plan.getSubject() + " " + context.plan.getTerm() + " Week " + context.plan.getWeekNumber());
            created.setDetail(context.school.getSchoolName() + " is behind ATP pacing for " + context.plan.getTopic() + ".");
            return created;
        });
        if (alert.getNotifiedAt() == null) {
            notifyRiskStakeholders(context, alert.getTitle(), alert.getDetail());
            alert.setNotifiedAt(OffsetDateTime.now());
        }
        curriculumRiskAlertRepository.save(alert);
    }

    private void notifyRiskStakeholders(AlertContext context, String title, String detail) {
        schoolUserProfileRepository.findBySchoolIdAndRoleNameAndDeletedFalse(context.school.getId(), "ROLE_SCHOOL_ADMIN")
                .forEach(profile -> notificationService.createInApp(profile.getUserId(), "CURRICULUM_RISK", title, detail));
        if (context.subjectId != null) {
            schoolSubjectRepository.findById(context.subjectId)
                    .map(SchoolSubject::getHodUserId)
                    .filter(Objects::nonNull)
                    .ifPresent(hodUserId -> notificationService.createInApp(hodUserId, "CURRICULUM_RISK", title, detail));
        }
        districtAdminProfileRepository.findByDistrictIdAndActiveTrueAndDeletedFalse(context.districtId)
                .forEach(profile -> notificationService.createInApp(profile.getUserId(), "CURRICULUM_RISK", title, detail));
    }

    private WeekMarker weekMarker(LocalDate today) {
        LocalDate startOfYear = LocalDate.of(today.getYear(), 1, 1);
        long dayOfYear = ChronoUnit.DAYS.between(startOfYear, today);
        int termNumber = Math.min(4, (today.getMonthValue() - 1) / 3 + 1);
        LocalDate termStart = LocalDate.of(today.getYear(), ((termNumber - 1) * 3) + 1, 1);
        int weekNumber = (int) Math.max(1, Math.min(10, ChronoUnit.DAYS.between(termStart, today) / 7 + 1));
        return new WeekMarker("Term " + termNumber, weekNumber, dayOfYear);
    }

    private int extractTermNumber(String value) {
        if (value == null) {
            return 1;
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return 1;
        }
        return Integer.parseInt(digits);
    }

    private LocalDate targetDate(Integer academicYear, String term, Integer weekNumber) {
        if (academicYear == null) {
            return null;
        }
        int termNumber = extractTermNumber(term);
        int month = ((termNumber - 1) * 3) + 1;
        LocalDate start = LocalDate.of(academicYear, month, 1);
        return start.plusWeeks(Math.max(0, (weekNumber == null ? 1 : weekNumber) - 1L));
    }

    private String normalizeRepositoryType(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if ("SYLLABUS_REPOSITORY".equals(normalized)) {
            return "SYLLABUS";
        }
        if ("ATP_REPOSITORY".equals(normalized)) {
            return "ATP";
        }
        if ("LESSON_PLAN_REPOSITORY".equals(normalized)) {
            return "LESSON_PLAN";
        }
        if ("TEACHING_RESOURCES".equals(normalized)) {
            return "TEACHING_RESOURCE";
        }
        return normalized;
    }

    private String normalizeGrade(String grade) {
        if (grade == null || grade.isBlank()) {
            return "";
        }
        String trimmed = grade.trim().replaceAll("\\s+", " ");
        String digits = trimmed.replaceAll("[^0-9]", "");
        return digits.isBlank() ? trimmed : "Grade " + digits;
    }

    private void ensurePublishedMappingsForSchool(UUID schoolId) {
        UUID districtId = schoolRepository.findById(schoolId).map(School::getDistrictId).orElse(null);
        if (districtId == null) {
            return;
        }
        ensurePublishedMappingsForDistrict(districtId);
    }

    private void ensurePublishedMappingsForDistrict(UUID districtId) {
        sanitizePublishedCalendarForDistrict(districtId);
        assetsForDistrict(districtId, "ATP").stream()
                .filter(this::assetVisibleToSchools)
                .filter(asset -> "PUBLISHED".equalsIgnoreCase(asset.getExtractionStatus())
                        || !atpCalendarItemRepository.findByCurriculumResourceIdAndStatusIgnoreCase(asset.getId(), "PUBLISHED").isEmpty())
                .forEach(asset -> {
                    syncPublishedAssetWeekPlans(asset);
                    createTeacherRemindersForAsset(asset);
                });
    }

    private void syncPublishedAssetWeekPlans(CurriculumAsset asset) {
        List<AtpCalendarItem> publishedItems = atpCalendarItemRepository.findByCurriculumResourceIdAndStatusIgnoreCaseOrderByTermAscWeekNumberAsc(asset.getId(), "PUBLISHED");
        for (AtpCalendarItem publishedItem : publishedItems) {
            if (isPublishableCalendarItem(publishedItem)) {
                syncPublishedItemToLegacyWeekPlan(publishedItem, asset);
            }
        }
    }

    private void createTeacherRemindersForAsset(CurriculumAsset asset) {
        List<AtpCalendarItem> publishedItems = atpCalendarItemRepository.findByCurriculumResourceIdAndStatusIgnoreCaseOrderByTermAscWeekNumberAsc(asset.getId(), "PUBLISHED");
        for (AtpCalendarItem publishedItem : publishedItems) {
            if (isPublishableCalendarItem(publishedItem)) {
                createTeacherReminders(publishedItem, asset);
            }
        }
    }

    private List<AtpCalendarItem> publishedItemsForDistrict(UUID districtId) {
        Map<UUID, CurriculumAsset> assetById = assetsForDistrict(districtId, "ATP").stream()
                .filter(this::assetVisibleToSchools)
                .collect(Collectors.toMap(CurriculumAsset::getId, item -> item));
        Set<UUID> assetIds = assetById.keySet();
        if (assetIds.isEmpty()) {
            return List.of();
        }
        return atpCalendarItemRepository.findByStatusIgnoreCaseOrderByAcademicYearDescTermAscWeekNumberAsc("PUBLISHED").stream()
                .filter(item -> assetIds.contains(item.getCurriculumResourceId()))
                .filter(this::isPublishableCalendarItem)
                .toList();
    }

    private long countPublishedItemsForDistrict(UUID schoolId) {
        UUID districtId = schoolRepository.findById(schoolId).map(School::getDistrictId).orElse(null);
        if (districtId == null) {
            return 0;
        }
        return publishedItemsForDistrict(districtId).size();
    }

    private boolean subjectMatchesPlan(String left, String right) {
        return normalize(left).equals(normalize(right));
    }

    private boolean assetVisibleToSchools(CurriculumAsset asset) {
        return asset != null
                && !asset.isArchived()
                && !asset.isDeleted()
                && asset.isActive()
                && ("ACTIVE".equalsIgnoreCase(asset.getStatus()) || "PUBLISHED".equalsIgnoreCase(asset.getExtractionStatus()))
                && (OWNER_DISTRICT.equalsIgnoreCase(asset.getOwnerScope())
                    || "DISTRICT".equalsIgnoreCase(asset.getSource())
                    || "DISTRICT_WIDE".equalsIgnoreCase(asset.getVisibility()));
    }

    private boolean isDistrictWideAsset(CurriculumAsset asset) {
        return asset != null && asset.getSchoolId() == null;
    }

    private List<CurriculumDtos.TeacherReminderDto> schoolReminders(UUID schoolId) {
        List<CurriculumDtos.TeacherReminderDto> reminders = atpTeacherReminderRepository.findBySchoolIdAndTeacherIdIsNullAndReminderDateBetweenOrderByReminderDateAsc(
                        schoolId,
                        OffsetDateTime.now().minusDays(1),
                        OffsetDateTime.now().plusDays(14))
                .stream()
                .limit(12)
                .map(this::toReminderDto)
                .toList();
        if (!reminders.isEmpty()) {
            return reminders;
        }
        return atpTeacherReminderRepository.findBySchoolIdAndTeacherIdIsNullAndStatusIgnoreCaseOrderByReminderDateAsc(schoolId, "QUEUED").stream()
                .limit(12)
                .map(this::toReminderDto)
                .toList();
    }

    private long districtWideVisibleItemCount(UUID schoolId) {
        UUID districtId = schoolRepository.findById(schoolId).map(School::getDistrictId).orElse(null);
        if (districtId == null) {
            return 0;
        }
        Set<UUID> districtWideAssetIds = assetsForDistrict(districtId, "ATP").stream()
                .filter(this::assetVisibleToSchools)
                .filter(this::isDistrictWideAsset)
                .map(CurriculumAsset::getId)
                .collect(Collectors.toSet());
        return publishedItemsForDistrict(districtId).stream()
                .filter(item -> districtWideAssetIds.contains(item.getCurriculumResourceId()))
                .count();
    }

    private String coverageKey(AtpCalendarItem item) {
        return coverageKey(item.getSubject(), item.getGrade(), item.getPhase(), item.getAcademicYear(), item.getTerm(), item.getWeekNumber());
    }

    private String coverageKey(CurriculumWeekPlan plan) {
        return coverageKey(plan.getSubject(), plan.getGrade(), plan.getCurriculumPhase(), plan.getAcademicYear(), plan.getTerm(), plan.getWeekNumber());
    }

    private String coverageKey(String subject, String grade, String phase, Integer academicYear, String term, Integer weekNumber) {
        return "%s|%s|%s|%s|%s|%s".formatted(
                normalize(subject),
                normalizeGrade(grade),
                normalize(phase),
                academicYear == null ? "" : academicYear,
                normalize(term),
                weekNumber
        );
    }

    private boolean gradeMatchesPlan(String requestedGrade, String planGrade) {
        String normalizedRequested = normalizeGrade(requestedGrade);
        String normalizedPlan = normalizeGrade(planGrade);
        if (normalizedRequested.isBlank() || normalizedPlan.isBlank()) {
            return false;
        }
        if (normalizedRequested.equalsIgnoreCase(normalizedPlan)) {
            return true;
        }
        String requestedDigits = normalizedRequested.replaceAll("[^0-9]", "");
        String planDigits = normalizedPlan.replaceAll("[^0-9]", "");
        return !requestedDigits.isBlank() && requestedDigits.equals(planDigits);
    }

    private boolean phaseMatchesPlan(String requestedPhase, String planPhase) {
        String normalizedRequested = normalize(requestedPhase);
        String normalizedPlan = normalize(planPhase);
        if (normalizedRequested.isBlank() || normalizedPlan.isBlank()) {
            return true;
        }
        return normalizedRequested.equals(normalizedPlan);
    }

    private Integer resolvePreferredAcademicYear(List<Integer>... yearSets) {
        int currentYear = LocalDate.now().getYear();
        List<Integer> values = new ArrayList<>();
        for (List<Integer> yearSet : yearSets) {
            if (yearSet == null) {
                continue;
            }
            yearSet.stream().filter(Objects::nonNull).forEach(values::add);
        }
        if (values.isEmpty()) {
            return null;
        }
        if (values.contains(currentYear)) {
            return currentYear;
        }
        return values.stream().max(Integer::compareTo).orElse(null);
    }

    private boolean matchesAcademicYear(Integer value, Integer preferredAcademicYear) {
        return preferredAcademicYear == null || value == null || Objects.equals(value, preferredAcademicYear);
    }

    private AtpCalendarItem choosePreferredItem(AtpCalendarItem left, AtpCalendarItem right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        Integer preferredAcademicYear = resolvePreferredAcademicYear(
                List.of(left.getAcademicYear()),
                List.of(right.getAcademicYear())
        );
        boolean leftMatches = matchesAcademicYear(left.getAcademicYear(), preferredAcademicYear);
        boolean rightMatches = matchesAcademicYear(right.getAcademicYear(), preferredAcademicYear);
        if (leftMatches && !rightMatches) {
            return left;
        }
        if (rightMatches && !leftMatches) {
            return right;
        }
        return right.getUpdatedAt().isAfter(left.getUpdatedAt()) ? right : left;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    private CurriculumDtos.TeacherLessonPlanResponse createLessonPlanFromCoverageItem(
            UUID schoolId,
            UUID teacherUserId,
            CurriculumDtos.TeacherCoverageItemDto coverageItem,
            AtpCalendarItem calendarItem,
            boolean regenerate
    ) {
        School school = schoolRepository.findById(schoolId).orElseThrow(() -> new ResourceConflictException("School not found."));
        User teacher = userRepository.findById(teacherUserId).orElseThrow(() -> new ResourceConflictException("Teacher not found."));
        CurriculumAsset sourceAsset = coverageItem.curriculumResourceId() == null ? null : curriculumAssetRepository.findById(coverageItem.curriculumResourceId()).orElse(null);
        String teacherMarker = "Teacher User ID: " + teacherUserId;
        String sourceMarker = "Source ATP Calendar Item ID: " + (calendarItem == null ? "WEEKPLAN-" + coverageItem.weekPlanId() : calendarItem.getId());
        CurriculumAsset existing = curriculumAssetRepository.findBySchoolIdAndRepositoryTypeIgnoreCaseAndArchivedFalseOrderBySubjectAscGradeAscUploadDateDesc(
                        schoolId,
                        "LESSON_PLAN"
                ).stream()
                .filter(asset -> subjectMatchesPlan(asset.getSubject(), coverageItem.subject()))
                .filter(asset -> gradeMatchesPlan(asset.getGrade(), coverageItem.grade()))
                .filter(asset -> Objects.equals(asset.getWeekNumber(), coverageItem.weekNumber()))
                .filter(asset -> normalize(asset.getTerm()).equals(normalize(coverageItem.term())))
                .filter(asset -> nullSafe(asset.getDescription(), "").contains(teacherMarker))
                .filter(asset -> nullSafe(asset.getDescription(), "").contains(sourceMarker))
                .findFirst()
                .orElse(null);
        LessonPlanDraft draft = buildLessonPlanDraft(coverageItem, sourceAsset);
        if (existing != null && !regenerate) {
            return toTeacherLessonPlanResponse(existing, coverageItem, draft, calendarItem, true);
        }

        CurriculumAsset lessonPlanAsset = existing == null ? new CurriculumAsset() : existing;
        lessonPlanAsset.setDistrictId(school.getDistrictId());
        lessonPlanAsset.setSchoolId(schoolId);
        lessonPlanAsset.setOwnerScope(OWNER_SCHOOL);
        lessonPlanAsset.setRepositoryType("LESSON_PLAN");
        lessonPlanAsset.setContentSource(CONTENT_SUPPLEMENTARY);
        lessonPlanAsset.setSource("SCHOOL");
        lessonPlanAsset.setVisibility("DISTRICT_WIDE");
        lessonPlanAsset.setStatus("ACTIVE");
        lessonPlanAsset.setActive(true);
        lessonPlanAsset.setArchived(false);
        lessonPlanAsset.setDeleted(false);
        lessonPlanAsset.setExtractionStatus("PUBLISHED");
        lessonPlanAsset.setExtractionError(null);
        lessonPlanAsset.setTitle(draft.title());
        lessonPlanAsset.setSubject(coverageItem.subject());
        lessonPlanAsset.setGrade(coverageItem.grade());
        lessonPlanAsset.setCurriculumPhase(coverageItem.phase());
        lessonPlanAsset.setAcademicYear(coverageItem.academicYear());
        lessonPlanAsset.setProvince(school.getProvince());
        lessonPlanAsset.setVersionNumber("ATP-GENERATED");
        lessonPlanAsset.setTerm(coverageItem.term());
        lessonPlanAsset.setWeekNumber(coverageItem.weekNumber());
        lessonPlanAsset.setUploadedByUserId(teacherUserId);
        lessonPlanAsset.setUploadDate(OffsetDateTime.now());
        lessonPlanAsset.setDescription(renderLessonPlanDescription(draft, coverageItem, school, teacher, sourceAsset, teacherMarker, sourceMarker));

        byte[] pdfBytes = buildLessonPlanPdf(draft);
        lessonPlanAsset.setPdfFileName(safeAssetFileName(draft.title(), "pdf"));
        lessonPlanAsset.setPdfContentType("application/pdf");
        lessonPlanAsset.setPdfBytes(pdfBytes);
        lessonPlanAsset.setPdfBase64(null);

        byte[] docxBytes = buildLessonPlanDocx(draft, coverageItem, sourceAsset);
        lessonPlanAsset.setDocxFileName(safeAssetFileName(draft.title(), "docx"));
        lessonPlanAsset.setDocxContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        lessonPlanAsset.setDocxBytes(docxBytes);
        lessonPlanAsset.setDocxBase64(null);

        CurriculumAsset saved = curriculumAssetRepository.save(lessonPlanAsset);
        logStoredFiles(saved);
        return toTeacherLessonPlanResponse(saved, coverageItem, draft, calendarItem, false);
    }

    private LessonPlanDraft buildLessonPlanDraft(CurriculumDtos.TeacherCoverageItemDto coverageItem, CurriculumAsset sourceAsset) {
        LessonPlanDraft fallback = buildFallbackLessonPlanDraft(coverageItem, sourceAsset);
        try {
            String prompt = """
                    Generate a weekly lesson plan from this published ATP calendar item.
                    Return ONLY strict JSON with this shape:
                    {
                      "days": [
                        {
                          "day": "Monday",
                          "topicContent": "string",
                          "objectives": "string",
                          "sourceOfMatter": "string",
                          "media": "string",
                          "lessonActivities": "string",
                          "evaluation": "string"
                        }
                      ]
                    }

                    Rules:
                    - Include Monday, Tuesday, Wednesday, Thursday, Friday in that order.
                    - Use the ATP data as the primary source.
                    - Keep the format suitable for a school lesson plan.
                    - Lesson Activities should include Step 1 through Step 5 where possible.

                    ATP data:
                    Subject: %s
                    Grade: %s
                    Phase: %s
                    Academic year: %s
                    Term: %s
                    Week: %s
                    Topic: %s
                    Sub-topic: %s
                    Learning objectives: %s
                    Suggested activities: %s
                    Assessment task: %s
                    Media/resources: %s
                    Source ATP document: %s
                    """.formatted(
                    coverageItem.subject(),
                    coverageItem.grade(),
                    nullSafe(coverageItem.phase(), ""),
                    coverageItem.academicYear() == null ? "" : coverageItem.academicYear(),
                    coverageItem.term(),
                    coverageItem.weekNumber(),
                    coverageItem.topic(),
                    nullSafe(coverageItem.subtopic(), coverageItem.topic()),
                    nullSafe(coverageItem.learningObjectives(), ""),
                    nullSafe(coverageItem.lessonFocus(), ""),
                    nullSafe(coverageItem.assessmentTask(), ""),
                    nullSafe(coverageItem.resources(), ""),
                    sourceAsset == null ? nullSafe(coverageItem.sourceTitle(), "Source ATP") : sourceAsset.getTitle()
            );
            String raw = aiProviderOrchestratorService.generateContent(prompt);
            if (raw != null && !raw.isBlank()) {
                AiLessonPlanPayload aiPayload = objectMapper.readValue(raw, AiLessonPlanPayload.class);
                LessonPlanDraft enriched = mergeAiLessonPlanDraft(fallback, aiPayload);
                if (!enriched.days().isEmpty()) {
                    return enriched;
                }
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private LessonPlanDraft mergeAiLessonPlanDraft(LessonPlanDraft fallback, AiLessonPlanPayload aiPayload) {
        if (aiPayload == null || aiPayload.days() == null || aiPayload.days().isEmpty()) {
            return fallback;
        }
        Map<String, CurriculumDtos.TeacherLessonPlanDayDto> fallbackByDay = fallback.days().stream()
                .collect(Collectors.toMap(day -> normalize(day.day()), day -> day, (left, right) -> left, LinkedHashMap::new));
        List<CurriculumDtos.TeacherLessonPlanDayDto> mergedDays = orderedWeekDays().stream()
                .map(day -> {
                    AiLessonPlanDay aiDay = aiPayload.days().stream()
                            .filter(item -> normalize(item.day()).equals(normalize(day)))
                            .findFirst()
                            .orElse(null);
                    CurriculumDtos.TeacherLessonPlanDayDto base = fallbackByDay.get(normalize(day));
                    if (base == null) {
                        return null;
                    }
                    return new CurriculumDtos.TeacherLessonPlanDayDto(
                            day,
                            firstNonBlank(aiDay == null ? null : trim(aiDay.topicContent()), base.topicContent()),
                            firstNonBlank(aiDay == null ? null : trim(aiDay.objectives()), base.objectives()),
                            firstNonBlank(aiDay == null ? null : trim(aiDay.sourceOfMatter()), base.sourceOfMatter()),
                            firstNonBlank(aiDay == null ? null : trim(aiDay.media()), base.media()),
                            firstNonBlank(aiDay == null ? null : trim(aiDay.lessonActivities()), base.lessonActivities()),
                            firstNonBlank(aiDay == null ? null : trim(aiDay.evaluation()), base.evaluation())
                    );
                })
                .filter(Objects::nonNull)
                .toList();
        return fallback.withDays(mergedDays);
    }

    private LessonPlanDraft buildFallbackLessonPlanDraft(CurriculumDtos.TeacherCoverageItemDto coverageItem, CurriculumAsset sourceAsset) {
        LocalDate weekEndingDate = resolveWeekEndingDate(coverageItem);
        String formattedWeekEnding = weekEndingDate == null ? "Not set" : "%02d/%02d/%04d".formatted(
                weekEndingDate.getDayOfMonth(),
                weekEndingDate.getMonthValue(),
                weekEndingDate.getYear()
        );
        String title = buildLessonPlanTitle(coverageItem);
        String subtopic = nullSafe(coverageItem.subtopic(), coverageItem.topic());
        String sourceOfMatter = buildSourceOfMatterLabel(coverageItem, sourceAsset);
        String media = nullSafe(coverageItem.resources(), "Charts, textbook, board work, guided worksheets, and ATP-aligned classroom media.");
        String objectivesBase = nullSafe(coverageItem.learningObjectives(), "Learners should understand the weekly ATP topic and apply it in guided and independent practice.");
        String activitiesBase = nullSafe(coverageItem.lessonFocus(), "Introduction, teacher modelling, guided practice, learner activities, and conclusion.");
        String evaluationBase = nullSafe(coverageItem.assessmentTask(), "Oral questions, class activity, and a short written task.");
        List<String> dayFocus = List.of(
                "Introduction to " + coverageItem.topic(),
                "Guided exploration of " + subtopic,
                "Application of " + coverageItem.topic(),
                "Practice and consolidation of " + subtopic,
                "Assessment and reflection on " + coverageItem.topic()
        );
        List<CurriculumDtos.TeacherLessonPlanDayDto> days = new ArrayList<>();
        List<String> labels = orderedWeekDays();
        for (int index = 0; index < labels.size(); index++) {
            String day = labels.get(index);
            String topicContent = dayFocus.get(index);
            days.add(new CurriculumDtos.TeacherLessonPlanDayDto(
                    day,
                    topicContent,
                    buildDayObjective(topicContent, objectivesBase, index),
                    sourceOfMatter,
                    media,
                    buildDayActivities(topicContent, activitiesBase, index),
                    buildDayEvaluation(evaluationBase, topicContent, index)
            ));
        }
        return new LessonPlanDraft(
                title,
                formattedWeekEnding,
                subtopic,
                sourceAsset == null ? nullSafe(coverageItem.sourceTitle(), "Source ATP") : sourceAsset.getTitle(),
                objectivesBase,
                "Introduce the ATP weekly topic, connect it to prior learning, and prepare learners for the progression across Monday to Friday.",
                activitiesBase,
                "Learners move from teacher-guided modelling to pair work, independent practice, and end-of-week consolidation.",
                media,
                evaluationBase,
                "Reinforcement task linked to " + subtopic + " and the week ending " + formattedWeekEnding + ".",
                "Provide scaffolded examples, extension work, visual supports, and extra teacher prompts where needed.",
                "Track what learners mastered during the week and what should be revisited in the next lesson cycle.",
                days
        );
    }

    private CurriculumDtos.TeacherLessonPlanResponse toTeacherLessonPlanResponse(
            CurriculumAsset asset,
            CurriculumDtos.TeacherCoverageItemDto coverageItem,
            LessonPlanDraft draft,
            AtpCalendarItem calendarItem,
            boolean alreadyExisted
    ) {
        return new CurriculumDtos.TeacherLessonPlanResponse(
                asset == null ? null : asset.getId(),
                calendarItem == null ? coverageItem.atpCalendarItemId() : calendarItem.getId(),
                alreadyExisted,
                asset != null && hasFileContent(asset.getPdfBytes(), asset.getPdfBase64()),
                asset != null && hasFileContent(asset.getDocxBytes(), asset.getDocxBase64()),
                draft.title(),
                draft.weekEnding(),
                draft.subtopic(),
                draft.sourceAtpTitle(),
                draft.learningObjectives(),
                draft.introduction(),
                draft.activities(),
                draft.learnerActivities(),
                draft.resources(),
                draft.assessment(),
                draft.homework(),
                draft.differentiation(),
                draft.reflection(),
                draft.days()
        );
    }

    private String renderLessonPlanDescription(
            LessonPlanDraft draft,
            CurriculumDtos.TeacherCoverageItemDto coverageItem,
            School school,
            User teacher,
            CurriculumAsset sourceAsset,
            String teacherMarker,
            String sourceMarker
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append(draft.title()).append("\n");
        builder.append("Week Ending: ").append(draft.weekEnding()).append("\n");
        builder.append("Sub-topic: ").append(draft.subtopic()).append("\n\n");
        builder.append("Template Columns: Day | Topic/Content | Objectives | Source of Matter | Media | Lesson Activities | Evaluation\n\n");
        for (CurriculumDtos.TeacherLessonPlanDayDto day : draft.days()) {
            builder.append(day.day()).append("\n");
            builder.append("Topic/Content: ").append(day.topicContent()).append("\n");
            builder.append("Objectives: ").append(day.objectives()).append("\n");
            builder.append("Source of Matter: ").append(day.sourceOfMatter()).append("\n");
            builder.append("Media: ").append(day.media()).append("\n");
            builder.append("Lesson Activities: ").append(day.lessonActivities()).append("\n");
            builder.append("Evaluation: ").append(day.evaluation()).append("\n\n");
        }
        builder.append("Metadata\n");
        builder.append("Teacher: ").append(fullName(teacher)).append("\n");
        builder.append(teacherMarker).append("\n");
        builder.append("School: ").append(school.getSchoolName()).append("\n");
        builder.append("Subject: ").append(coverageItem.subject()).append("\n");
        builder.append("Grade: ").append(coverageItem.grade()).append("\n");
        builder.append("Phase: ").append(nullSafe(coverageItem.phase(), "Not set")).append("\n");
        builder.append("Academic Year: ").append(coverageItem.academicYear() == null ? "Not set" : coverageItem.academicYear()).append("\n");
        builder.append("Term: ").append(coverageItem.term()).append("\n");
        builder.append("Week: ").append(coverageItem.weekNumber()).append("\n");
        builder.append(sourceMarker).append("\n");
        builder.append("Source ATP Document: ").append(sourceAsset == null ? draft.sourceAtpTitle() : sourceAsset.getTitle()).append("\n");
        return builder.toString();
    }

    private byte[] buildLessonPlanPdf(LessonPlanDraft draft) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDPageContentStream stream = new PDPageContentStream(document, page);
            float margin = 40;
            float y = page.getMediaBox().getHeight() - margin;
            y = writePdfLine(stream, bold, 16, margin, y, draft.title());
            y = writePdfLine(stream, font, 12, margin, y - 6, "Week Ending: " + draft.weekEnding());
            y = writePdfLine(stream, font, 12, margin, y - 4, "Sub-topic: " + draft.subtopic());
            y = writePdfLine(stream, bold, 12, margin, y - 10, "Day | Topic/Content | Objectives | Source of Matter | Media | Lesson Activities | Evaluation");
            for (CurriculumDtos.TeacherLessonPlanDayDto day : draft.days()) {
                for (String line : List.of(
                        day.day(),
                        "Topic/Content: " + day.topicContent(),
                        "Objectives: " + day.objectives(),
                        "Source of Matter: " + day.sourceOfMatter(),
                        "Media: " + day.media(),
                        "Lesson Activities: " + day.lessonActivities(),
                        "Evaluation: " + day.evaluation(),
                        ""
                )) {
                    for (String wrapped : wrapPdfLine(line, 92)) {
                        if (y < 70) {
                            stream.close();
                            page = new PDPage(PDRectangle.A4);
                            document.addPage(page);
                            stream = new PDPageContentStream(document, page);
                            y = page.getMediaBox().getHeight() - margin;
                        }
                        y = writePdfLine(stream, line.equals(day.day()) ? bold : font, 10, margin, y - 2, wrapped);
                    }
                }
            }
            stream.close();
            document.save(output);
            return output.toByteArray();
        } catch (Exception ex) {
            return buildFallbackLessonPlanPdf(draft);
        }
    }

    private byte[] buildFallbackLessonPlanPdf(LessonPlanDraft draft) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                float margin = 40;
                float y = page.getMediaBox().getHeight() - margin;
                y = writePdfLine(stream, bold, 16, margin, y, draft.title());
                y = writePdfLine(stream, font, 12, margin, y - 6, "Week Ending: " + draft.weekEnding());
                y = writePdfLine(stream, font, 12, margin, y - 6, "Sub-topic: " + draft.subtopic());
                y = writePdfLine(stream, font, 11, margin, y - 10, "A simplified lesson plan PDF was generated because the full export contained unsupported characters.");
            }
            document.save(output);
            return output.toByteArray();
        } catch (Exception fallbackEx) {
            return "%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF".getBytes(StandardCharsets.US_ASCII);
        }
    }

    private byte[] buildLessonPlanDocx(LessonPlanDraft draft, CurriculumDtos.TeacherCoverageItemDto coverageItem, CurriculumAsset sourceAsset) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            addZipEntry(zip, "[Content_Types].xml", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                      <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                      <Default Extension="xml" ContentType="application/xml"/>
                      <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                      <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
                      <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
                    </Types>
                    """);
            addZipEntry(zip, "_rels/.rels", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
                      <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
                      <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
                    </Relationships>
                    """);
            addZipEntry(zip, "docProps/app.xml", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties"
                                xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
                      <Application>EduRite</Application>
                    </Properties>
                    """);
            addZipEntry(zip, "docProps/core.xml", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties"
                                       xmlns:dc="http://purl.org/dc/elements/1.1/"
                                       xmlns:dcterms="http://purl.org/dc/terms/"
                                       xmlns:dcmitype="http://purl.org/dc/dcmitype/"
                                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                      <dc:title>%s</dc:title>
                      <dc:creator>EduRite</dc:creator>
                    </cp:coreProperties>
                    """.formatted(xmlEscape(draft.title())));
            StringBuilder documentXml = new StringBuilder("""
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <w:document xmlns:wpc="http://schemas.microsoft.com/office/word/2010/wordprocessingCanvas"
                                xmlns:mc="http://schemas.openxmlformats.org/markup-compatibility/2006"
                                xmlns:o="urn:schemas-microsoft-com:office:office"
                                xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
                                xmlns:m="http://schemas.openxmlformats.org/officeDocument/2006/math"
                                xmlns:v="urn:schemas-microsoft-com:vml"
                                xmlns:wp14="http://schemas.microsoft.com/office/word/2010/wordprocessingDrawing"
                                xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
                                xmlns:w10="urn:schemas-microsoft-com:office:word"
                                xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
                                xmlns:w14="http://schemas.microsoft.com/office/word/2010/wordml"
                                xmlns:wpg="http://schemas.microsoft.com/office/word/2010/wordprocessingGroup"
                                xmlns:wpi="http://schemas.microsoft.com/office/word/2010/wordprocessingInk"
                                xmlns:wne="http://schemas.microsoft.com/office/word/2006/wordml"
                                xmlns:wps="http://schemas.microsoft.com/office/word/2010/wordprocessingShape"
                                mc:Ignorable="w14 wp14">
                      <w:body>
                    """);
            appendDocxParagraph(documentXml, draft.title(), true);
            appendDocxParagraph(documentXml, "Week Ending: " + draft.weekEnding(), false);
            appendDocxParagraph(documentXml, "Sub-topic: " + draft.subtopic(), false);
            appendDocxParagraph(documentXml, "Day | Topic/Content | Objectives | Source of Matter | Media | Lesson Activities | Evaluation", true);
            for (CurriculumDtos.TeacherLessonPlanDayDto day : draft.days()) {
                appendDocxParagraph(documentXml, day.day(), true);
                appendDocxParagraph(documentXml, "Topic/Content: " + day.topicContent(), false);
                appendDocxParagraph(documentXml, "Objectives: " + day.objectives(), false);
                appendDocxParagraph(documentXml, "Source of Matter: " + day.sourceOfMatter(), false);
                appendDocxParagraph(documentXml, "Media: " + day.media(), false);
                appendDocxParagraph(documentXml, "Lesson Activities: " + day.lessonActivities(), false);
                appendDocxParagraph(documentXml, "Evaluation: " + day.evaluation(), false);
            }
            appendDocxParagraph(documentXml, "Source ATP: " + (sourceAsset == null ? draft.sourceAtpTitle() : sourceAsset.getTitle()), false);
            appendDocxParagraph(documentXml, "Subject: " + coverageItem.subject() + " | Grade: " + coverageItem.grade() + " | Term: " + coverageItem.term() + " | Week: " + coverageItem.weekNumber(), false);
            documentXml.append("<w:sectPr><w:pgSz w:w=\"11906\" w:h=\"16838\"/><w:pgMar w:top=\"1440\" w:right=\"1440\" w:bottom=\"1440\" w:left=\"1440\"/></w:sectPr></w:body></w:document>");
            addZipEntry(zip, "word/document.xml", documentXml.toString());
            zip.finish();
            return output.toByteArray();
        } catch (Exception ex) {
            return draft.title().getBytes(StandardCharsets.UTF_8);
        }
    }

    private void addZipEntry(ZipOutputStream zip, String path, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(path));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private void appendDocxParagraph(StringBuilder builder, String text, boolean bold) {
        builder.append("<w:p><w:r>");
        if (bold) {
            builder.append("<w:rPr><w:b/></w:rPr>");
        }
        builder.append("<w:t xml:space=\"preserve\">").append(xmlEscape(text)).append("</w:t></w:r></w:p>");
    }

    private String xmlEscape(String value) {
        return nullSafe(value, "")
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private List<String> orderedWeekDays() {
        return List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday");
    }

    private String buildLessonPlanTitle(CurriculumDtos.TeacherCoverageItemDto coverageItem) {
        return (coverageItem.grade() + " " + coverageItem.subject() + " Lesson Plan").toUpperCase(Locale.ROOT);
    }

    private String buildSourceOfMatterLabel(CurriculumDtos.TeacherCoverageItemDto coverageItem, CurriculumAsset sourceAsset) {
        String sourceTitle = sourceAsset == null ? nullSafe(coverageItem.sourceTitle(), "Source ATP document") : sourceAsset.getTitle();
        return sourceTitle + " / CAPS ATP / " + coverageItem.subject() + " " + coverageItem.grade();
    }

    private String buildDayObjective(String topicContent, String objectivesBase, int index) {
        return switch (index) {
            case 0 -> "Learners should recall prior knowledge and explain the focus of " + topicContent + ". " + objectivesBase;
            case 1 -> "Learners should apply guided reasoning to " + topicContent + ". " + objectivesBase;
            case 2 -> "Learners should demonstrate understanding of " + topicContent + " through classwork and discussion. " + objectivesBase;
            case 3 -> "Learners should consolidate skills for " + topicContent + " with increasing independence. " + objectivesBase;
            default -> "Learners should evaluate and communicate what they learned about " + topicContent + ". " + objectivesBase;
        };
    }

    private String buildDayActivities(String topicContent, String activitiesBase, int index) {
        return """
                Step 1: Introduction - Connect prior learning to %s.
                Step 2: Teacher Activities - Model and explain the core idea using ATP guidance.
                Step 3: Guided Practice - Work through examples with the class.
                Step 4: Learner Activities - Learners complete paired or independent practice tasks.
                Step 5: Conclusion - Review key points and prepare for the next lesson.
                ATP Guidance: %s
                """.formatted(topicContent, activitiesBase);
    }

    private String buildDayEvaluation(String evaluationBase, String topicContent, int index) {
        return switch (index) {
            case 4 -> "End-of-week assessment for " + topicContent + ". " + evaluationBase;
            default -> "Formative evaluation during " + topicContent + ": " + evaluationBase;
        };
    }

    private LocalDate resolveWeekEndingDate(CurriculumDtos.TeacherCoverageItemDto coverageItem) {
        if (coverageItem.endDate() != null) {
            return coverageItem.endDate();
        }
        LocalDate start = coverageItem.startDate();
        if (start != null) {
            return start.plusDays(4);
        }
        LocalDate target = targetDate(coverageItem.academicYear(), coverageItem.term(), coverageItem.weekNumber());
        return target == null ? null : target.plusDays(4);
    }

    private String safeAssetFileName(String title, String extension) {
        String slug = nullSafe(title, "lesson-plan")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return (slug.isBlank() ? "lesson-plan" : slug) + "." + extension;
    }

    private float writePdfLine(PDPageContentStream stream, PDType1Font font, int size, float x, float y, String text) throws Exception {
        stream.beginText();
        stream.setFont(font, size);
        stream.newLineAtOffset(x, y);
        stream.showText(sanitizePdfText(text));
        stream.endText();
        return y - (size + 4);
    }

    private String sanitizePdfText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text
                .replace('\u2018', '\'')
                .replace('\u2019', '\'')
                .replace('\u201C', '"')
                .replace('\u201D', '"')
                .replace('\u2013', '-')
                .replace('\u2014', '-')
                .replace('\u2022', '-')
                .replace('\u00A0', ' ')
                .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "")
                .chars()
                .filter(character -> character >= 32 && character <= 126)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    private List<String> wrapPdfLine(String text, int maxChars) {
        String value = nullSafe(text, "");
        if (value.length() <= maxChars) {
            return List.of(value);
        }
        List<String> lines = new ArrayList<>();
        String remaining = value;
        while (remaining.length() > maxChars) {
            int breakIndex = remaining.lastIndexOf(' ', maxChars);
            if (breakIndex <= 0) {
                breakIndex = maxChars;
            }
            lines.add(remaining.substring(0, breakIndex).trim());
            remaining = remaining.substring(breakIndex).trim();
        }
        if (!remaining.isBlank()) {
            lines.add(remaining);
        }
        return lines;
    }

    private record LessonPlanDraft(
            String title,
            String weekEnding,
            String subtopic,
            String sourceAtpTitle,
            String learningObjectives,
            String introduction,
            String activities,
            String learnerActivities,
            String resources,
            String assessment,
            String homework,
            String differentiation,
            String reflection,
            List<CurriculumDtos.TeacherLessonPlanDayDto> days
    ) {
        private LessonPlanDraft withDays(List<CurriculumDtos.TeacherLessonPlanDayDto> replacementDays) {
            return new LessonPlanDraft(
                    title,
                    weekEnding,
                    subtopic,
                    sourceAtpTitle,
                    learningObjectives,
                    introduction,
                    activities,
                    learnerActivities,
                    resources,
                    assessment,
                    homework,
                    differentiation,
                    reflection,
                    replacementDays
            );
        }
    }

    private record AiLessonPlanPayload(List<AiLessonPlanDay> days) {}

    private record AiLessonPlanDay(
            String day,
            String topicContent,
            String objectives,
            String sourceOfMatter,
            String media,
            String lessonActivities,
            String evaluation
    ) {}

    private String parseSection(String body, String label, String fallback) {
        if (body == null || body.isBlank()) {
            return fallback;
        }
        String pattern = "(?is)" + Pattern.quote(label + ":") + "\\s*(.*?)(?=\\n[A-Za-z ][A-Za-z ]+:|\\z)";
        Matcher matcher = Pattern.compile(pattern).matcher(body);
        if (!matcher.find()) {
            return fallback;
        }
        String value = matcher.group(1) == null ? "" : matcher.group(1).trim();
        return value.isBlank() ? fallback : value;
    }

    private String nullSafe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String fullName(User user) {
        String fullName = (nullSafe(user.getFirstName(), "") + " " + nullSafe(user.getLastName(), "")).trim();
        return fullName.isBlank() ? user.getEmail() : fullName;
    }

    private void triggerAtpExtraction(CurriculumAsset asset, UUID actorUserId) {
        if (!"ATP".equalsIgnoreCase(asset.getRepositoryType()) || !hasFileContent(asset.getPdfBytes(), asset.getPdfBase64())) {
            return;
        }
        try {
            aiAtpExtractionService.extractAtpCalendarFromPdf(asset.getId(), actorUserId);
        } catch (RuntimeException ignored) {
            // Extraction failures are persisted on the asset and surfaced in the review UI.
        }
    }

    private AtpCalendarItem requireDistrictCalendarItem(UUID districtId, UUID itemId) {
        AtpCalendarItem item = atpCalendarItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceConflictException("ATP calendar item not found."));
        requireDistrictAsset(districtId, item.getCurriculumResourceId());
        return item;
    }

    private CurriculumDtos.CurriculumCalendarItemDto toCalendarDto(AtpCalendarItem item, CurriculumAsset asset) {
        boolean publishable = isPublishableCalendarItem(item);
        return new CurriculumDtos.CurriculumCalendarItemDto(
                item.getId(),
                item.getCurriculumResourceId(),
                item.getSubject(),
                item.getGrade(),
                item.getPhase(),
                item.getAcademicYear(),
                asset == null ? null : asset.getProvince(),
                item.getTerm(),
                item.getWeekNumber(),
                item.getStartDate(),
                item.getEndDate(),
                item.getTopic(),
                item.getSubtopic(),
                item.getLearningObjectives(),
                item.getResources(),
                item.getAssessmentTask(),
                item.getLessonFocus(),
                item.getNotes(),
                item.getStatus(),
                asset == null ? "ATP Source" : asset.getTitle(),
                asset == null ? "PENDING" : asset.getExtractionStatus(),
                publishable,
                "PUBLISHED".equalsIgnoreCase(item.getStatus()) ? "positive" : "warning"
        );
    }

    private void validateCalendarItemForPublish(AtpCalendarItem item) {
        if (isPublishableCalendarItem(item)) {
            return;
        }
        throw new ResourceConflictException("This ATP row is incomplete or low quality. Regenerate or edit the calendar item before publishing it.");
    }

    private void sanitizePublishedCalendarForDistrict(UUID districtId) {
        assetsForDistrict(districtId, "ATP").stream()
                .filter(this::assetVisibleToSchools)
                .forEach(this::sanitizePublishedItemsForAsset);
    }

    private void sanitizePublishedItemsForAsset(CurriculumAsset asset) {
        List<AtpCalendarItem> publishedItems = atpCalendarItemRepository.findByCurriculumResourceIdAndStatusIgnoreCaseOrderByTermAscWeekNumberAsc(
                asset.getId(),
                "PUBLISHED"
        );
        for (AtpCalendarItem item : publishedItems) {
            if (isPublishableCalendarItem(item)) {
                continue;
            }
            item.setStatus("ARCHIVED");
            atpCalendarItemRepository.save(item);
            deactivateLegacyWeekPlansForItem(item, asset);
        }
    }

    private void deactivateLegacyWeekPlansForItem(AtpCalendarItem item, CurriculumAsset asset) {
        curriculumWeekPlanRepository.findByCurriculumAssetIdAndTermIgnoreCaseAndWeekNumber(
                        asset.getId(),
                        item.getTerm(),
                        item.getWeekNumber()
                ).stream()
                .filter(plan -> subjectMatchesPlan(plan.getSubject(), item.getSubject()))
                .filter(plan -> gradeMatchesPlan(plan.getGrade(), item.getGrade()))
                .filter(plan -> phaseMatchesPlan(plan.getCurriculumPhase(), item.getPhase()))
                .filter(plan -> Objects.equals(plan.getAcademicYear(), item.getAcademicYear()))
                .forEach(plan -> {
                    plan.setActive(false);
                    plan.setStatus("ARCHIVED");
                    curriculumWeekPlanRepository.save(plan);
                });
    }

    private boolean isPublishableCalendarItem(AtpCalendarItem item) {
        if (item == null) {
            return false;
        }
        String topic = trim(item.getTopic());
        if (topic == null || topic.isBlank() || looksLikeInvalidCalendarTopic(topic)) {
            return false;
        }
        return hasStructuredCalendarSupport(item);
    }

    private boolean hasStructuredCalendarSupport(AtpCalendarItem item) {
        return hasRealContent(item.getSubtopic())
                || hasRealContent(item.getLearningObjectives())
                || hasRealContent(item.getLessonFocus())
                || hasRealContent(item.getAssessmentTask())
                || hasRealContent(item.getResources());
    }

    private boolean hasRealContent(String value) {
        String trimmed = trim(value);
        if (trimmed == null || trimmed.isBlank()) {
            return false;
        }
        String normalized = normalize(trimmed);
        return !normalized.equals("not set")
                && !normalized.equals("pending")
                && !normalized.equals("subtopic not set")
                && !normalized.equals("assessment task not set")
                && !normalized.equals("date pending");
    }

    private boolean looksLikeInvalidCalendarTopic(String topic) {
        String normalized = normalize(topic);
        if (normalized.isBlank()) {
            return true;
        }
        if (normalized.matches("^(week\\s*\\d+\\s*){2,}$")) {
            return true;
        }
        Matcher matcher = Pattern.compile("(?i)\\bweek\\b").matcher(topic);
        int count = 0;
        while (matcher.find()) {
            count++;
            if (count > 1) {
                return true;
            }
        }
        return false;
    }

    private boolean isUsableCoverageItem(CurriculumDtos.TeacherCoverageItemDto item) {
        if (item == null) {
            return false;
        }
        String topic = trim(item.topic());
        if (topic == null || topic.isBlank() || looksLikeInvalidCalendarTopic(topic)) {
            return false;
        }
        return hasRealContent(item.subtopic())
                || hasRealContent(item.learningObjectives())
                || hasRealContent(item.lessonFocus())
                || hasRealContent(item.assessmentTask())
                || hasRealContent(item.resources());
    }

    private void syncPublishedItemToLegacyWeekPlan(AtpCalendarItem item, CurriculumAsset asset) {
        List<CurriculumWeekPlan> sameWeekPlans = curriculumWeekPlanRepository.findByCurriculumAssetIdAndTermIgnoreCaseAndWeekNumber(
                asset.getId(),
                item.getTerm(),
                item.getWeekNumber()
        );
        CurriculumWeekPlan weekPlan = sameWeekPlans.stream()
                .filter(plan -> subjectMatchesPlan(plan.getSubject(), item.getSubject())
                        && gradeMatchesPlan(plan.getGrade(), item.getGrade())
                        && phaseMatchesPlan(plan.getCurriculumPhase(), item.getPhase())
                        && Objects.equals(plan.getAcademicYear(), item.getAcademicYear()))
                .findFirst()
                .orElseGet(CurriculumWeekPlan::new);
        weekPlan.setCurriculumAssetId(asset.getId());
        weekPlan.setDistrictId(asset.getDistrictId());
        weekPlan.setSchoolId(asset.getSchoolId());
        weekPlan.setSubject(item.getSubject());
        weekPlan.setGrade(item.getGrade());
        weekPlan.setCurriculumPhase(item.getPhase());
        weekPlan.setAcademicYear(item.getAcademicYear());
        weekPlan.setProvince(asset.getProvince());
        weekPlan.setTerm(item.getTerm());
        weekPlan.setWeekNumber(item.getWeekNumber());
        weekPlan.setStartDate(item.getStartDate());
        weekPlan.setEndDate(item.getEndDate());
        weekPlan.setTopic(item.getTopic());
        weekPlan.setSubtopic(item.getSubtopic());
        weekPlan.setLearningOutcomes(item.getLearningObjectives());
        weekPlan.setAssessmentActivities(item.getAssessmentTask());
        weekPlan.setResourcesMaterials(item.getResources());
        weekPlan.setLessonFocus(item.getLessonFocus());
        weekPlan.setNotes(item.getNotes());
        weekPlan.setStatus("PUBLISHED");
        weekPlan.setExpectedCompletionLabel(item.getTerm() + " Week " + item.getWeekNumber());
        weekPlan.setActive(true);
        curriculumWeekPlanRepository.save(weekPlan);
        sameWeekPlans.stream()
                .filter(plan -> !plan.getId().equals(weekPlan.getId()))
                .filter(plan -> subjectMatchesPlan(plan.getSubject(), item.getSubject())
                        && gradeMatchesPlan(plan.getGrade(), item.getGrade())
                        && phaseMatchesPlan(plan.getCurriculumPhase(), item.getPhase())
                        && Objects.equals(plan.getAcademicYear(), item.getAcademicYear()))
                .forEach(plan -> {
                    plan.setActive(false);
                    plan.setStatus("ARCHIVED");
                    curriculumWeekPlanRepository.save(plan);
                });
    }

    private void createTeacherReminders(AtpCalendarItem item, CurriculumAsset asset) {
        LocalDate startDate = firstNonNull(item.getStartDate(), targetDate(item.getAcademicYear(), item.getTerm(), item.getWeekNumber()));
        if (startDate == null) {
            startDate = LocalDate.now();
        }
        LocalDate endDate = firstNonNull(item.getEndDate(), startDate.plusDays(4));
        for (School school : schoolRepository.findByDistrictIdOrderBySchoolNameAsc(asset.getDistrictId())) {
            upsertSchoolReminder(item, school.getId(), "WEEK_START", atDate(startDate, 6, 0),
                    item.getGrade() + " " + item.getSubject() + " Week " + item.getWeekNumber() + " ATP: " + item.getTopic() + " should be covered this week.");
            upsertSchoolReminder(item, school.getId(), "MID_WEEK_CHECK", atDate(startDate.plusDays(2), 12, 0),
                    item.getSubject() + " " + item.getTerm() + " Week " + item.getWeekNumber() + " progress check.");
            upsertSchoolReminder(item, school.getId(), "WEEK_END_COMPLETION", atDate(endDate, 14, 0),
                    "Mark completion for " + item.getSubject() + " Week " + item.getWeekNumber() + ": " + item.getTopic() + ".");
            for (TeacherAssignment assignment : teacherAssignmentRepository.findBySchoolIdAndActiveTrue(school.getId())) {
                SchoolSubject subject = schoolSubjectRepository.findById(assignment.getSubjectId()).orElse(null);
                if (subject == null) {
                    continue;
                }
                if (!normalize(item.getSubject()).equals(normalize(subject.getSubjectName()))
                        || !normalizeGrade(item.getGrade()).equals(normalizeGrade(firstNonBlank(assignment.getGrade(), firstNonBlank(subject.getGrade(), subject.getGradeRange()))))
                        || !phaseMatchesPlan(firstNonBlank(assignment.getPhase(), subject.getPhase()), item.getPhase())) {
                    continue;
                }
                upsertReminder(item, school.getId(), assignment.getTeacherUserId(), "WEEK_START", atDate(startDate, 6, 0),
                        item.getGrade() + " " + item.getSubject() + " Week " + item.getWeekNumber() + " ATP: " + item.getTopic() + " should be covered this week. Please update your progress by Friday.");
                upsertReminder(item, school.getId(), assignment.getTeacherUserId(), "MID_WEEK_CHECK", atDate(startDate.plusDays(2), 12, 0),
                        item.getSubject() + " " + item.getTerm() + " Week " + item.getWeekNumber() + " mid-week progress check.");
                upsertReminder(item, school.getId(), assignment.getTeacherUserId(), "WEEK_END_COMPLETION", atDate(endDate, 14, 0),
                        "Mark completion for " + item.getSubject() + " Week " + item.getWeekNumber() + ": " + item.getTopic() + ".");
            }
        }
    }

    private void upsertSchoolReminder(AtpCalendarItem item, UUID schoolId, String reminderType, OffsetDateTime reminderDate, String message) {
        AtpTeacherReminder reminder = atpTeacherReminderRepository.findByAtpCalendarItemIdAndSchoolIdAndTeacherIdIsNullAndReminderType(item.getId(), schoolId, reminderType)
                .orElseGet(AtpTeacherReminder::new);
        reminder.setAtpCalendarItemId(item.getId());
        reminder.setSchoolId(schoolId);
        reminder.setTeacherId(null);
        reminder.setSubject(item.getSubject());
        reminder.setGrade(item.getGrade());
        reminder.setReminderType(reminderType);
        reminder.setReminderDate(reminderDate);
        reminder.setReminderMessage(message);
        reminder.setStatus("QUEUED");
        atpTeacherReminderRepository.save(reminder);
    }

    private void upsertReminder(AtpCalendarItem item, UUID schoolId, UUID teacherId, String reminderType, OffsetDateTime reminderDate, String message) {
        AtpTeacherReminder reminder = atpTeacherReminderRepository.findByAtpCalendarItemIdAndSchoolIdAndTeacherIdAndReminderType(item.getId(), schoolId, teacherId, reminderType)
                .orElseGet(AtpTeacherReminder::new);
        reminder.setAtpCalendarItemId(item.getId());
        reminder.setSchoolId(schoolId);
        reminder.setTeacherId(teacherId);
        reminder.setSubject(item.getSubject());
        reminder.setGrade(item.getGrade());
        reminder.setReminderType(reminderType);
        reminder.setReminderDate(reminderDate);
        reminder.setReminderMessage(message);
        reminder.setStatus("QUEUED");
        atpTeacherReminderRepository.save(reminder);
    }

    private CurriculumDtos.TeacherReminderDto toReminderDto(AtpTeacherReminder reminder) {
        return new CurriculumDtos.TeacherReminderDto(
                reminder.getReminderType(),
                reminder.getSubject() + " " + reminder.getGrade(),
                reminder.getReminderMessage(),
                "OVERDUE_ATP".equalsIgnoreCase(reminder.getReminderType()) || "INTERVENTION_ALERT".equalsIgnoreCase(reminder.getReminderType()) ? "critical" : "warning",
                reminder.getReminderDate(),
                reminder.getStatus()
        );
    }

    private OffsetDateTime atDate(LocalDate date, int hour, int minute) {
        return OffsetDateTime.of(LocalDateTime.of(date, LocalTime.of(hour, minute)), ZoneOffset.UTC);
    }

    private String resolveAssetSource(String ownerScope, UUID actorUserId) {
        if (!OWNER_DISTRICT.equalsIgnoreCase(ownerScope)) {
            return CurriculumResourceService.SOURCE_SCHOOL;
        }
        if (actorUserId == null) {
            return CurriculumResourceService.SOURCE_DISTRICT;
        }
        return userRepository.findById(actorUserId)
                .filter(user -> user.getRoles().stream().anyMatch(role -> ROLE_SUBJECT_ADVISOR.equalsIgnoreCase(role.getName())))
                .map(user -> CurriculumResourceService.SOURCE_SUBJECT_ADVISOR)
                .orElse(CurriculumResourceService.SOURCE_DISTRICT);
    }

    private record GeneratedWeek(
            String term,
            int weekNumber,
            String topic,
            String subtopic,
            String learningOutcomes,
            String assessmentActivities
    ) {}

    private record WeekMarker(String term, int weekNumber, long dayOfYear) {}

    private record TeacherWeekContext(
            CurriculumDtos.TeacherCoverageItemDto currentTopic,
            List<CurriculumWeekPlan> currentPlans,
            List<CurriculumDtos.TeacherCoverageItemDto> behindTopics,
            List<CurriculumDtos.TeacherCoverageItemDto> upcomingTopics,
            Map<UUID, CurriculumWeekPlan> planById,
            Map<UUID, UUID> subjectIdByWeekPlan,
            Map<UUID, AtpCalendarItem> atpItemByWeekPlanId
    ) {}

    private record ComplianceSnapshot(
            int districtCompliancePercent,
            long onTrackSchools,
            long behindSchools,
            long aheadSchools,
            List<CurriculumDtos.CurriculumComplianceSchoolDto> schoolRows,
            List<CurriculumDtos.CurriculumHeatMapItemDto> heatMap,
            List<DistrictDtos.InsightItemDto> subjectsBehind,
            List<DistrictDtos.InsightItemDto> teachersBehind,
            List<CurriculumDtos.CurriculumRiskAlertDto> riskAlerts,
            List<AlertContext> openAlertContexts
    ) {}

    private record AlertContext(
            UUID districtId,
            School school,
            UUID teacherUserId,
            CurriculumWeekPlan plan,
            UUID subjectId,
            User teacher
    ) {}
}
