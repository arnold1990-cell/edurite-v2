package com.edurite.school.service;

import com.edurite.common.exception.ResourceConflictException;
import com.edurite.school.dto.SchoolLinkDtos;
import com.edurite.school.portal.entity.School;
import com.edurite.school.portal.entity.SchoolUserProfile;
import com.edurite.school.portal.entity.LearnerEnrollment;
import com.edurite.school.portal.entity.StudentSchoolLink;
import com.edurite.school.portal.entity.SchoolClass;
import com.edurite.school.portal.repository.LearnerEnrollmentRepository;
import com.edurite.school.portal.repository.SchoolRepository;
import com.edurite.school.portal.repository.SchoolClassRepository;
import com.edurite.school.portal.repository.SchoolUserProfileRepository;
import com.edurite.school.portal.repository.StudentSchoolLinkRepository;
import com.edurite.security.service.CurrentUserService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.user.entity.User;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MySchoolService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_NONE = "NONE";

    private final CurrentUserService currentUserService;
    private final SchoolRepository schoolRepository;
    private final StudentSchoolLinkRepository studentSchoolLinkRepository;
    private final SchoolUserProfileRepository schoolUserProfileRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final LearnerEnrollmentRepository learnerEnrollmentRepository;
    private final SchoolClassRepository schoolClassRepository;

    public MySchoolService(
            CurrentUserService currentUserService,
            SchoolRepository schoolRepository,
            StudentSchoolLinkRepository studentSchoolLinkRepository,
            SchoolUserProfileRepository schoolUserProfileRepository,
            StudentProfileRepository studentProfileRepository,
            LearnerEnrollmentRepository learnerEnrollmentRepository,
            SchoolClassRepository schoolClassRepository
    ) {
        this.currentUserService = currentUserService;
        this.schoolRepository = schoolRepository;
        this.studentSchoolLinkRepository = studentSchoolLinkRepository;
        this.schoolUserProfileRepository = schoolUserProfileRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.learnerEnrollmentRepository = learnerEnrollmentRepository;
        this.schoolClassRepository = schoolClassRepository;
    }

    @Transactional(readOnly = true)
    public List<SchoolLinkDtos.PublicSchoolDto> listActiveSchools() {
        return schoolRepository.findByStatusIgnoreCaseOrderBySchoolNameAsc("ACTIVE").stream()
                .map(this::toPublicSchool)
                .toList();
    }

    @Transactional(readOnly = true)
    public SchoolLinkDtos.StudentSchoolStatusDto studentStatus(Principal principal) {
        User user = currentUserService.requireUser(principal);
        return toStudentStatus(studentSchoolLinkRepository.findByStudentId(user.getId()).orElse(null));
    }

    @Transactional
    public SchoolLinkDtos.StudentSchoolStatusDto requestJoin(Principal principal, UUID schoolId) {
        User user = currentUserService.requireUser(principal);
        School school = schoolRepository.findById(schoolId)
                .filter(item -> "ACTIVE".equalsIgnoreCase(item.getStatus()))
                .orElseThrow(() -> new ResourceConflictException("Selected school is not available."));

        StudentSchoolLink link = studentSchoolLinkRepository.findByStudentId(user.getId()).orElse(null);
        if (link != null) {
            if (STATUS_APPROVED.equalsIgnoreCase(link.getStatus())) {
                throw new ResourceConflictException("Your school link is already approved.");
            }
            if (STATUS_PENDING.equalsIgnoreCase(link.getStatus())) {
                throw new ResourceConflictException("You already have a pending school request.");
            }
        } else {
            link = new StudentSchoolLink();
            link.setStudentId(user.getId());
        }

        String schoolCode = ensureSchoolCode(school);
        link.setSchoolId(school.getId());
        link.setSchoolCode(schoolCode);
        link.setStatus(STATUS_PENDING);
        link.setGeneratedUsername(null);
        link.setRequestedAt(OffsetDateTime.now());
        link.setApprovedAt(null);
        link.setApprovedBy(null);
        link.setRejectedAt(null);
        link.setRejectedBy(null);
        return toStudentStatus(studentSchoolLinkRepository.save(link));
    }

    @Transactional(readOnly = true)
    public List<SchoolLinkDtos.SchoolJoinRequestItemDto> listSchoolRequests(UUID schoolId, String status) {
        List<StudentSchoolLink> links = trim(status) == null
                ? studentSchoolLinkRepository.findBySchoolIdOrderByRequestedAtDesc(schoolId)
                : studentSchoolLinkRepository.findBySchoolIdAndStatusIgnoreCaseOrderByRequestedAtDesc(schoolId, status);
        return links.stream()
                .map(this::toRequestItem)
                .toList();
    }

    @Transactional
    public SchoolLinkDtos.SchoolJoinRequestItemDto approveRequest(UUID schoolId, UUID approvedBy, UUID requestId) {
        StudentSchoolLink link = studentSchoolLinkRepository.findById(requestId)
                .orElseThrow(() -> new ResourceConflictException("School join request not found."));
        if (!schoolId.equals(link.getSchoolId())) {
            throw new ResourceConflictException("You can only approve requests for your school.");
        }
        if (!STATUS_PENDING.equalsIgnoreCase(link.getStatus())) {
            throw new ResourceConflictException("Only pending requests can be approved.");
        }

        School school = schoolRepository.findById(schoolId).orElseThrow(() -> new ResourceConflictException("School not found."));
        String schoolCode = ensureSchoolCode(school);
        User user = currentUserService.requireUserById(link.getStudentId());
        SchoolUserProfile existingProfile = schoolUserProfileRepository.findByUserIdAndDeletedFalse(user.getId()).orElse(null);
        String generatedUsername = existingPortalUsername(existingProfile);
        if (generatedUsername == null) {
            generatedUsername = generateUniqueUsername(user, schoolCode);
        }
        if (existingProfile != null && !schoolId.equals(existingProfile.getSchoolId())) {
            throw new ResourceConflictException("This learner is already configured for another school portal.");
        }
        SchoolUserProfile profile = existingProfile == null ? new SchoolUserProfile() : existingProfile;
        profile.setSchoolId(schoolId);
        profile.setUserId(user.getId());
        profile.setRoleName(SchoolAccessService.ROLE_SCHOOL_STUDENT);
        profile.setPortalUsername(generatedUsername);
        profile.setEmployeeOrStudentNo(generatedUsername);
        profile.setInitialPassword(null);
        profile.setActive(true);
        profile.setDeleted(false);
        schoolUserProfileRepository.save(profile);

        link.setSchoolCode(schoolCode);
        link.setStatus(STATUS_APPROVED);
        link.setGeneratedUsername(generatedUsername);
        link.setApprovedAt(OffsetDateTime.now());
        link.setApprovedBy(approvedBy);
        link.setRejectedAt(null);
        link.setRejectedBy(null);
        return toRequestItem(studentSchoolLinkRepository.save(link));
    }

    @Transactional
    public SchoolLinkDtos.SchoolJoinRequestItemDto rejectRequest(UUID schoolId, UUID rejectedBy, UUID requestId) {
        StudentSchoolLink link = studentSchoolLinkRepository.findById(requestId)
                .orElseThrow(() -> new ResourceConflictException("School join request not found."));
        if (!schoolId.equals(link.getSchoolId())) {
            throw new ResourceConflictException("You can only reject requests for your school.");
        }
        if (!STATUS_PENDING.equalsIgnoreCase(link.getStatus())) {
            throw new ResourceConflictException("Only pending requests can be rejected.");
        }

        link.setStatus(STATUS_REJECTED);
        link.setRejectedAt(OffsetDateTime.now());
        link.setRejectedBy(rejectedBy);
        link.setApprovedAt(null);
        link.setApprovedBy(null);
        return toRequestItem(studentSchoolLinkRepository.save(link));
    }

    @Transactional(readOnly = true)
    public SchoolAccessService.AccessContext resolveApprovedStudentSchoolContext(UUID studentUserId) {
        StudentSchoolLink link = studentSchoolLinkRepository.findByStudentIdAndStatusIgnoreCase(studentUserId, STATUS_APPROVED)
                .orElseThrow(() -> new ResourceConflictException("School portal access has not been approved yet."));
        return new SchoolAccessService.AccessContext(studentUserId, link.getSchoolId(), SchoolAccessService.ROLE_SCHOOL_STUDENT);
    }

    private SchoolLinkDtos.PublicSchoolDto toPublicSchool(School school) {
        return new SchoolLinkDtos.PublicSchoolDto(school.getId(), school.getSchoolName(), resolveSchoolCode(school));
    }

    private SchoolLinkDtos.StudentSchoolStatusDto toStudentStatus(StudentSchoolLink link) {
        if (link == null) {
            return new SchoolLinkDtos.StudentSchoolStatusDto(STATUS_NONE, null, null, null, null, null, null, null, null);
        }
        School school = schoolRepository.findById(link.getSchoolId()).orElse(null);
        SchoolLinkDtos.PublicSchoolDto schoolDto = school == null ? null : toPublicSchool(school);
        LearnerPlacement placement = resolveLearnerPlacement(link.getSchoolId(), link.getStudentId());
        String normalizedStatus = link.getStatus() == null ? STATUS_NONE : link.getStatus().toUpperCase(Locale.ROOT);
        String message = switch (normalizedStatus) {
            case STATUS_PENDING -> schoolDto == null ? "Your request is pending approval." : "Your request has been sent to " + schoolDto.name() + ". Please wait for school admin approval.";
            case STATUS_APPROVED -> schoolDto == null ? "Your school access has been approved." : "Your school access has been approved for " + schoolDto.name() + ".";
            case STATUS_REJECTED -> schoolDto == null ? "Your school request was rejected." : "Your request to join " + schoolDto.name() + " was rejected.";
            default -> null;
        };
        return new SchoolLinkDtos.StudentSchoolStatusDto(
                link.getStatus(),
                schoolDto,
                link.getGeneratedUsername(),
                placement.grade(),
                placement.className(),
                message,
                link.getRequestedAt(),
                link.getApprovedAt(),
                link.getRejectedAt()
        );
    }

    private SchoolLinkDtos.SchoolJoinRequestItemDto toRequestItem(StudentSchoolLink link) {
        User user = currentUserService.requireUserById(link.getStudentId());
        School school = schoolRepository.findById(link.getSchoolId()).orElseThrow(() -> new ResourceConflictException("School not found."));
        String learnerName = ("%s %s".formatted(safe(user.getFirstName()), safe(user.getLastName()))).trim();
        return new SchoolLinkDtos.SchoolJoinRequestItemDto(
                link.getId(),
                link.getStudentId(),
                learnerName.isBlank() ? user.getEmail() : learnerName,
                user.getEmail(),
                trim(user.getPhoneNumber()),
                school.getId(),
                school.getSchoolName(),
                resolveSchoolCode(school),
                link.getRequestedAt(),
                link.getStatus(),
                link.getGeneratedUsername()
        );
    }

    private String existingPortalUsername(SchoolUserProfile profile) {
        if (profile == null) {
            return null;
        }
        return trim(profile.getPortalUsername());
    }

    private LearnerPlacement resolveLearnerPlacement(UUID schoolId, UUID studentUserId) {
        List<LearnerEnrollment> enrollments = learnerEnrollmentRepository.findBySchoolIdAndLearnerUserIdAndActiveTrue(schoolId, studentUserId);
        for (LearnerEnrollment enrollment : enrollments) {
            SchoolClass schoolClass = schoolClassRepository.findById(enrollment.getClassId()).orElse(null);
            if (schoolClass != null) {
                return new LearnerPlacement(trim(schoolClass.getGrade()), trim(schoolClass.getClassName()));
            }
        }
        return new LearnerPlacement(null, null);
    }

    private record LearnerPlacement(String grade, String className) {}

    private String generateUniqueUsername(User user, String schoolCode) {
        StudentProfile profile = studentProfileRepository.findByUserId(user.getId()).orElse(null);
        String firstNameSource = profile != null && profile.getFirstName() != null ? profile.getFirstName() : user.getFirstName();
        String lastNameSource = profile != null && profile.getLastName() != null ? profile.getLastName() : user.getLastName();
        String[] firstNameParts = safe(firstNameSource).trim().split("\\s+");
        String firstName = firstNameParts.length == 0 ? "learner" : sanitizeLower(firstNameParts[0]);
        StringBuilder suffix = new StringBuilder();
        for (int index = 1; index < firstNameParts.length; index++) {
            String part = sanitizeLower(firstNameParts[index]);
            if (!part.isEmpty()) {
                suffix.append(part.charAt(0));
            }
        }
        String lastName = sanitizeLower(lastNameSource);
        if (!lastName.isEmpty()) {
            suffix.append(lastName.charAt(0));
        }
        String base = (firstName + suffix + schoolCode).replaceAll("[^a-zA-Z0-9]", "");
        String candidate = base;
        int counter = 2;
        while (studentSchoolLinkRepository.existsByGeneratedUsernameIgnoreCase(candidate)
                || schoolUserProfileRepository.existsByPortalUsernameIgnoreCaseAndDeletedFalse(candidate)) {
            candidate = base + counter++;
        }
        return candidate;
    }

    private String ensureSchoolCode(School school) {
        String existing = trim(school.getSchoolCode());
        if (existing != null) {
            return existing;
        }
        String generated = resolveSchoolCode(school);
        school.setSchoolCode(generated);
        schoolRepository.save(school);
        return school.getSchoolCode();
    }

    private String resolveSchoolCode(School school) {
        String existing = trim(school.getSchoolCode());
        if (existing != null) {
            return existing;
        }
        String[] parts = safe(school.getSchoolName()).trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (String part : parts) {
            String normalized = part.replaceAll("[^A-Za-z0-9]", "");
            if (!normalized.isEmpty()) {
                initials.append(Character.toUpperCase(normalized.charAt(0)));
            }
        }
        String fallback = sanitizeUpper(school.getSchoolName());
        String generated;
        if (initials.length() >= 3) {
            generated = initials.substring(0, Math.min(initials.length(), 8));
        } else if (!fallback.isEmpty()) {
            generated = fallback.substring(0, Math.min(fallback.length(), 8));
        } else {
            generated = "EDU";
        }
        return generated.isBlank() ? "EDU" : generated;
    }

    private String sanitizeLower(String value) {
        return safe(value).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private String sanitizeUpper(String value) {
        return safe(value).toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
