package com.edurite.universityapplication.service;

import com.edurite.common.exception.ResourceConflictException;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.service.StudentContextService;
import com.edurite.universityapplication.dto.UniversityApplicationDtos.UniversityApplicationRequest;
import com.edurite.universityapplication.dto.UniversityApplicationDtos.UniversityApplicationResponse;
import com.edurite.universityapplication.entity.UniversityApplication;
import com.edurite.universityapplication.repository.UniversityApplicationRepository;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UniversityApplicationService {

    private static final Set<String> STATUSES = Set.of("DRAFT", "READY", "SUBMITTED", "ACCEPTED", "REJECTED", "WAITLISTED");

    private final StudentContextService studentContextService;
    private final UniversityApplicationRepository repository;

    public UniversityApplicationService(StudentContextService studentContextService, UniversityApplicationRepository repository) {
        this.studentContextService = studentContextService;
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<UniversityApplicationResponse> list(Principal principal) {
        StudentProfile profile = studentContextService.requireStudent(principal);
        return repository.findByStudentIdOrderByApplicationDeadlineAscCreatedAtDesc(profile.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UniversityApplicationResponse create(Principal principal, UniversityApplicationRequest request) {
        StudentProfile profile = studentContextService.requireStudent(principal);
        UniversityApplication application = new UniversityApplication();
        application.setStudentId(profile.getId());
        apply(application, request);
        return toResponse(repository.save(application));
    }

    @Transactional
    public UniversityApplicationResponse update(Principal principal, UUID id, UniversityApplicationRequest request) {
        StudentProfile profile = studentContextService.requireStudent(principal);
        UniversityApplication application = repository.findByIdAndStudentId(id, profile.getId())
                .orElseThrow(() -> new ResourceConflictException("University application not found"));
        apply(application, request);
        return toResponse(repository.save(application));
    }

    @Transactional
    public void delete(Principal principal, UUID id) {
        StudentProfile profile = studentContextService.requireStudent(principal);
        UniversityApplication application = repository.findByIdAndStudentId(id, profile.getId())
                .orElseThrow(() -> new ResourceConflictException("University application not found"));
        repository.delete(application);
    }

    private void apply(UniversityApplication application, UniversityApplicationRequest request) {
        application.setUniversityName(clean(request.universityName()));
        application.setProgrammeName(clean(request.programmeName()));
        application.setCountry(clean(request.country()));
        application.setIntakeYear(request.intakeYear());
        application.setApplicationDeadline(request.applicationDeadline());
        application.setApplicationStatus(normalizeStatus(request.applicationStatus()));
        application.setNotes(clean(request.notes()));
        application.setDocumentReferences(clean(request.documentReferences()));
    }

    private String normalizeStatus(String value) {
        if (value == null || value.isBlank()) {
            return "DRAFT";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(normalized)) {
            throw new ResourceConflictException("Unsupported university application status: " + value);
        }
        return normalized;
    }

    private UniversityApplicationResponse toResponse(UniversityApplication application) {
        LocalDate deadline = application.getApplicationDeadline();
        boolean deadlineSoon = deadline != null
                && !deadline.isBefore(LocalDate.now())
                && !deadline.isAfter(LocalDate.now().plusDays(30));
        return new UniversityApplicationResponse(
                application.getId(),
                application.getUniversityName(),
                application.getProgrammeName(),
                valueOr(application.getCountry(), ""),
                application.getIntakeYear(),
                deadline,
                application.getApplicationStatus(),
                valueOr(application.getNotes(), ""),
                valueOr(application.getDocumentReferences(), ""),
                deadlineSoon,
                application.getUpdatedAt()
        );
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}

