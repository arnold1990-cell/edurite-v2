
package com.edurite.curriculum.service;

import com.edurite.ai.service.AiProviderOrchestratorService;
import com.edurite.common.exception.ResourceConflictException;
import com.edurite.curriculum.dto.CurriculumDtos;
import com.edurite.curriculum.entity.AtpCalendarItem;
import com.edurite.curriculum.entity.CurriculumAsset;
import com.edurite.curriculum.entity.CurriculumWeekPlan;
import com.edurite.curriculum.repository.AtpCalendarItemRepository;
import com.edurite.curriculum.repository.CurriculumAssetRepository;
import com.edurite.curriculum.repository.CurriculumWeekPlanRepository;
import com.edurite.school.portal.entity.School;
import com.edurite.school.portal.entity.SchoolClass;
import com.edurite.school.portal.entity.TeacherAssignment;
import com.edurite.school.portal.repository.SchoolClassRepository;
import com.edurite.school.portal.repository.SchoolRepository;
import com.edurite.school.portal.repository.SchoolSubjectRepository;
import com.edurite.school.portal.repository.TeacherAssignmentRepository;
import com.edurite.user.entity.User;
import com.edurite.user.repository.UserRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurriculumLessonPlanService {
    private static final Pattern XML_TEXT_PATTERN = Pattern.compile("<w:t[^>]*>(.*?)</w:t>", Pattern.DOTALL);
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CurriculumAssetRepository curriculumAssetRepository;
    private final CurriculumWeekPlanRepository curriculumWeekPlanRepository;
    private final AtpCalendarItemRepository atpCalendarItemRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolSubjectRepository schoolSubjectRepository;
    private final TeacherAssignmentRepository teacherAssignmentRepository;
    private final UserRepository userRepository;
    private final AiProviderOrchestratorService aiProviderOrchestratorService;
    private final Environment environment;
    private final ObjectMapper objectMapper;

    public CurriculumLessonPlanService(
            CurriculumAssetRepository curriculumAssetRepository,
            CurriculumWeekPlanRepository curriculumWeekPlanRepository,
            AtpCalendarItemRepository atpCalendarItemRepository,
            SchoolRepository schoolRepository,
            SchoolClassRepository schoolClassRepository,
            SchoolSubjectRepository schoolSubjectRepository,
            TeacherAssignmentRepository teacherAssignmentRepository,
            UserRepository userRepository,
            AiProviderOrchestratorService aiProviderOrchestratorService,
            Environment environment,
            ObjectMapper objectMapper
    ) {
        this.curriculumAssetRepository = curriculumAssetRepository;
        this.curriculumWeekPlanRepository = curriculumWeekPlanRepository;
        this.atpCalendarItemRepository = atpCalendarItemRepository;
        this.schoolRepository = schoolRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.schoolSubjectRepository = schoolSubjectRepository;
        this.teacherAssignmentRepository = teacherAssignmentRepository;
        this.userRepository = userRepository;
        this.aiProviderOrchestratorService = aiProviderOrchestratorService;
        this.environment = environment;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CurriculumDtos.TeacherLessonPlanResponse generateForTeacher(UUID schoolId, UUID teacherUserId, CurriculumDtos.LessonPlanGenerationRequest request) {
        ResolvedGenerationContext context = resolveTeacherContext(schoolId, teacherUserId, request);
        return generate(context, Boolean.TRUE.equals(request.regenerate()));
    }

    @Transactional
    public CurriculumDtos.TeacherLessonPlanResponse generateForSchoolAdmin(UUID schoolId, UUID actorUserId, CurriculumDtos.LessonPlanGenerationRequest request) {
        ResolvedGenerationContext context = resolveSchoolAdminContext(schoolId, actorUserId, request);
        return generate(context, Boolean.TRUE.equals(request.regenerate()));
    }

    @Transactional
    public CurriculumDtos.TeacherLessonPlanResponse saveDraftForTeacher(UUID schoolId, UUID teacherUserId, UUID assetId, CurriculumDtos.LessonPlanDraftSaveRequest request) {
        CurriculumAsset asset = requireLessonPlanAsset(assetId, schoolId);
        UUID ownerTeacherId = firstNonNull(asset.getTeacherUserId(), asset.getUploadedByUserId());
        if (ownerTeacherId != null && !Objects.equals(ownerTeacherId, teacherUserId)) {
            throw new ResourceConflictException("Only the assigned teacher can edit this lesson plan.");
        }
        return saveDraft(asset, request, "DRAFT");
    }

    @Transactional
    public CurriculumDtos.TeacherLessonPlanResponse saveDraftForSchoolAdmin(UUID schoolId, UUID assetId, CurriculumDtos.LessonPlanDraftSaveRequest request) {
        return saveDraft(requireLessonPlanAsset(assetId, schoolId), request, "DRAFT");
    }

    @Transactional
    public CurriculumDtos.TeacherLessonPlanResponse publishForTeacher(UUID schoolId, UUID teacherUserId, UUID assetId, CurriculumDtos.LessonPlanDraftSaveRequest request) {
        CurriculumAsset asset = requireLessonPlanAsset(assetId, schoolId);
        UUID ownerTeacherId = firstNonNull(asset.getTeacherUserId(), asset.getUploadedByUserId());
        if (ownerTeacherId != null && !Objects.equals(ownerTeacherId, teacherUserId)) {
            throw new ResourceConflictException("Only the assigned teacher can publish this lesson plan.");
        }
        return saveDraft(asset, request, "PUBLISHED");
    }

    @Transactional
    public CurriculumDtos.TeacherLessonPlanResponse publishForSchoolAdmin(UUID schoolId, UUID assetId, CurriculumDtos.LessonPlanDraftSaveRequest request) {
        return saveDraft(requireLessonPlanAsset(assetId, schoolId), request, "PUBLISHED");
    }

    @Transactional(readOnly = true)
    public CurriculumDtos.TeacherLessonPlanResponse loadLessonPlan(UUID schoolId, UUID assetId) {
        CurriculumAsset asset = requireLessonPlanAsset(assetId, schoolId);
        return toResponse(asset, loadPayload(asset), false);
    }

    @Transactional
    public CurriculumDtos.TeacherLessonPlanResponse generateFromWeekPlan(UUID schoolId, UUID teacherUserId, UUID weekPlanId) {
        CurriculumWeekPlan weekPlan = curriculumWeekPlanRepository.findByIdAndActiveTrue(weekPlanId)
                .orElseThrow(() -> new ResourceConflictException("Curriculum week is not available."));
        AtpCalendarItem atpItem = atpCalendarItemRepository.findByCurriculumResourceIdAndStatusIgnoreCaseOrderByTermAscWeekNumberAsc(weekPlan.getCurriculumAssetId(), "PUBLISHED")
                .stream()
                .filter(item -> normalize(item.getSubject()).equals(normalize(weekPlan.getSubject())))
                .filter(item -> normalizeGrade(item.getGrade()).equals(normalizeGrade(weekPlan.getGrade())))
                .filter(item -> normalize(item.getTerm()).equals(normalize(weekPlan.getTerm())))
                .filter(item -> Objects.equals(item.getWeekNumber(), weekPlan.getWeekNumber()))
                .findFirst()
                .orElse(null);
        return generateForTeacher(schoolId, teacherUserId, new CurriculumDtos.LessonPlanGenerationRequest(
                atpItem == null ? null : atpItem.getId(),
                weekPlan.getCurriculumAssetId(),
                weekPlan.getSubject(),
                weekPlan.getGrade(),
                weekPlan.getCurriculumPhase(),
                weekPlan.getAcademicYear(),
                weekPlan.getTerm(),
                weekPlan.getWeekNumber(),
                weekPlan.getTopic(),
                firstNonNull(weekPlan.getStartDate(), LocalDate.now()),
                60,
                null,
                teacherUserId,
                "English",
                weekPlan.getResourcesMaterials(),
                weekPlan.getLessonFocus(),
                false
        ));
    }
    private CurriculumDtos.TeacherLessonPlanResponse generate(ResolvedGenerationContext context, boolean regenerate) {
        Optional<CurriculumAsset> existing = curriculumAssetRepository.findByGenerationRequestKey(context.generationRequestKey())
                .filter(asset -> Objects.equals(asset.getSchoolId(), context.school().getId()))
                .filter(asset -> !asset.isDeleted() && !asset.isArchived());
        if (existing.isPresent() && !regenerate) {
            return toResponse(existing.get(), loadPayload(existing.get()), true);
        }
        LessonPlanPayload payload = buildPayload(context);
        CurriculumAsset asset = existing.orElseGet(CurriculumAsset::new);
        applyGeneratedLessonPlan(asset, context, payload, existing.map(CurriculumAsset::getLessonPlanStatus).filter(value -> !value.isBlank()).orElse("DRAFT"));
        return toResponse(curriculumAssetRepository.save(asset), payload, false);
    }

    private CurriculumDtos.TeacherLessonPlanResponse saveDraft(CurriculumAsset asset, CurriculumDtos.LessonPlanDraftSaveRequest request, String status) {
        LessonPlanPayload payload = normalizeEditablePayload(request, asset);
        asset.setTitle(payload.title());
        asset.setSubject(payload.subject());
        asset.setGrade(payload.grade());
        asset.setCurriculumPhase(payload.phase());
        asset.setAcademicYear(payload.academicYear());
        asset.setTerm(payload.term());
        asset.setWeekNumber(payload.weekNumber());
        asset.setLessonTopic(payload.topic());
        asset.setLessonDate(payload.lessonDate());
        asset.setLessonDurationMinutes(payload.lessonDurationMinutes());
        asset.setLanguage(payload.language());
        asset.setLessonPlanStatus(status);
        asset.setDescription(buildAssetDescription(payload, asset));
        asset.setLessonPlanPayloadJson(writeJson(payload));
        if ("PUBLISHED".equalsIgnoreCase(status)) {
            asset.setPublishedAt(OffsetDateTime.now());
        }
        updateFiles(asset, payload);
        return toResponse(curriculumAssetRepository.save(asset), payload, false);
    }

    private ResolvedGenerationContext resolveTeacherContext(UUID schoolId, UUID teacherUserId, CurriculumDtos.LessonPlanGenerationRequest request) {
        School school = requireSchool(schoolId);
        User teacher = requireUser(teacherUserId, "Teacher not found.");
        CurriculumAsset sourceAsset = resolveVisibleSourceAsset(school, request.sourceCurriculumAssetId());
        AtpCalendarItem atpItem = resolveCalendarItem(sourceAsset, request.sourceAtpCalendarItemId());
        String subject = firstNonBlank(request.subject(), firstNonBlank(atpItem == null ? null : atpItem.getSubject(), sourceAsset.getSubject()));
        String grade = firstNonBlank(request.grade(), firstNonBlank(atpItem == null ? null : atpItem.getGrade(), sourceAsset.getGrade()));
        String phase = firstNonBlank(request.phase(), firstNonBlank(atpItem == null ? null : atpItem.getPhase(), sourceAsset.getCurriculumPhase()));
        TeacherAssignment assignment = teacherAssignmentRepository.findBySchoolIdAndTeacherUserIdAndActiveTrue(schoolId, teacherUserId).stream()
                .filter(item -> request.classId() == null || Objects.equals(item.getClassId(), request.classId()))
                .filter(item -> subjectMatches(item.getSubjectId(), schoolId, subject))
                .filter(item -> gradeMatches(item.getGrade(), grade))
                .filter(item -> phaseMatches(item.getPhase(), phase))
                .findFirst()
                .orElseThrow(() -> new ResourceConflictException("Teachers can only generate lesson plans inside their teaching assignments."));
        SchoolClass schoolClass = schoolClassRepository.findById(assignment.getClassId()).orElse(null);
        return buildResolvedContext(school, teacher, teacher, schoolClass, sourceAsset, atpItem, request, subject, grade, phase, assignment.getTeacherUserId());
    }

    private ResolvedGenerationContext resolveSchoolAdminContext(UUID schoolId, UUID actorUserId, CurriculumDtos.LessonPlanGenerationRequest request) {
        School school = requireSchool(schoolId);
        User actor = requireUser(actorUserId, "School admin not found.");
        CurriculumAsset sourceAsset = resolveVisibleSourceAsset(school, request.sourceCurriculumAssetId());
        AtpCalendarItem atpItem = resolveCalendarItem(sourceAsset, request.sourceAtpCalendarItemId());
        String subject = firstNonBlank(request.subject(), firstNonBlank(atpItem == null ? null : atpItem.getSubject(), sourceAsset.getSubject()));
        String grade = firstNonBlank(request.grade(), firstNonBlank(atpItem == null ? null : atpItem.getGrade(), sourceAsset.getGrade()));
        String phase = firstNonBlank(request.phase(), firstNonBlank(atpItem == null ? null : atpItem.getPhase(), sourceAsset.getCurriculumPhase()));
        ensureSchoolOffersSubject(schoolId, subject, grade, phase);
        SchoolClass schoolClass = request.classId() == null ? null : schoolClassRepository.findById(request.classId())
                .filter(item -> Objects.equals(item.getSchoolId(), schoolId))
                .orElseThrow(() -> new ResourceConflictException("Selected class does not belong to this school."));
        User selectedTeacher = request.teacherUserId() == null ? actor : requireUser(request.teacherUserId(), "Selected teacher not found.");
        if (request.teacherUserId() != null) {
            teacherAssignmentRepository.findBySchoolIdAndTeacherUserIdAndActiveTrue(schoolId, request.teacherUserId()).stream()
                    .filter(item -> schoolClass == null || Objects.equals(item.getClassId(), schoolClass.getId()))
                    .filter(item -> subjectMatches(item.getSubjectId(), schoolId, subject))
                    .filter(item -> gradeMatches(item.getGrade(), grade))
                    .filter(item -> phaseMatches(item.getPhase(), phase))
                    .findFirst()
                    .orElseThrow(() -> new ResourceConflictException("Selected teacher is not assigned to this subject, class, or grade."));
        }
        return buildResolvedContext(school, actor, selectedTeacher, schoolClass, sourceAsset, atpItem, request, subject, grade, phase, request.teacherUserId());
    }

    private ResolvedGenerationContext buildResolvedContext(School school, User actor, User teacher, SchoolClass schoolClass, CurriculumAsset sourceAsset, AtpCalendarItem atpItem, CurriculumDtos.LessonPlanGenerationRequest request, String subject, String grade, String phase, UUID teacherUserId) {
        Integer academicYear = firstNonNull(request.academicYear(), firstNonNull(atpItem == null ? null : atpItem.getAcademicYear(), sourceAsset.getAcademicYear()));
        String term = normalizeTerm(firstNonBlank(request.term(), firstNonBlank(atpItem == null ? null : atpItem.getTerm(), sourceAsset.getTerm())));
        Integer weekNumber = firstNonNull(request.weekNumber(), firstNonNull(atpItem == null ? null : atpItem.getWeekNumber(), sourceAsset.getWeekNumber()));
        String topic = firstNonBlank(request.topic(), firstNonBlank(atpItem == null ? null : atpItem.getTopic(), sourceAsset.getLessonTopic()));
        LocalDate lessonDate = firstNonNull(request.lessonDate(), firstNonNull(atpItem == null ? null : atpItem.getStartDate(), LocalDate.now()));
        int duration = request.lessonDurationMinutes() == null || request.lessonDurationMinutes() <= 0 ? 60 : request.lessonDurationMinutes();
        return new ResolvedGenerationContext(
                school,
                actor,
                teacher,
                schoolClass,
                sourceAsset,
                atpItem,
                subject,
                normalizeGrade(grade),
                firstNonBlank(phase, sourceAsset.getCurriculumPhase()),
                academicYear,
                term,
                weekNumber == null ? 1 : weekNumber,
                firstNonBlank(topic, sourceAsset.getTitle()),
                lessonDate,
                duration,
                firstNonBlank(request.language(), "English"),
                firstNonBlank(request.availableResources(), sourceAsset.getDescription()),
                trim(request.additionalInstructions()),
                teacherUserId,
                generationRequestKey(school.getId(), teacherUserId, schoolClass == null ? null : schoolClass.getId(), sourceAsset.getId(), atpItem == null ? null : atpItem.getId(), term, weekNumber == null ? 1 : weekNumber, topic, lessonDate, firstNonBlank(request.language(), "English"))
        );
    }
    private CurriculumAsset resolveVisibleSourceAsset(School school, UUID sourceCurriculumAssetId) {
        if (sourceCurriculumAssetId == null) {
            throw new ResourceConflictException("Select an ATP source before generating a lesson plan.");
        }
        CurriculumAsset asset = curriculumAssetRepository.findById(sourceCurriculumAssetId)
                .orElseThrow(() -> new ResourceConflictException("Selected ATP resource was not found."));
        if (!isVisibleToSchool(asset, school)) {
            throw new ResourceConflictException("Selected ATP resource is not visible to this school.");
        }
        if (!"ATP".equalsIgnoreCase(asset.getRepositoryType())) {
            throw new ResourceConflictException("Lesson plans can only be generated from ATP resources.");
        }
        return asset;
    }

    private AtpCalendarItem resolveCalendarItem(CurriculumAsset sourceAsset, UUID sourceAtpCalendarItemId) {
        if (sourceAtpCalendarItemId == null) {
            return atpCalendarItemRepository.findByCurriculumResourceIdAndStatusIgnoreCaseOrderByTermAscWeekNumberAsc(sourceAsset.getId(), "PUBLISHED")
                    .stream().findFirst().orElse(null);
        }
        return atpCalendarItemRepository.findByIdAndStatusIgnoreCase(sourceAtpCalendarItemId, "PUBLISHED")
                .filter(item -> Objects.equals(item.getCurriculumResourceId(), sourceAsset.getId()))
                .orElseThrow(() -> new ResourceConflictException("Selected ATP topic is not published or does not belong to the chosen ATP."));
    }

    private void ensureSchoolOffersSubject(UUID schoolId, String subject, String grade, String phase) {
        boolean offered = schoolSubjectRepository.findBySchoolIdAndActiveTrue(schoolId).stream()
                .anyMatch(item -> normalize(item.getSubjectName()).equals(normalize(subject))
                        && gradeMatches(firstNonBlank(item.getGrade(), item.getGradeRange()), grade)
                        && phaseMatches(item.getPhase(), phase));
        if (!offered) {
            throw new ResourceConflictException("School admins can only generate lesson plans for subjects and grades offered by their school.");
        }
    }

    private LessonPlanPayload buildPayload(ResolvedGenerationContext context) {
        String atpText = extractDocumentText(context.sourceAsset());
        String syllabusText = curriculumAssetRepository.findByDistrictIdAndRepositoryTypeIgnoreCaseAndArchivedFalseOrderBySubjectAscGradeAscUploadDateDesc(context.school().getDistrictId(), "SYLLABUS")
                .stream()
                .filter(asset -> isVisibleToSchool(asset, context.school()))
                .filter(asset -> normalize(asset.getSubject()).equals(normalize(context.subject())))
                .filter(asset -> gradeMatches(asset.getGrade(), context.grade()))
                .filter(asset -> phaseMatches(asset.getCurriculumPhase(), context.phase()))
                .limit(2)
                .map(this::extractDocumentText)
                .filter(text -> !text.isBlank())
                .collect(Collectors.joining("\n\n---\n\n"));
        LessonPlanPayload fallback = buildFallbackPayload(context);
        try {
            String raw = aiProviderOrchestratorService.generateContent(buildPrompt(context, atpText, syllabusText));
            if (raw != null && !raw.isBlank()) {
                AiLessonPlanPayload aiPayload = objectMapper.readValue(raw, AiLessonPlanPayload.class);
                return mergePayload(fallback, aiPayload, context);
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private void applyGeneratedLessonPlan(CurriculumAsset asset, ResolvedGenerationContext context, LessonPlanPayload payload, String lessonPlanStatus) {
        asset.setDistrictId(context.school().getDistrictId());
        asset.setSchoolId(context.school().getId());
        asset.setTeacherUserId(context.teacherUserId());
        asset.setClassId(context.schoolClass() == null ? null : context.schoolClass().getId());
        asset.setSourceCurriculumAssetId(context.sourceAsset().getId());
        asset.setSourceAtpCalendarItemId(context.atpItem() == null ? null : context.atpItem().getId());
        asset.setOwnerScope("SCHOOL");
        asset.setRepositoryType("LESSON_PLAN");
        asset.setContentSource("SUPPLEMENTARY");
        asset.setSource("SCHOOL");
        asset.setVisibility(CurriculumResourceService.VISIBILITY_SCHOOL_ONLY);
        asset.setStatus(CurriculumResourceService.STATUS_ACTIVE);
        asset.setLessonPlanStatus(lessonPlanStatus == null || lessonPlanStatus.isBlank() ? "DRAFT" : lessonPlanStatus);
        asset.setTitle(payload.title());
        asset.setSubject(payload.subject());
        asset.setGrade(payload.grade());
        asset.setCurriculumPhase(payload.phase());
        asset.setAcademicYear(payload.academicYear());
        asset.setProvince(context.school().getProvince());
        asset.setVersionNumber(payload.generatedByAi() ? "AI-GENERATED" : "MANUAL-DRAFT");
        asset.setDescription(buildAssetDescription(payload, asset));
        asset.setTerm(payload.term());
        asset.setWeekNumber(payload.weekNumber());
        asset.setLessonTopic(payload.topic());
        asset.setLessonDate(payload.lessonDate());
        asset.setLessonDurationMinutes(payload.lessonDurationMinutes());
        asset.setLanguage(payload.language());
        asset.setGenerationRequestKey(context.generationRequestKey());
        asset.setAiProvider(resolvePrimaryProvider());
        asset.setAiModel(resolveModelName());
        asset.setAiGeneratedAt(OffsetDateTime.now());
        asset.setGeneratedByAi(payload.generatedByAi());
        asset.setLessonPlanPayloadJson(writeJson(payload));
        asset.setAiMetadataJson(writeJson(new LessonPlanMetadata(context.additionalInstructions(), context.availableResources(), payload.curriculumReferences(), extractDocumentTitle(context.sourceAsset()))));
        asset.setUploadedByUserId(context.actor().getId());
        asset.setUploadDate(OffsetDateTime.now());
        asset.setArchived(false);
        asset.setActive(true);
        asset.setDeleted(false);
        asset.setExtractionStatus("PUBLISHED");
        asset.setExtractionError(null);
        if ("PUBLISHED".equalsIgnoreCase(asset.getLessonPlanStatus())) {
            asset.setPublishedAt(OffsetDateTime.now());
        }
        updateFiles(asset, payload);
    }

    private LessonPlanPayload normalizeEditablePayload(CurriculumDtos.LessonPlanDraftSaveRequest request, CurriculumAsset asset) {
        LessonPlanPayload existing = loadPayload(asset);
        List<CurriculumDtos.LessonPlanStageDto> stages = request.stages() == null || request.stages().isEmpty() ? existing.stages() : request.stages();
        return new LessonPlanPayload(
                firstNonBlank(request.title(), existing.title()), firstNonBlank(request.schoolName(), existing.schoolName()), firstNonBlank(request.teacherName(), existing.teacherName()), firstNonBlank(request.className(), existing.className()),
                firstNonBlank(request.subject(), existing.subject()), normalizeGrade(firstNonBlank(request.grade(), existing.grade())), firstNonBlank(request.phase(), existing.phase()), firstNonNull(request.academicYear(), existing.academicYear()), normalizeTerm(firstNonBlank(request.term(), existing.term())), firstNonNull(request.weekNumber(), existing.weekNumber()),
                firstNonBlank(request.topic(), existing.topic()), firstNonNull(request.lessonDate(), existing.lessonDate()), firstNonNull(request.lessonDurationMinutes(), existing.lessonDurationMinutes()), firstNonBlank(request.language(), existing.language()), firstNonBlank(request.sourceAtpTitle(), existing.sourceAtpTitle()),
                firstNonBlank(request.curriculumReferences(), existing.curriculumReferences()), firstNonBlank(request.priorKnowledge(), existing.priorKnowledge()), firstNonBlank(request.availableResources(), existing.availableResources()), trim(request.additionalInstructions()) == null ? existing.additionalInstructions() : trim(request.additionalInstructions()),
                firstNonBlank(request.learningObjectives(), existing.learningObjectives()), firstNonBlank(request.introduction(), existing.introduction()), firstNonBlank(request.teacherActivities(), existing.teacherActivities()), firstNonBlank(request.learnerActivities(), existing.learnerActivities()), firstNonBlank(request.differentiation(), existing.differentiation()), firstNonBlank(request.assessment(), existing.assessment()),
                firstNonBlank(request.homework(), existing.homework()), firstNonBlank(request.reflection(), existing.reflection()), stages, buildDaysFromStages(stages, firstNonBlank(request.topic(), existing.topic()), firstNonBlank(request.learningObjectives(), existing.learningObjectives()), firstNonBlank(request.availableResources(), existing.availableResources()), firstNonBlank(request.assessment(), existing.assessment())), asset.isGeneratedByAi(), formatWeekEnding(firstNonNull(request.lessonDate(), existing.lessonDate())), existing.subtopic()
        );
    }

    private LessonPlanPayload loadPayload(CurriculumAsset asset) {
        try {
            if (asset.getLessonPlanPayloadJson() != null && !asset.getLessonPlanPayloadJson().isBlank()) {
                return objectMapper.readValue(asset.getLessonPlanPayloadJson(), LessonPlanPayload.class);
            }
        } catch (Exception ignored) {
        }
        return buildFallbackPayload(new ResolvedGenerationContext(requireSchool(asset.getSchoolId()), requireUser(firstNonNull(asset.getUploadedByUserId(), asset.getTeacherUserId()), "User not found."), requireUser(firstNonNull(asset.getTeacherUserId(), asset.getUploadedByUserId()), "Teacher not found."), asset.getClassId() == null ? null : schoolClassRepository.findById(asset.getClassId()).orElse(null), asset.getSourceCurriculumAssetId() == null ? asset : curriculumAssetRepository.findById(asset.getSourceCurriculumAssetId()).orElse(asset), asset.getSourceAtpCalendarItemId() == null ? null : atpCalendarItemRepository.findById(asset.getSourceAtpCalendarItemId()).orElse(null), firstNonBlank(asset.getSubject(), "Subject"), normalizeGrade(asset.getGrade()), firstNonBlank(asset.getCurriculumPhase(), ""), asset.getAcademicYear(), normalizeTerm(asset.getTerm()), firstNonNull(asset.getWeekNumber(), 1), firstNonBlank(asset.getLessonTopic(), asset.getTitle()), firstNonNull(asset.getLessonDate(), LocalDate.now()), firstNonNull(asset.getLessonDurationMinutes(), 60), firstNonBlank(asset.getLanguage(), "English"), null, null, asset.getTeacherUserId(), firstNonBlank(asset.getGenerationRequestKey(), "legacy")));
    }
    private LessonPlanPayload buildFallbackPayload(ResolvedGenerationContext context) {
        List<CurriculumDtos.LessonPlanStageDto> stages = List.of(
                new CurriculumDtos.LessonPlanStageDto("Introduction", "10 min", "Connect prior knowledge to the ATP topic and clarify the learning objective.", "Respond to the starter task and discuss prior knowledge."),
                new CurriculumDtos.LessonPlanStageDto("Teacher Modelling", "15 min", "Model the concept using the ATP guidance, examples, and teacher explanation.", "Observe the worked example and ask clarifying questions."),
                new CurriculumDtos.LessonPlanStageDto("Guided Practice", "15 min", "Lead scaffolded practice and check for understanding.", "Complete guided examples with a partner and respond to prompts."),
                new CurriculumDtos.LessonPlanStageDto("Independent Practice", "15 min", "Monitor classwork and support learners who need intervention.", "Complete the independent task linked to the ATP topic."),
                new CurriculumDtos.LessonPlanStageDto("Assessment and Reflection", "5 min", "Run the exit task, collect evidence, and summarise the lesson.", "Submit the exit task, reflect on learning, and capture homework."));
        String sourceTitle = extractDocumentTitle(context.sourceAsset());
        String learningObjectives = firstNonBlank(context.atpItem() == null ? null : context.atpItem().getLearningObjectives(), "Learners should master the selected ATP topic without drifting beyond the source content.");
        String resources = firstNonBlank(context.availableResources(), firstNonBlank(context.atpItem() == null ? null : context.atpItem().getResources(), "Textbook, worksheets, board work, and ATP-linked classroom resources."));
        String assessment = firstNonBlank(context.atpItem() == null ? null : context.atpItem().getAssessmentTask(), "Use oral questioning, classwork, and an exit task aligned to the ATP topic.");
        return new LessonPlanPayload(
                buildTitle(context.grade(), context.subject(), context.topic()),
                context.school().getSchoolName(), fullName(context.teacher()), context.schoolClass() == null ? "Unassigned Class" : context.schoolClass().getGrade() + " " + context.schoolClass().getClassName(),
                context.subject(), context.grade(), context.phase(), context.academicYear(), context.term(), context.weekNumber(), context.topic(), context.lessonDate(), context.lessonDurationMinutes(), context.language(), sourceTitle,
                "ATP source: " + sourceTitle + " | Subject: " + context.subject() + " | Grade: " + context.grade() + " | Term: " + context.term() + " | Week: " + context.weekNumber(),
                "Use the previous ATP topic, prerequisite concepts, and learner misconceptions as the bridge into this lesson.",
                resources, context.additionalInstructions(), learningObjectives,
                "Introduce the ATP topic, connect it to prior learning, and explain how the lesson will progress.",
                "Model the concept, scaffold guided practice, monitor learners closely, and conclude with reflective questioning.",
                "Participate in discussion, complete guided and independent work, and share evidence of understanding.",
                "Provide scaffolded prompts, extension tasks, language support, and targeted teacher intervention where required.",
                assessment,
                "Complete a short reinforcement activity that consolidates the published ATP topic.",
                "Capture what learners understood, what needs reteaching, and what to adjust for the next lesson.",
                stages, buildDaysFromStages(stages, context.topic(), learningObjectives, resources, assessment), true, formatWeekEnding(context.lessonDate()), firstNonBlank(context.atpItem() == null ? null : context.atpItem().getSubtopic(), context.topic()));
    }

    private LessonPlanPayload mergePayload(LessonPlanPayload fallback, AiLessonPlanPayload aiPayload, ResolvedGenerationContext context) {
        if (aiPayload == null) {
            return fallback;
        }
        List<CurriculumDtos.LessonPlanStageDto> stages = aiPayload.stages() == null || aiPayload.stages().isEmpty() ? fallback.stages() : aiPayload.stages().stream()
                .map(stage -> new CurriculumDtos.LessonPlanStageDto(firstNonBlank(stage.stage(), "Lesson Stage"), firstNonBlank(stage.duration(), "10 min"), firstNonBlank(stage.teacherActivities(), fallback.teacherActivities()), firstNonBlank(stage.learnerActivities(), fallback.learnerActivities()))).toList();
        String learningObjectives = firstNonBlank(aiPayload.learningObjectives(), fallback.learningObjectives());
        String resources = firstNonBlank(aiPayload.availableResources(), fallback.availableResources());
        String assessment = firstNonBlank(aiPayload.assessment(), fallback.assessment());
        return new LessonPlanPayload(fallback.title(), fallback.schoolName(), fallback.teacherName(), fallback.className(), fallback.subject(), fallback.grade(), fallback.phase(), fallback.academicYear(), fallback.term(), fallback.weekNumber(), fallback.topic(), fallback.lessonDate(), fallback.lessonDurationMinutes(), fallback.language(), fallback.sourceAtpTitle(), firstNonBlank(aiPayload.curriculumReferences(), fallback.curriculumReferences()), firstNonBlank(aiPayload.priorKnowledge(), fallback.priorKnowledge()), resources, fallback.additionalInstructions(), learningObjectives, firstNonBlank(aiPayload.introduction(), fallback.introduction()), firstNonBlank(aiPayload.teacherActivities(), fallback.teacherActivities()), firstNonBlank(aiPayload.learnerActivities(), fallback.learnerActivities()), firstNonBlank(aiPayload.differentiation(), fallback.differentiation()), assessment, firstNonBlank(aiPayload.homework(), fallback.homework()), firstNonBlank(aiPayload.reflection(), fallback.reflection()), stages, buildDaysFromStages(stages, context.topic(), learningObjectives, resources, assessment), true, fallback.weekEnding(), fallback.subtopic());
    }

    private String buildPrompt(ResolvedGenerationContext context, String atpText, String syllabusText) {
        return """
                Generate a grounded lesson plan from the supplied ATP and syllabus extracts.
                Return ONLY strict JSON with fields curriculumReferences, priorKnowledge, learningObjectives, introduction, teacherActivities, learnerActivities, differentiation, assessment, homework, reflection, availableResources, and stages[].
                Each stage must include stage, duration, teacherActivities, learnerActivities.
                Do not invent curriculum topics or references outside the supplied ATP.
                School: %s
                Teacher: %s
                Class: %s
                Subject: %s
                Grade: %s
                Phase: %s
                Academic year: %s
                Term: %s
                Week: %s
                Topic: %s
                Lesson date: %s
                Duration minutes: %s
                Language: %s
                Additional instructions: %s
                Selected resources: %s
                ATP source title: %s
                ATP source text:
                %s
                Syllabus support text:
                %s
                """.formatted(context.school().getSchoolName(), fullName(context.teacher()), context.schoolClass() == null ? "Unassigned Class" : context.schoolClass().getGrade() + " " + context.schoolClass().getClassName(), context.subject(), context.grade(), context.phase(), context.academicYear(), context.term(), context.weekNumber(), context.topic(), context.lessonDate(), context.lessonDurationMinutes(), context.language(), firstNonBlank(context.additionalInstructions(), "None"), firstNonBlank(context.availableResources(), "None"), extractDocumentTitle(context.sourceAsset()), truncate(atpText, 14000), truncate(syllabusText, 8000));
    }

    private CurriculumDtos.TeacherLessonPlanResponse toResponse(CurriculumAsset asset, LessonPlanPayload payload, boolean alreadyExisted) {
        return new CurriculumDtos.TeacherLessonPlanResponse(asset.getId(), asset.getSourceAtpCalendarItemId(), asset.getSourceCurriculumAssetId(), alreadyExisted, asset.getPdfBytes() != null && asset.getPdfBytes().length > 0, asset.getDocxBytes() != null && asset.getDocxBytes().length > 0, firstNonBlank(asset.getLessonPlanStatus(), "DRAFT"), asset.isGeneratedByAi(), payload.title(), payload.schoolName(), payload.teacherName(), payload.className(), payload.weekEnding(), payload.subtopic(), payload.sourceAtpTitle(), payload.subject(), payload.grade(), payload.phase(), payload.academicYear(), payload.term(), payload.weekNumber(), payload.topic(), payload.lessonDate(), payload.lessonDurationMinutes(), payload.language(), payload.curriculumReferences(), payload.priorKnowledge(), payload.availableResources(), payload.additionalInstructions(), payload.learningObjectives(), payload.introduction(), payload.teacherActivities(), payload.learnerActivities(), payload.availableResources(), payload.assessment(), payload.homework(), payload.differentiation(), payload.reflection(), payload.stages(), payload.days(), asset.getAiGeneratedAt(), asset.getAiProvider(), asset.getAiModel());
    }

    private List<CurriculumDtos.TeacherLessonPlanDayDto> buildDaysFromStages(List<CurriculumDtos.LessonPlanStageDto> stages, String topic, String objectives, String resources, String assessment) {
        List<String> labels = List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday");
        List<CurriculumDtos.TeacherLessonPlanDayDto> days = new ArrayList<>();
        for (int index = 0; index < labels.size(); index++) {
            CurriculumDtos.LessonPlanStageDto stage = stages.isEmpty() ? null : stages.get(Math.min(index, stages.size() - 1));
            days.add(new CurriculumDtos.TeacherLessonPlanDayDto(labels.get(index), topic, objectives, resources, resources, stage == null ? "Teacher and learner activities are captured in the main lesson-plan stages." : stage.teacherActivities() + "\nLearners: " + stage.learnerActivities(), assessment));
        }
        return days;
    }

    private byte[] buildPdf(LessonPlanPayload payload) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4); document.addPage(page);
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA); PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                float y = 790; y = writePdfLine(stream, bold, 16, 48, y, payload.title());
                for (String section : List.of(payload.schoolName() + " | " + payload.teacherName() + " | " + payload.className(), payload.subject() + " | " + payload.grade() + " | " + payload.term() + " Week " + payload.weekNumber(), "Topic: " + payload.topic(), "Curriculum References: " + payload.curriculumReferences(), "Prior Knowledge: " + payload.priorKnowledge(), "Learning Objectives: " + payload.learningObjectives(), "Introduction: " + payload.introduction(), "Teacher Activities: " + payload.teacherActivities(), "Learner Activities: " + payload.learnerActivities(), "Differentiation: " + payload.differentiation(), "Assessment: " + payload.assessment(), "Homework: " + payload.homework(), "Reflection: " + payload.reflection())) {
                    for (String line : wrapPdfLine(section, 95)) { y = writePdfLine(stream, regular, 10, 48, y, line); }
                }
                for (CurriculumDtos.LessonPlanStageDto stage : payload.stages()) {
                    y = writePdfLine(stream, bold, 11, 48, y, stage.stage() + " - " + stage.duration());
                    for (String line : wrapPdfLine("Teacher: " + stage.teacherActivities(), 92)) { y = writePdfLine(stream, regular, 10, 56, y, line); }
                    for (String line : wrapPdfLine("Learners: " + stage.learnerActivities(), 92)) { y = writePdfLine(stream, regular, 10, 56, y, line); }
                }
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream(); document.save(output); return output.toByteArray();
        } catch (Exception ex) { return payload.title().getBytes(StandardCharsets.UTF_8); }
    }

    private byte[] buildDocx(LessonPlanPayload payload) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(output)) {
            addZipEntry(zip, "[Content_Types].xml", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/></Types>");
            addZipEntry(zip, "_rels/.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/></Relationships>");
            StringBuilder documentXml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>");
            for (String line : List.of(payload.title(), payload.schoolName() + " | " + payload.teacherName() + " | " + payload.className(), payload.subject() + " | " + payload.grade() + " | " + payload.term() + " Week " + payload.weekNumber(), "Topic: " + payload.topic(), "Curriculum References: " + payload.curriculumReferences(), "Prior Knowledge: " + payload.priorKnowledge(), "Learning Objectives: " + payload.learningObjectives(), "Introduction: " + payload.introduction(), "Teacher Activities: " + payload.teacherActivities(), "Learner Activities: " + payload.learnerActivities(), "Assessment: " + payload.assessment(), "Homework: " + payload.homework(), "Reflection: " + payload.reflection())) { appendDocxParagraph(documentXml, line); }
            for (CurriculumDtos.LessonPlanStageDto stage : payload.stages()) { appendDocxParagraph(documentXml, stage.stage() + " - " + stage.duration()); appendDocxParagraph(documentXml, "Teacher Activities: " + stage.teacherActivities()); appendDocxParagraph(documentXml, "Learner Activities: " + stage.learnerActivities()); }
            documentXml.append("</w:body></w:document>"); addZipEntry(zip, "word/document.xml", documentXml.toString()); zip.finish(); return output.toByteArray();
        } catch (Exception ex) { return payload.title().getBytes(StandardCharsets.UTF_8); }
    }
    private void updateFiles(CurriculumAsset asset, LessonPlanPayload payload) {
        asset.setPdfFileName(slugify(payload.title()) + ".pdf");
        asset.setPdfContentType("application/pdf");
        asset.setPdfBytes(buildPdf(payload));
        asset.setPdfBase64(null);
        asset.setDocxFileName(slugify(payload.title()) + ".docx");
        asset.setDocxContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        asset.setDocxBytes(buildDocx(payload));
        asset.setDocxBase64(null);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new ResourceConflictException("Unable to persist the lesson-plan data.");
        }
    }

    private String buildAssetDescription(LessonPlanPayload payload, CurriculumAsset asset) {
        return String.join("\n",
                "Lesson Topic: " + firstNonBlank(payload.topic(), "Not set"),
                "Subject: " + firstNonBlank(payload.subject(), "Not set"),
                "Grade: " + firstNonBlank(payload.grade(), "Not set"),
                "Phase: " + firstNonBlank(payload.phase(), "Not set"),
                "Term/Week: " + firstNonBlank(payload.term(), "Not set") + " / " + firstNonNull(payload.weekNumber(), 0),
                "Teacher: " + firstNonBlank(payload.teacherName(), "Unassigned"),
                "Status: " + firstNonBlank(asset.getLessonPlanStatus(), "DRAFT")
        );
    }

    private String resolvePrimaryProvider() {
        return firstNonBlank(environment.getProperty("ai.primary-provider"), "gemini");
    }

    private String resolveModelName() {
        String provider = resolvePrimaryProvider().toLowerCase(Locale.ROOT);
        return switch (provider) {
            case "openai" -> firstNonBlank(environment.getProperty("openai.model"), "gpt-4o-mini");
            case "openrouter" -> firstNonBlank(environment.getProperty("openrouter.model"), "openai/gpt-4o-mini");
            default -> firstNonBlank(environment.getProperty("gemini.model"), "gemini-2.5-flash");
        };
    }

    private CurriculumAsset requireLessonPlanAsset(UUID assetId, UUID schoolId) {
        CurriculumAsset asset = curriculumAssetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceConflictException("Lesson plan not found."));
        if (!Objects.equals(asset.getSchoolId(), schoolId) || !"LESSON_PLAN".equalsIgnoreCase(asset.getRepositoryType())) {
            throw new ResourceConflictException("Lesson plan not found.");
        }
        return asset;
    }

    private School requireSchool(UUID schoolId) {
        return schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceConflictException("School not found."));
    }

    private User requireUser(UUID userId, String message) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceConflictException(message));
    }

    private boolean subjectMatches(UUID subjectId, UUID schoolId, String subjectName) {
        if (subjectId == null || subjectName == null) {
            return false;
        }
        return schoolSubjectRepository.findBySchoolIdAndActiveTrue(schoolId).stream()
                .filter(item -> Objects.equals(item.getId(), subjectId))
                .anyMatch(item -> normalize(item.getSubjectName()).equals(normalize(subjectName)));
    }

    private boolean gradeMatches(String left, String right) {
        return normalizeGrade(left).equals(normalizeGrade(right));
    }

    private boolean phaseMatches(String left, String right) {
        String normalizedLeft = normalize(left);
        String normalizedRight = normalize(right);
        return normalizedLeft.isBlank() || normalizedRight.isBlank() || normalizedLeft.equals(normalizedRight);
    }

    private boolean isVisibleToSchool(CurriculumAsset asset, School school) {
        return Objects.equals(asset.getDistrictId(), school.getDistrictId())
                && (asset.getSchoolId() == null || Objects.equals(asset.getSchoolId(), school.getId()))
                && !asset.isDeleted()
                && !asset.isArchived()
                && asset.isActive();
    }

    private String extractDocumentText(CurriculumAsset asset) {
        byte[] pdfBytes = asset.getPdfBytes();
        if (pdfBytes != null && pdfBytes.length > 0) {
            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                return new PDFTextStripper().getText(document);
            } catch (Exception ignored) {
            }
        }
        byte[] docxBytes = asset.getDocxBytes();
        if (docxBytes != null && docxBytes.length > 0) {
            return extractDocxText(docxBytes);
        }
        return firstNonBlank(asset.getDescription(), "");
    }

    private String extractDocxText(byte[] docxBytes) {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(docxBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!"word/document.xml".equals(entry.getName())) {
                    continue;
                }
                String xml = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                Matcher matcher = XML_TEXT_PATTERN.matcher(xml);
                StringBuilder builder = new StringBuilder();
                while (matcher.find()) {
                    builder.append(matcher.group(1)).append(' ');
                }
                return builder.toString().trim();
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private String extractDocumentTitle(CurriculumAsset asset) {
        return firstNonBlank(asset.getTitle(), "District ATP");
    }

    private String fullName(User user) {
        return (firstNonBlank(user.getFirstName(), "") + " " + firstNonBlank(user.getLastName(), "")).trim().isBlank()
                ? firstNonBlank(user.getEmail(), "Teacher")
                : (firstNonBlank(user.getFirstName(), "") + " " + firstNonBlank(user.getLastName(), "")).trim();
    }

    private String buildTitle(String grade, String subject, String topic) {
        return grade + " " + subject + " Lesson Plan - " + topic;
    }

    private String formatWeekEnding(LocalDate lessonDate) {
        return lessonDate == null ? "Not set" : DISPLAY_DATE.format(lessonDate);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private float writePdfLine(PDPageContentStream stream, PDType1Font font, int fontSize, float x, float y, String text) throws Exception {
        if (y < 40) {
            return y;
        }
        stream.beginText();
        stream.setFont(font, fontSize);
        stream.newLineAtOffset(x, y);
        stream.showText(text == null ? "" : text);
        stream.endText();
        return y - (fontSize + 4);
    }

    private List<String> wrapPdfLine(String value, int maxChars) {
        if (value == null || value.isBlank()) {
            return List.of("");
        }
        List<String> lines = new ArrayList<>();
        String remaining = value.trim();
        while (remaining.length() > maxChars) {
            int split = remaining.lastIndexOf(' ', maxChars);
            if (split <= 0) {
                split = maxChars;
            }
            lines.add(remaining.substring(0, split).trim());
            remaining = remaining.substring(split).trim();
        }
        if (!remaining.isBlank()) {
            lines.add(remaining);
        }
        return lines;
    }

    private void addZipEntry(ZipOutputStream zip, String name, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private void appendDocxParagraph(StringBuilder xml, String text) {
        xml.append("<w:p><w:r><w:t>")
                .append(escapeXml(firstNonBlank(text, "")))
                .append("</w:t></w:r></w:p>");
    }

    private String escapeXml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String generationRequestKey(UUID schoolId, UUID teacherUserId, UUID classId, UUID sourceAssetId, UUID sourceAtpCalendarItemId, String term, Integer weekNumber, String topic, LocalDate lessonDate, String language) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String raw = List.of(
                    String.valueOf(schoolId),
                    String.valueOf(teacherUserId),
                    String.valueOf(classId),
                    String.valueOf(sourceAssetId),
                    String.valueOf(sourceAtpCalendarItemId),
                    firstNonBlank(term, ""),
                    String.valueOf(firstNonNull(weekNumber, 0)),
                    firstNonBlank(topic, ""),
                    String.valueOf(lessonDate),
                    firstNonBlank(language, "English")
            ).stream().collect(Collectors.joining("|"));
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (Exception ex) {
            return UUID.randomUUID().toString();
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeGrade(String value) {
        return normalize(value).replace("grade", "").replaceAll("\\s+", "");
    }

    private String normalizeTerm(String value) {
        if (value == null || value.isBlank()) {
            return "Term 1";
        }
        String trimmed = value.trim();
        return trimmed.toLowerCase(Locale.ROOT).startsWith("term") ? trimmed : "Term " + trimmed;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String firstNonBlank(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first.trim();
    }

    private <T> T firstNonNull(T first, T fallback) {
        return first != null ? first : fallback;
    }

    private String slugify(String value) {
        String normalized = normalize(value).replaceAll("[^a-z0-9]+", "-");
        return normalized.isBlank() ? "lesson-plan" : normalized.replaceAll("^-+|-+$", "");
    }

    private record ResolvedGenerationContext(
            School school,
            User actor,
            User teacher,
            SchoolClass schoolClass,
            CurriculumAsset sourceAsset,
            AtpCalendarItem atpItem,
            String subject,
            String grade,
            String phase,
            Integer academicYear,
            String term,
            Integer weekNumber,
            String topic,
            LocalDate lessonDate,
            Integer lessonDurationMinutes,
            String language,
            String availableResources,
            String additionalInstructions,
            UUID teacherUserId,
            String generationRequestKey
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AiLessonPlanPayload(
            String curriculumReferences,
            String priorKnowledge,
            String learningObjectives,
            String introduction,
            String teacherActivities,
            String learnerActivities,
            String differentiation,
            String assessment,
            String homework,
            String reflection,
            String availableResources,
            List<AiLessonPlanStagePayload> stages
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AiLessonPlanStagePayload(
            String stage,
            String duration,
            String teacherActivities,
            String learnerActivities
    ) {}

    private record LessonPlanMetadata(
            String additionalInstructions,
            String selectedResources,
            String curriculumReferences,
            String sourceTitle
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LessonPlanPayload(
            String title,
            String schoolName,
            String teacherName,
            String className,
            String subject,
            String grade,
            String phase,
            Integer academicYear,
            String term,
            Integer weekNumber,
            String topic,
            LocalDate lessonDate,
            Integer lessonDurationMinutes,
            String language,
            String sourceAtpTitle,
            String curriculumReferences,
            String priorKnowledge,
            String availableResources,
            String additionalInstructions,
            String learningObjectives,
            String introduction,
            String teacherActivities,
            String learnerActivities,
            String differentiation,
            String assessment,
            String homework,
            String reflection,
            List<CurriculumDtos.LessonPlanStageDto> stages,
            List<CurriculumDtos.TeacherLessonPlanDayDto> days,
            boolean generatedByAi,
            String weekEnding,
            String subtopic
    ) {}
}
