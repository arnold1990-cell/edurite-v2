package com.edurite.district.service;

import com.edurite.common.exception.ResourceConflictException;
import com.edurite.district.entity.SchoolCircuitAssignment;
import com.edurite.district.dto.DistrictDtos;
import com.edurite.district.repository.SchoolCircuitAssignmentRepository;
import com.edurite.notification.service.NotificationService;
import com.edurite.school.portal.entity.School;
import com.edurite.school.portal.entity.SchoolRegistrationRequest;
import com.edurite.school.portal.entity.SchoolStatus;
import com.edurite.school.portal.entity.SchoolUserProfile;
import com.edurite.school.portal.repository.SchoolRegistrationRequestRepository;
import com.edurite.school.portal.repository.SchoolRepository;
import com.edurite.school.portal.repository.SchoolUserProfileRepository;
import com.edurite.school.service.SchoolAccessService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DistrictSchoolRegistrationService {
    private static final Logger log = LoggerFactory.getLogger(DistrictSchoolRegistrationService.class);

    private final SchoolRegistrationRequestRepository schoolRegistrationRequestRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolUserProfileRepository schoolUserProfileRepository;
    private final SchoolCircuitAssignmentRepository schoolCircuitAssignmentRepository;
    private final NotificationService notificationService;

    public DistrictSchoolRegistrationService(
            SchoolRegistrationRequestRepository schoolRegistrationRequestRepository,
            SchoolRepository schoolRepository,
            SchoolUserProfileRepository schoolUserProfileRepository,
            SchoolCircuitAssignmentRepository schoolCircuitAssignmentRepository,
            NotificationService notificationService
    ) {
        this.schoolRegistrationRequestRepository = schoolRegistrationRequestRepository;
        this.schoolRepository = schoolRepository;
        this.schoolUserProfileRepository = schoolUserProfileRepository;
        this.schoolCircuitAssignmentRepository = schoolCircuitAssignmentRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public DistrictDtos.SchoolRegistrationRequestsResponse requests(UUID districtId, String status, String search) {
        String searchText = sanitizeSearchText(search);
        SchoolStatus statusFilter = parseStatusFilter(status);
        log.info(
                "Loading district school registration requests: districtId={}, status={}, searchValue={}, searchType={}, sanitizedSearchValue={}",
                districtId,
                status,
                search,
                search == null ? "null" : search.getClass().getName(),
                searchText
        );
        List<SchoolRegistrationRequest> allItems = loadRequests(districtId, null, null);
        List<SchoolRegistrationRequest> items = loadRequests(districtId, statusFilter, searchText);
        long pending = allItems.stream().filter(item -> item.getStatus() == SchoolStatus.PENDING_DISTRICT_APPROVAL).count();
        long approved = allItems.stream().filter(item -> item.getStatus() == SchoolStatus.ACTIVE).count();
        long rejected = allItems.stream().filter(item -> item.getStatus() == SchoolStatus.REJECTED).count();
        return new DistrictDtos.SchoolRegistrationRequestsResponse(
                List.of(
                        new DistrictDtos.MetricCardDto("Pending requests", String.valueOf(pending), "Awaiting district approval", pending > 0 ? "warning" : "positive"),
                        new DistrictDtos.MetricCardDto("Approved schools", String.valueOf(approved), "Self-registered schools onboarded", "positive"),
                        new DistrictDtos.MetricCardDto("Rejected requests", String.valueOf(rejected), "Requests requiring resubmission", rejected > 0 ? "warning" : "neutral")
                ),
                items.stream().map(this::toDto).toList(),
                items.size()
        );
    }

    @Transactional
    public DistrictDtos.SchoolRegistrationRequestItemDto decide(UUID districtId, UUID requestId, UUID actorUserId, DistrictDtos.SchoolRegistrationDecisionRequest request) {
        String decision = normalize(request.decision());
        if (!"approve".equals(decision) && !"reject".equals(decision)) {
            throw new ResourceConflictException("Decision must be APPROVE or REJECT.");
        }
        if ("approve".equals(decision)) {
            return approve(districtId, requestId, actorUserId);
        }
        return reject(districtId, requestId, actorUserId, request.rejectionReason());
    }

    @Transactional
    public DistrictDtos.SchoolRegistrationRequestItemDto approve(UUID districtId, UUID requestId, UUID actorUserId) {
        SchoolRegistrationRequest registration = requireScopedRequest(districtId, requestId);
        approve(registration, actorUserId);
        return toDto(registration);
    }

    @Transactional
    public DistrictDtos.SchoolRegistrationRequestItemDto reject(UUID districtId, UUID requestId, UUID actorUserId, String rejectionReason) {
        SchoolRegistrationRequest registration = requireScopedRequest(districtId, requestId);
        reject(registration, rejectionReason);
        return toDto(registration);
    }

    private SchoolRegistrationRequest requireScopedRequest(UUID districtId, UUID requestId) {
        SchoolRegistrationRequest registration = schoolRegistrationRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceConflictException("School registration request not found."));
        if (!districtId.equals(registration.getDistrictId())) {
            throw new ResourceConflictException("School registration request is outside your district.");
        }
        return registration;
    }

    private void approve(SchoolRegistrationRequest registration, UUID actorUserId) {
        if (registration.getStatus() == SchoolStatus.ACTIVE) {
            return;
        }
        if (schoolRepository.existsByRegistrationNumberIgnoreCase(registration.getEmisNumber())) {
            throw new ResourceConflictException("A school with this EMIS number already exists.");
        }

        School school = new School();
        school.setSchoolName(registration.getSchoolName());
        school.setRegistrationNumber(registration.getEmisNumber());
        school.setSchoolCode(registration.getEmisNumber());
        school.setStatus("ACTIVE");
        school.setDistrictId(registration.getDistrictId());
        school.setDistrict(registration.getDistrictName());
        school.setProvince(registration.getProvince());
        school.setContactEmail(registration.getSchoolEmail());
        school.setContactPhone(registration.getPhoneNumber());
        school.setAddress(registration.getPhysicalAddress());
        school = schoolRepository.save(school);

        SchoolUserProfile profile = schoolUserProfileRepository.findByUserIdAndDeletedFalse(registration.getUserId())
                .orElseGet(SchoolUserProfile::new);
        profile.setSchoolId(school.getId());
        profile.setUserId(registration.getUserId());
        profile.setRoleName(SchoolAccessService.ROLE_SCHOOL_ADMIN);
        profile.setPortalUsername(registration.getEmisNumber().toLowerCase(Locale.ROOT));
        profile.setEmployeeOrStudentNo(registration.getEmisNumber());
        profile.setActive(true);
        profile.setDeleted(false);
        schoolUserProfileRepository.save(profile);

        if (registration.getCircuitId() != null) {
            List<SchoolCircuitAssignment> existingAssignments = schoolCircuitAssignmentRepository.findBySchoolId(school.getId());
            if (!existingAssignments.isEmpty()) {
                schoolCircuitAssignmentRepository.deleteAll(existingAssignments);
            }
            SchoolCircuitAssignment assignment = new SchoolCircuitAssignment();
            assignment.setSchoolId(school.getId());
            assignment.setCircuitId(registration.getCircuitId());
            assignment.setAssignedAt(OffsetDateTime.now());
            assignment.setAssignedBy(actorUserId);
            schoolCircuitAssignmentRepository.save(assignment);
        }

        registration.setSchoolId(school.getId());
        registration.setStatus(SchoolStatus.ACTIVE);
        registration.setRejectionReason(null);
        registration.setApprovedAt(OffsetDateTime.now());
        registration.setRejectedAt(null);
        schoolRegistrationRequestRepository.save(registration);

        notificationService.createInApp(
                registration.getUserId(),
                "SCHOOL_REGISTRATION_APPROVED",
                "School registration approved",
                registration.getSchoolName() + " has been approved and now has full School Admin Portal access."
        );
    }

    private void reject(SchoolRegistrationRequest registration, String rejectionReason) {
        if (normalize(rejectionReason) == null) {
            throw new ResourceConflictException("Rejection reason is required.");
        }
        registration.setStatus(SchoolStatus.REJECTED);
        registration.setRejectionReason(rejectionReason.trim());
        registration.setRejectedAt(OffsetDateTime.now());
        registration.setApprovedAt(null);
        schoolRegistrationRequestRepository.save(registration);

        notificationService.createInApp(
                registration.getUserId(),
                "SCHOOL_REGISTRATION_REJECTED",
                "School registration rejected",
                "Your district registration request was rejected: " + registration.getRejectionReason()
        );
    }

    private SchoolStatus parseStatusFilter(String status) {
        String normalized = normalize(status);
        if (normalized == null) {
            return null;
        }
        return switch (normalized) {
            case "pending", "pending_district_approval" -> SchoolStatus.PENDING_DISTRICT_APPROVAL;
            case "approved", "active" -> SchoolStatus.ACTIVE;
            case "rejected" -> SchoolStatus.REJECTED;
            case "suspended" -> SchoolStatus.SUSPENDED;
            default -> throw new ResourceConflictException("Unsupported school registration status filter.");
        };
    }

    private DistrictDtos.SchoolRegistrationRequestItemDto toDto(SchoolRegistrationRequest request) {
        return new DistrictDtos.SchoolRegistrationRequestItemDto(
                request.getId(),
                request.getUserId(),
                request.getSchoolId(),
                request.getSchoolName(),
                request.getEmisNumber(),
                request.getProvince(),
                request.getDistrictName(),
                request.getCircuit(),
                request.getSchoolType(),
                request.getPrincipalName(),
                request.getPrincipalEmail(),
                request.getSchoolEmail(),
                request.getPhoneNumber(),
                request.getPhysicalAddress(),
                request.getStatus().name(),
                request.getRejectionReason(),
                request.getSubmittedAt(),
                request.getApprovedAt(),
                request.getRejectedAt()
        );
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String sanitizeSearchText(String search) {
        if (search == null) {
            return null;
        }
        String trimmed = search.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<SchoolRegistrationRequest> loadRequests(UUID districtId, SchoolStatus statusFilter, String searchText) {
        if (searchText == null) {
            if (statusFilter == null) {
                return schoolRegistrationRequestRepository.findByDistrictIdOrderBySubmittedAtDesc(districtId);
            }
            return schoolRegistrationRequestRepository.findByDistrictIdAndStatusOrderBySubmittedAtDesc(districtId, statusFilter);
        }
        if (statusFilter == null) {
            return schoolRegistrationRequestRepository.searchByDistrict(districtId, searchText);
        }
        return schoolRegistrationRequestRepository.searchByDistrictAndStatus(districtId, statusFilter, searchText);
    }
}
