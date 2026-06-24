package com.edurite.opportunity.service;

import com.edurite.career.entity.Career;
import com.edurite.career.repository.CareerRepository;
import com.edurite.opportunity.dto.OpportunityType;
import com.edurite.opportunity.dto.UnifiedOpportunityResponse;
import com.edurite.student.entity.SavedCareer;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.SavedCareerRepository;
import com.edurite.student.service.StudentService;
import java.security.Principal;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class StudentOpportunityService {

    private final CareerRepository careerRepository;
    private final SavedCareerRepository savedCareerRepository;
    private final StudentService studentService;

    public StudentOpportunityService(
            CareerRepository careerRepository,
            SavedCareerRepository savedCareerRepository,
            StudentService studentService
    ) {
        this.careerRepository = careerRepository;
        this.savedCareerRepository = savedCareerRepository;
        this.studentService = studentService;
    }

    public List<UnifiedOpportunityResponse> search(
            Principal principal,
            String q,
            String field,
            String industry,
            String qualification,
            String location,
            String demand,
            String opportunityType
    ) {
        StudentProfile profile = studentService.getProfileEntity(principal);
        Set<String> savedKeys = savedKeys(profile.getId());
        OpportunityTypeFilter typeFilter = OpportunityTypeFilter.from(opportunityType);

        Stream<UnifiedOpportunityResponse> careers = typeFilter.includes(OpportunityType.CAREER)
                ? careerRepository.search(
                                safe(q),
                                safe(industry).isBlank() ? safe(field) : safe(industry),
                                safe(qualification),
                                safe(location),
                                safe(demand),
                                "",
                                PageRequest.of(0, 50))
                        .stream()
                        .map(career -> mapCareer(career, savedKeys, profile))
                : Stream.empty();

        Stream<UnifiedOpportunityResponse> catalog = catalog().stream()
                .filter(item -> typeFilter.includes(item.type()))
                .filter(item -> matches(item, q, field, industry, qualification, location, demand))
                .map(item -> mapCatalog(item, savedKeys, profile));

        return Stream.concat(careers, catalog)
                .sorted(Comparator
                        .comparing(UnifiedOpportunityResponse::recommended).reversed()
                        .thenComparing(UnifiedOpportunityResponse::saved).reversed()
                        .thenComparing(UnifiedOpportunityResponse::title))
                .toList();
    }

    public void saveOpportunity(Principal principal, OpportunityType type, String opportunityId, String title) {
        StudentProfile profile = studentService.getProfileEntity(principal);
        if (type == OpportunityType.CAREER) {
            studentService.saveCareer(principal, UUID.fromString(opportunityId));
            return;
        }
        if (!savedCareerRepository.existsByStudentIdAndOpportunityTypeAndExternalOpportunityKey(profile.getId(), type.name(), opportunityId)) {
            SavedCareer saved = new SavedCareer();
            saved.setStudentId(profile.getId());
            saved.setOpportunityType(type.name());
            saved.setExternalOpportunityKey(opportunityId);
            saved.setTitleSnapshot(title);
            savedCareerRepository.save(saved);
        }
    }

    public void unsaveOpportunity(Principal principal, OpportunityType type, String opportunityId) {
        StudentProfile profile = studentService.getProfileEntity(principal);
        if (type == OpportunityType.CAREER) {
            studentService.unsaveCareer(principal, UUID.fromString(opportunityId));
            return;
        }
        savedCareerRepository.deleteByStudentIdAndOpportunityTypeAndExternalOpportunityKey(profile.getId(), type.name(), opportunityId);
    }

    private UnifiedOpportunityResponse mapCareer(Career career, Set<String> savedKeys, StudentProfile profile) {
        return new UnifiedOpportunityResponse(
                career.getId().toString(),
                career.getTitle(),
                OpportunityType.CAREER,
                career.getIndustry(),
                career.getIndustry(),
                career.getQualificationLevel(),
                career.getLocation(),
                career.getDemandLevel(),
                savedKeys.contains(savedKey(OpportunityType.CAREER, career.getId().toString())),
                isRecommended(profile, career.getIndustry(), career.getQualificationLevel(), career.getLocation())
        );
    }

    private UnifiedOpportunityResponse mapCatalog(CatalogOpportunity item, Set<String> savedKeys, StudentProfile profile) {
        return new UnifiedOpportunityResponse(
                item.id(),
                item.title(),
                item.type(),
                item.field(),
                item.industry(),
                item.qualification(),
                item.location(),
                item.demand(),
                savedKeys.contains(savedKey(item.type(), item.id())),
                isRecommended(profile, item.field(), item.qualification(), item.location())
        );
    }

    private Set<String> savedKeys(UUID studentId) {
        return savedCareerRepository.findByStudentId(studentId).stream()
                .map(this::savedKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String savedKey(SavedCareer savedCareer) {
        if (savedCareer.getCareerId() != null) {
            return savedKey(OpportunityType.CAREER, savedCareer.getCareerId().toString());
        }
        String type = safe(savedCareer.getOpportunityType());
        if (type.isBlank() || safe(savedCareer.getExternalOpportunityKey()).isBlank()) {
            return "";
        }
        return savedKey(OpportunityType.valueOf(type), savedCareer.getExternalOpportunityKey());
    }

    private String savedKey(OpportunityType type, String id) {
        return type.name() + ":" + id;
    }

    private boolean matches(CatalogOpportunity item, String q, String field, String industry, String qualification, String location, String demand) {
        return contains(item.title(), q)
                && contains(item.field(), field)
                && contains(item.industry(), industry)
                && contains(item.qualification(), qualification)
                && contains(item.location(), location)
                && contains(item.demand(), demand);
    }

    private boolean contains(String actual, String expected) {
        String actualValue = safe(actual).toLowerCase(Locale.ROOT);
        String expectedValue = safe(expected).toLowerCase(Locale.ROOT);
        return actualValue.contains(expectedValue);
    }

    private boolean isRecommended(StudentProfile profile, String fieldOrIndustry, String qualification, String location) {
        String interests = safe(profile.getInterests()).toLowerCase(Locale.ROOT);
        String profileQualification = safe(profile.getQualificationLevel()).toLowerCase(Locale.ROOT);
        String profileLocation = safe(profile.getLocation()).toLowerCase(Locale.ROOT);
        return (!interests.isBlank() && interests.contains(safe(fieldOrIndustry).toLowerCase(Locale.ROOT)))
                || (!profileQualification.isBlank() && profileQualification.contains(safe(qualification).toLowerCase(Locale.ROOT)))
                || (!profileLocation.isBlank() && profileLocation.contains(safe(location).toLowerCase(Locale.ROOT)));
    }

    private List<CatalogOpportunity> catalog() {
        return List.of(
                new CatalogOpportunity("job-junior-web-developer", "Junior Web Developer", OpportunityType.JOB, "Software Development", "Technology", "Diploma", "Johannesburg", "High"),
                new CatalogOpportunity("job-cloud-support-associate", "Cloud Support Associate", OpportunityType.JOB, "Cloud Computing", "Technology", "Undergraduate", "Remote", "Medium")
        );
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private record CatalogOpportunity(
            String id,
            String title,
            OpportunityType type,
            String field,
            String industry,
            String qualification,
            String location,
            String demand
    ) {
    }

    private enum OpportunityTypeFilter {
        ALL,
        CAREER,
        JOB;

        static OpportunityTypeFilter from(String value) {
            if (value == null || value.isBlank() || "ALL".equalsIgnoreCase(value)) {
                return ALL;
            }
            return OpportunityTypeFilter.valueOf(value.trim().toUpperCase(Locale.ROOT));
        }

        boolean includes(OpportunityType type) {
            return this == ALL || this.name().equals(type.name());
        }
    }
}

