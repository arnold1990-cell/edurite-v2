package com.edurite.admin.service;

import com.edurite.account.service.AccountService;
import com.edurite.admin.dto.AdminAnalyticsDto;
import com.edurite.admin.dto.AdminApplicationsPerBursaryDto;
import com.edurite.admin.dto.AdminBulkUploadResultDto;
import com.edurite.admin.dto.AdminBulkUploadRowErrorDto;
import com.edurite.admin.dto.AdminBursaryDto;
import com.edurite.admin.dto.AdminCompanyDto;
import com.edurite.admin.dto.AdminDistrictDtos;
import com.edurite.admin.dto.AdminMonthlyMetricDto;
import com.edurite.admin.dto.AdminPlatformSettingsDto;
import com.edurite.admin.dto.AdminPlatformSettingsUpdateRequest;
import com.edurite.admin.dto.AdminRecentBursaryDto;
import com.edurite.admin.dto.AdminRecentCompanyDto;
import com.edurite.admin.dto.AdminRecentUserDto;
import com.edurite.admin.dto.AdminStatusCountDto;
import com.edurite.admin.dto.AdminUserDto;
import com.edurite.admin.entity.AuditLog;
import com.edurite.admin.entity.RolePermission;
import com.edurite.admin.repository.AuditLogRepository;
import com.edurite.admin.repository.RolePermissionRepository;
import com.edurite.application.entity.ApplicationRecord;
import com.edurite.application.repository.ApplicationRepository;
import com.edurite.bursary.entity.Bursary;
import com.edurite.bursary.repository.BursaryRepository;
import com.edurite.common.exception.ResourceConflictException;
import com.edurite.company.entity.CompanyApprovalStatus;
import com.edurite.company.entity.CompanyProfile;
import com.edurite.company.repository.CompanyProfileRepository;
import com.edurite.district.entity.District;
import com.edurite.district.entity.DistrictAdminProfile;
import com.edurite.district.repository.DistrictAdminProfileRepository;
import com.edurite.district.repository.DistrictRepository;
import com.edurite.security.service.CurrentUserService;
import com.edurite.school.portal.entity.SchoolStatus;
import com.edurite.school.portal.repository.SchoolRegistrationRequestRepository;
import com.edurite.school.portal.repository.SchoolRepository;
import com.edurite.user.entity.Role;
import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import com.edurite.user.repository.RoleRepository;
import com.edurite.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AdminService {

    private static final List<String> DEFAULT_ROLE_PERMISSIONS = List.of(
            "ADMIN_DASHBOARD_VIEW", "USER_MANAGE", "ROLE_MANAGE", "BURSARY_REVIEW", "SUBSCRIPTION_VIEW", "ANALYTICS_VIEW"
    );
    private static final List<String> REQUIRED_BULK_UPLOAD_HEADERS = List.of("email", "firstName", "lastName", "role", "password");
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final List<String> ROLE_PRIORITY = List.of(
            "ROLE_ADMIN",
            "ROLE_DISTRICT_ADMIN",
            "ROLE_DISTRICT_DIRECTOR",
            "ROLE_CIRCUIT_MANAGER",
            "ROLE_SUBJECT_ADVISOR",
            "ROLE_SCHOOL_ADMIN",
            "ROLE_TEACHER",
            "ROLE_COMPANY",
            "ROLE_SCHOOL_STUDENT",
            "ROLE_STUDENT"
    );

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final AuditLogRepository auditLogRepository;
    private final BursaryRepository bursaryRepository;
    private final ApplicationRepository applicationRepository;
    private final CompanyProfileRepository companyProfileRepository;
    private final DistrictRepository districtRepository;
    private final DistrictAdminProfileRepository districtAdminProfileRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolRegistrationRequestRepository schoolRegistrationRequestRepository;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final PlatformSettingsService platformSettingsService;
    private final AccountService accountService;

    public AdminService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RolePermissionRepository rolePermissionRepository,
            AuditLogRepository auditLogRepository,
            BursaryRepository bursaryRepository,
            ApplicationRepository applicationRepository,
            CompanyProfileRepository companyProfileRepository,
            DistrictRepository districtRepository,
            DistrictAdminProfileRepository districtAdminProfileRepository,
            SchoolRepository schoolRepository,
            SchoolRegistrationRequestRepository schoolRegistrationRequestRepository,
            CurrentUserService currentUserService,
            PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper,
            PlatformSettingsService platformSettingsService,
            AccountService accountService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.auditLogRepository = auditLogRepository;
        this.bursaryRepository = bursaryRepository;
        this.applicationRepository = applicationRepository;
        this.companyProfileRepository = companyProfileRepository;
        this.districtRepository = districtRepository;
        this.districtAdminProfileRepository = districtAdminProfileRepository;
        this.schoolRepository = schoolRepository;
        this.schoolRegistrationRequestRepository = schoolRegistrationRequestRepository;
        this.currentUserService = currentUserService;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.platformSettingsService = platformSettingsService;
        this.accountService = accountService;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimToNull(String value) {
        String trimmed = safe(value);
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeRoleName(String value) {
        if (value == null || value.isBlank()) {
            throw new ResourceConflictException("Role name is required");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }

    private boolean hasRole(User user, String roleName) {
        String normalized = normalizeRoleName(roleName);
        return user.getRoles().stream().map(Role::getName).map(this::normalizeRoleName).anyMatch(normalized::equals);
    }

    private boolean isLiveUser(User user) {
        return user.getDeletedAt() == null && user.getStatus() != UserStatus.DELETED;
    }

    private String generateUniqueDistrictUsername(String districtName) {
        String normalizedBase = safe(districtName)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", ".")
                .replaceAll("^\\.+|\\.+$", "");
        String base = (normalizedBase.isBlank() ? "district" : normalizedBase) + ".admin";
        String candidate = base;
        int suffix = 2;
        while (userRepository.existsByUsernameIgnoreCase(candidate)) {
            candidate = base + suffix;
            suffix += 1;
        }
        return candidate;
    }

    @Transactional(readOnly = true)
    public List<AdminUserDto> users(String search, String status, String accountType, String companyStatus, boolean includeDeleted) {
        String normalizedSearch = safe(search).toLowerCase(Locale.ROOT);
        String normalizedStatus = safe(status).toUpperCase(Locale.ROOT);
        String normalizedRole = safe(accountType).toUpperCase(Locale.ROOT);
        CompanyApprovalStatus normalizedCompanyStatus = parseCompanyStatus(companyStatus);
        Map<UUID, CompanyProfile> companiesByUserId = companyProfileRepository.findAll().stream()
                .filter(company -> includeDeleted || company.getDeletedAt() == null)
                .collect(Collectors.toMap(CompanyProfile::getUserId, company -> company, (left, right) -> left));

        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(user -> includeDeleted || isLiveUser(user))
                .filter(user -> normalizedSearch.isBlank() || matchesUserSearch(user, normalizedSearch))
                .filter(user -> normalizedStatus.isBlank() || user.getStatus().name().equalsIgnoreCase(normalizedStatus))
                .filter(user -> normalizedRole.isBlank() || hasRole(user, normalizeRoleName(normalizedRole)))
                .filter(user -> {
                    if (normalizedCompanyStatus == null) {
                        return true;
                    }
                    CompanyProfile company = companiesByUserId.get(user.getId());
                    return company != null && company.getStatus() == normalizedCompanyStatus;
                })
                .map(user -> toUserDto(user, companiesByUserId.get(user.getId())))
                .toList();
    }

    @Transactional
    public AdminUserDto updateUserStatus(UUID userId, boolean active, Principal principal) {
        User actor = currentUserService.requireUser(principal);
        User target = userRepository.findById(userId).orElseThrow(() -> new ResourceConflictException("User not found"));
        ensureCanModifyUser(actor, target, active ? "unsuspend" : "suspend");

        target.setStatus(active ? UserStatus.ACTIVE : UserStatus.SUSPENDED);
        User saved = userRepository.save(target);
        writeAudit(principal, "ADMIN_USER_STATUS_UPDATED", "USER", saved.getId(), Map.of("active", active));
        CompanyProfile company = companyProfileRepository.findByUserId(saved.getId()).orElse(null);
        return toUserDto(saved, company);
    }

    @Transactional
    public Map<String, String> deleteUser(UUID userId, String reason, Principal principal) {
        User actor = currentUserService.requireUser(principal);
        User target = userRepository.findById(userId).orElseThrow(() -> new ResourceConflictException("User not found"));
        ensureCanModifyUser(actor, target, "delete");
        Map<String, String> result = accountService.deleteAccountByAdmin(principal, userId, reason);
        writeAudit(principal, "ADMIN_USER_DELETED", "USER", userId, Map.of("reason", safe(reason)));
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> roles() {
        ensureDefaultPermissions();
        return roleRepository.findAll().stream().map(this::toRoleMap).toList();
    }

    @Transactional
    public Map<String, Object> createRole(Map<String, Object> payload, Principal principal) {
        String name = normalizeRoleName((String) payload.get("name"));
        if (roleRepository.findByName(name).isPresent()) {
            throw new ResourceConflictException("Role already exists");
        }
        Role role = new Role();
        role.setName(name);
        Role savedRole = roleRepository.save(role);
        syncPermissions(savedRole, extractPermissions(payload));
        writeAudit(principal, "ADMIN_ROLE_CREATED", "ROLE", savedRole.getId(), payload);
        return toRoleMap(savedRole);
    }

    @Transactional
    public Map<String, Object> updateRole(UUID roleId, Map<String, Object> payload, Principal principal) {
        Role role = roleRepository.findById(roleId).orElseThrow(() -> new ResourceConflictException("Role not found"));
        if (payload.get("name") instanceof String name && !name.isBlank()) {
            role.setName(normalizeRoleName(name));
        }
        Role savedRole = roleRepository.save(role);
        syncPermissions(savedRole, extractPermissions(payload));
        writeAudit(principal, "ADMIN_ROLE_UPDATED", "ROLE", savedRole.getId(), payload);
        return toRoleMap(savedRole);
    }

    @Transactional
    public Map<String, String> deleteRole(UUID roleId, Principal principal) {
        Role role = roleRepository.findById(roleId).orElseThrow(() -> new ResourceConflictException("Role not found"));
        if (Set.of(
                "ROLE_STUDENT",
                "ROLE_COMPANY",
                "ROLE_ADMIN",
                "ROLE_DISTRICT_ADMIN",
                "ROLE_DISTRICT_DIRECTOR",
                "ROLE_CIRCUIT_MANAGER",
                "ROLE_SUBJECT_ADVISOR",
                "ROLE_SCHOOL_ADMIN",
                "ROLE_TEACHER",
                "ROLE_SCHOOL_STUDENT"
        ).contains(role.getName())) {
            throw new ResourceConflictException("Built-in roles cannot be deleted");
        }
        rolePermissionRepository.deleteByRoleId(roleId);
        roleRepository.delete(role);
        writeAudit(principal, "ADMIN_ROLE_DELETED", "ROLE", roleId, Map.of("name", role.getName()));
        return Map.of("message", "Role deleted");
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> auditLogs() {
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc().stream().map(log -> Map.<String, Object>of(
                "id", log.getId(),
                "actorId", log.getActorId(),
                "action", log.getAction(),
                "entityType", safe(log.getEntityType()),
                "entityId", log.getEntityId(),
                "details", log.getDetails(),
                "createdAt", log.getCreatedAt()
        )).toList();
    }

    @Transactional(readOnly = true)
    public List<AdminCompanyDto> companies(String search, String status, boolean includeDeleted) {
        CompanyApprovalStatus parsedStatus = parseCompanyStatus(status);
        return companyProfileRepository.searchForAdmin(safe(search), parsedStatus, includeDeleted).stream()
                .map(this::toCompanyDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminCompanyDto> pendingCompanies() {
        return companies("", "PENDING", false);
    }

    @Transactional(readOnly = true)
    public AdminCompanyDto companyById(UUID companyId) {
        CompanyProfile company = companyProfileRepository.findById(companyId).orElseThrow(() -> new ResourceConflictException("Company not found"));
        return toCompanyDto(company);
    }

    @Transactional
    public AdminCompanyDto approveCompany(UUID companyId, String notes, Principal principal) {
        return changeCompanyStatus(companyId, CompanyApprovalStatus.APPROVED, notes, principal, "ADMIN_COMPANY_APPROVED");
    }

    @Transactional
    public AdminCompanyDto rejectCompany(UUID companyId, String notes, Principal principal) {
        return changeCompanyStatus(companyId, CompanyApprovalStatus.REJECTED, notes, principal, "ADMIN_COMPANY_REJECTED");
    }

    @Transactional
    public AdminCompanyDto requestCompanyMoreInfo(UUID companyId, String notes, Principal principal) {
        return changeCompanyStatus(companyId, CompanyApprovalStatus.MORE_INFO_REQUIRED, notes, principal, "ADMIN_COMPANY_MORE_INFO_REQUESTED");
    }

    @Transactional
    public AdminCompanyDto suspendCompany(UUID companyId, String notes, Principal principal) {
        return changeCompanyStatus(companyId, CompanyApprovalStatus.SUSPENDED, notes, principal, "ADMIN_COMPANY_SUSPENDED");
    }

    @Transactional
    public AdminCompanyDto reactivateCompany(UUID companyId, String notes, Principal principal) {
        CompanyProfile company = companyProfileRepository.findById(companyId).orElseThrow(() -> new ResourceConflictException("Company not found"));
        if (company.getDeletedAt() != null) {
            throw new ResourceConflictException("Company has been deleted");
        }
        if (company.getStatus() != CompanyApprovalStatus.SUSPENDED && company.getStatus() != CompanyApprovalStatus.REJECTED) {
            throw new ResourceConflictException("Only suspended or rejected companies can be reactivated");
        }
        company.setStatus(CompanyApprovalStatus.APPROVED);
        company.setReviewedAt(OffsetDateTime.now());
        company.setReviewedBy(currentUserService.requireUser(principal).getId());
        company.setReviewNotes(trimToNull(notes));
        CompanyProfile saved = companyProfileRepository.save(company);
        writeAudit(principal, "ADMIN_COMPANY_REACTIVATED", "COMPANY", saved.getId(), Map.of("notes", safe(notes)));
        return toCompanyDto(saved);
    }

    @Transactional
    public AdminCompanyDto deleteCompany(UUID companyId, String reason, Principal principal) {
        CompanyProfile company = companyProfileRepository.findById(companyId).orElseThrow(() -> new ResourceConflictException("Company not found"));
        if (company.getDeletedAt() != null) {
            throw new ResourceConflictException("Company is already deleted");
        }
        User actor = currentUserService.requireUser(principal);
        company.setDeletedAt(OffsetDateTime.now());
        company.setDeletedBy(actor.getId());
        company.setDeletionReason(trimToNull(reason));
        company.setStatus(CompanyApprovalStatus.SUSPENDED);
        company.setReviewedAt(OffsetDateTime.now());
        company.setReviewedBy(actor.getId());
        company.setReviewNotes("Deleted by admin");
        CompanyProfile saved = companyProfileRepository.save(company);
        accountService.deleteAccountByAdmin(principal, saved.getUserId(), reason == null ? "Company deleted by admin" : reason);
        writeAudit(principal, "ADMIN_COMPANY_DELETED", "COMPANY", saved.getId(), Map.of("reason", safe(reason)));
        return toCompanyDto(saved);
    }

    @Transactional(readOnly = true)
    public List<AdminBursaryDto> bursaries(String status, UUID companyId, LocalDate fromDate, LocalDate toDate, boolean includeDeleted) {
        OffsetDateTime from = fromDate == null ? null : fromDate.atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        OffsetDateTime to = toDate == null ? null : toDate.plusDays(1).atStartOfDay().minusSeconds(1).atOffset(OffsetDateTime.now().getOffset());
        Map<UUID, CompanyProfile> companiesById = companyProfileRepository.findAll().stream()
                .collect(Collectors.toMap(CompanyProfile::getId, company -> company, (left, right) -> left));
        return bursaryRepository.searchForAdmin(safe(status), companyId, from, to, includeDeleted).stream()
                .map(bursary -> toBursaryDto(bursary, companiesById.get(bursary.getCompanyId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminBursaryDto> pendingBursaries() {
        return bursaries("PENDING_APPROVAL", null, null, null, false);
    }

    @Transactional
    public AdminBursaryDto reviewBursary(UUID bursaryId, String decision, String comment, Principal principal) {
        Bursary bursary = bursaryRepository.findById(bursaryId).orElseThrow(() -> new ResourceConflictException("Bursary not found"));
        ensureBursaryNotDeleted(bursary);
        String normalized = decision == null ? "" : decision.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("APPROVED", "REJECTED", "REQUEST_CHANGES").contains(normalized)) {
            throw new ResourceConflictException("Invalid bursary review decision");
        }
        bursary.setStatus(switch (normalized) {
            case "APPROVED" -> "ACTIVE";
            case "REJECTED" -> "REJECTED";
            default -> "PENDING_APPROVAL";
        });
        Bursary saved = bursaryRepository.save(bursary);
        writeAudit(principal, "ADMIN_BURSARY_REVIEWED", "BURSARY", saved.getId(), Map.of("decision", normalized, "comment", safe(comment)));
        return toBursaryDto(saved, companyProfileRepository.findById(saved.getCompanyId()).orElse(null));
    }

    @Transactional
    public AdminBursaryDto suspendBursary(UUID bursaryId, String reason, Principal principal) {
        Bursary bursary = bursaryRepository.findById(bursaryId).orElseThrow(() -> new ResourceConflictException("Bursary not found"));
        ensureBursaryNotDeleted(bursary);
        bursary.setStatus("SUSPENDED");
        Bursary saved = bursaryRepository.save(bursary);
        writeAudit(principal, "ADMIN_BURSARY_SUSPENDED", "BURSARY", saved.getId(), Map.of("reason", safe(reason)));
        return toBursaryDto(saved, companyProfileRepository.findById(saved.getCompanyId()).orElse(null));
    }

    @Transactional
    public AdminBursaryDto reactivateBursary(UUID bursaryId, String reason, Principal principal) {
        Bursary bursary = bursaryRepository.findById(bursaryId).orElseThrow(() -> new ResourceConflictException("Bursary not found"));
        ensureBursaryNotDeleted(bursary);
        if (!"SUSPENDED".equalsIgnoreCase(bursary.getStatus()) && !"REJECTED".equalsIgnoreCase(bursary.getStatus())) {
            throw new ResourceConflictException("Only suspended or rejected bursaries can be reactivated");
        }
        bursary.setStatus("ACTIVE");
        Bursary saved = bursaryRepository.save(bursary);
        writeAudit(principal, "ADMIN_BURSARY_REACTIVATED", "BURSARY", saved.getId(), Map.of("reason", safe(reason)));
        return toBursaryDto(saved, companyProfileRepository.findById(saved.getCompanyId()).orElse(null));
    }

    @Transactional
    public AdminBursaryDto deleteBursary(UUID bursaryId, String reason, Principal principal) {
        Bursary bursary = bursaryRepository.findById(bursaryId).orElseThrow(() -> new ResourceConflictException("Bursary not found"));
        ensureBursaryNotDeleted(bursary);
        bursary.setDeletedAt(OffsetDateTime.now());
        bursary.setDeletedBy(currentUserService.requireUser(principal).getId());
        bursary.setDeletionReason(trimToNull(reason));
        bursary.setStatus("DELETED");
        Bursary saved = bursaryRepository.save(bursary);
        writeAudit(principal, "ADMIN_BURSARY_DELETED", "BURSARY", saved.getId(), Map.of("reason", safe(reason)));
        return toBursaryDto(saved, companyProfileRepository.findById(saved.getCompanyId()).orElse(null));
    }

    @Transactional(readOnly = true)
    public AdminAnalyticsDto analytics() {
        List<User> users = userRepository.findAll();
        List<User> activeUsers = users.stream().filter(this::isLiveUser).toList();
        List<CompanyProfile> companies = companyProfileRepository.findAll().stream().filter(company -> company.getDeletedAt() == null).toList();
        List<Bursary> bursaries = bursaryRepository.findAll().stream().filter(bursary -> bursary.getDeletedAt() == null).toList();
        List<ApplicationRecord> applications = applicationRepository.findAll();

        long totalStudents = activeUsers.stream().filter(user -> hasRole(user, "ROLE_STUDENT")).count();
        long totalAdmins = activeUsers.stream().filter(user -> hasRole(user, "ROLE_ADMIN")).count();
        long totalCompanies = companies.size();
        long pendingCompanies = companies.stream().filter(company -> company.getStatus() == CompanyApprovalStatus.PENDING).count();
        long approvedCompanies = companies.stream().filter(company -> company.getStatus() == CompanyApprovalStatus.APPROVED).count();
        long suspendedCompanies = companies.stream().filter(company -> company.getStatus() == CompanyApprovalStatus.SUSPENDED).count();

        long activeBursaries = bursaries.stream().filter(bursary -> "ACTIVE".equalsIgnoreCase(bursary.getStatus())).count();
        long suspendedBursaries = bursaries.stream().filter(bursary -> "SUSPENDED".equalsIgnoreCase(bursary.getStatus())).count();
        long closedOrExpiredBursaries = bursaries.stream().filter(this::isClosedOrExpired).count();

        Map<UUID, Long> applicationCountByBursaryId = applications.stream()
                .collect(Collectors.groupingBy(ApplicationRecord::getBursaryId, Collectors.counting()));
        List<AdminApplicationsPerBursaryDto> applicationsPerBursary = bursaries.stream()
                .map(bursary -> new AdminApplicationsPerBursaryDto(bursary.getId(), bursary.getTitle(), applicationCountByBursaryId.getOrDefault(bursary.getId(), 0L)))
                .sorted(Comparator.comparingLong(AdminApplicationsPerBursaryDto::totalApplications).reversed())
                .limit(10)
                .toList();

        List<AdminRecentUserDto> recentUsers = activeUsers.stream()
                .sorted(Comparator.comparing(User::getCreatedAt).reversed())
                .limit(10)
                .map(user -> new AdminRecentUserDto(
                        user.getId(),
                        ("%s %s".formatted(safe(user.getFirstName()), safe(user.getLastName()))).trim(),
                        user.getEmail(),
                        user.getRoles().stream().map(Role::getName).sorted().toList(),
                        user.getCreatedAt()
                ))
                .toList();
        List<AdminRecentCompanyDto> recentCompanies = companies.stream()
                .sorted(Comparator.comparing(CompanyProfile::getCreatedAt).reversed())
                .limit(10)
                .map(company -> new AdminRecentCompanyDto(company.getId(), company.getCompanyName(), company.getOfficialEmail(), company.getStatus().name(), company.getCreatedAt()))
                .toList();
        List<AdminRecentBursaryDto> recentBursaries = bursaries.stream()
                .sorted(Comparator.comparing(Bursary::getCreatedAt).reversed())
                .limit(10)
                .map(bursary -> new AdminRecentBursaryDto(bursary.getId(), bursary.getTitle(), bursary.getCompanyId(), safe(bursary.getStatus()), bursary.getApplicationEndDate(), bursary.getCreatedAt()))
                .toList();

        List<AdminStatusCountDto> bursariesByStatus = bursaries.stream()
                .collect(Collectors.groupingBy(bursary -> safe(bursary.getStatus()).toUpperCase(Locale.ROOT), Collectors.counting()))
                .entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(entry -> new AdminStatusCountDto(entry.getKey(), entry.getValue()))
                .toList();

        return new AdminAnalyticsDto(
                activeUsers.size(),
                totalStudents,
                totalCompanies,
                totalAdmins,
                pendingCompanies,
                approvedCompanies,
                suspendedCompanies,
                bursaries.size(),
                activeBursaries,
                suspendedBursaries,
                closedOrExpiredBursaries,
                applications.size(),
                applicationsPerBursary,
                recentUsers,
                recentCompanies,
                recentBursaries,
                toMonthlyMetrics(activeUsers.stream().map(User::getCreatedAt).toList()),
                toMonthlyMetrics(applications.stream().map(ApplicationRecord::getCreatedAt).toList()),
                toMonthlyMetrics(bursaries.stream().map(Bursary::getCreatedAt).toList()),
                bursariesByStatus
        );
    }

    @Transactional(readOnly = true)
    public AdminDistrictDtos.AdminDistrictManagementResponse districts() {
        List<District> districts = districtRepository.findAll().stream()
                .sorted(Comparator.comparing(District::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        long activeDistricts = districts.stream().filter(District::isActive).count();
        long pendingSchoolRegistrations = schoolRegistrationRequestRepository.findAll().stream()
                .filter(item -> item.getStatus() == SchoolStatus.PENDING_DISTRICT_APPROVAL)
                .count();
        long totalSchools = schoolRepository.findAll().size();

        return new AdminDistrictDtos.AdminDistrictManagementResponse(
                List.of(
                        new AdminDistrictDtos.AdminDistrictMetricDto("Total Districts", String.valueOf(districts.size()), "All districts created by EduRite Admin"),
                        new AdminDistrictDtos.AdminDistrictMetricDto("Active Districts", String.valueOf(activeDistricts), "Districts currently enabled for school onboarding"),
                        new AdminDistrictDtos.AdminDistrictMetricDto("Pending School Registrations", String.valueOf(pendingSchoolRegistrations), "School requests awaiting district review"),
                        new AdminDistrictDtos.AdminDistrictMetricDto("Total Schools", String.valueOf(totalSchools), "Schools linked across all districts")
                ),
                districts.stream().map(this::toDistrictDto).toList()
        );
    }

    @Transactional
    public AdminDistrictDtos.AdminDistrictItemDto createDistrict(AdminDistrictDtos.AdminCreateDistrictRequest request, Principal principal) {
        String districtName = safe(request.districtName());
        String districtCode = safe(request.districtCode()).toUpperCase(Locale.ROOT);
        String directorName = safe(request.directorName());
        String adminName = safe(request.adminName());
        String adminEmail = safe(request.adminEmail()).toLowerCase(Locale.ROOT);
        String phoneNumber = safe(request.phoneNumber());
        String address = safe(request.physicalAddress());
        String status = safe(request.status()).toUpperCase(Locale.ROOT);

        if (districtName.isBlank() || districtCode.isBlank() || directorName.isBlank() || adminName.isBlank()
                || adminEmail.isBlank() || phoneNumber.isBlank() || address.isBlank()) {
            throw new ResourceConflictException("All district fields are required.");
        }
        if (!Set.of("ACTIVE", "INACTIVE").contains(status)) {
            throw new ResourceConflictException("District status must be Active or Inactive.");
        }
        if (districtRepository.findByDistrictCodeIgnoreCase(districtCode).isPresent()) {
            throw new ResourceConflictException("District code already exists.");
        }
        if (userRepository.existsByEmailIgnoreCase(adminEmail)) {
            throw new ResourceConflictException("District admin email already exists.");
        }

        District district = new District();
        district.setDistrictName(districtName);
        district.setDistrictCode(districtCode);
        district.setDirectorName(directorName);
        district.setAdminName(adminName);
        district.setAdminEmail(adminEmail);
        district.setPhoneNumber(phoneNumber);
        district.setAddress(address);
        district.setStatus(status);
        district.setActive("ACTIVE".equals(status));
        district.setLicensingStatus(status);
        district.setContactEmail(adminEmail);
        district.setContactPhone(phoneNumber);
        district = districtRepository.save(district);
        AdminCredentials credentials = createDistrictAdminAccount(district);

        writeAudit(principal, "ADMIN_DISTRICT_CREATED", "DISTRICT", district.getId(), Map.of(
                "districtCode", districtCode,
                "districtName", districtName,
                "username", credentials.username(),
                "status", status
        ));

        return toDistrictDto(district, credentials.username(), credentials.temporaryPassword());
    }

    @Transactional
    public AdminDistrictDtos.AdminDistrictItemDto updateDistrict(UUID districtId, AdminDistrictDtos.AdminUpdateDistrictRequest request, Principal principal) {
        District district = districtRepository.findById(districtId)
                .orElseThrow(() -> new ResourceConflictException("District not found."));
        String directorName = safe(request.directorName());
        String adminName = safe(request.adminName());
        String adminEmail = safe(request.adminEmail()).toLowerCase(Locale.ROOT);
        String phoneNumber = safe(request.phoneNumber());
        String status = safe(request.status()).toUpperCase(Locale.ROOT);
        if (directorName.isBlank() || adminName.isBlank() || adminEmail.isBlank() || phoneNumber.isBlank()) {
            throw new ResourceConflictException("District director, admin, email, and phone are required.");
        }
        if (!Set.of("ACTIVE", "INACTIVE").contains(status)) {
            throw new ResourceConflictException("District status must be Active or Inactive.");
        }

        district.setDirectorName(directorName);
        district.setAdminName(adminName);
        district.setAdminEmail(adminEmail);
        district.setPhoneNumber(phoneNumber);
        district.setStatus(status);
        district.setActive("ACTIVE".equals(status));
        district.setLicensingStatus(status);
        district.setContactEmail(adminEmail);
        district.setContactPhone(phoneNumber);
        districtRepository.save(district);

        districtAdminProfileRepository.findByDistrictIdAndActiveTrueAndDeletedFalse(district.getId())
                .forEach(profile -> profile.setActive(district.isActive()));

        writeAudit(principal, "ADMIN_DISTRICT_UPDATED", "DISTRICT", district.getId(), Map.of(
                "districtCode", district.getDistrictCode(),
                "status", status
        ));
        return toDistrictDto(district);
    }

    @Transactional
    public AdminDistrictDtos.AdminDistrictItemDto createDistrictAdmin(UUID districtId, Principal principal) {
        District district = districtRepository.findById(districtId)
                .orElseThrow(() -> new ResourceConflictException("District not found."));
        AdminCredentials credentials = createDistrictAdminAccount(district);
        writeAudit(principal, "ADMIN_DISTRICT_ADMIN_CREATED", "DISTRICT", district.getId(), Map.of(
                "districtCode", district.getDistrictCode(),
                "username", credentials.username()
        ));
        return toDistrictDto(district, credentials.username(), credentials.temporaryPassword());
    }

    @Transactional(readOnly = true)
    public AdminPlatformSettingsDto settings() {
        return platformSettingsService.getCurrentSettings();
    }

    @Transactional
    public AdminPlatformSettingsDto updateSettings(AdminPlatformSettingsUpdateRequest request, Principal principal) {
        AdminPlatformSettingsDto updated = platformSettingsService.updateSettings(request);
        writeAudit(principal, "ADMIN_SETTINGS_UPDATED", "PLATFORM_SETTINGS", updated.id(), request);
        return updated;
    }

    @Transactional
    public AdminBulkUploadResultDto bulkUploadUsers(MultipartFile file, Principal principal) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ResourceConflictException("A CSV file is required");
        }
        String originalFilename = safe(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
        if (!originalFilename.endsWith(".csv")) {
            throw new ResourceConflictException("Only CSV files are supported");
        }

        int maxRows = platformSettingsService.getCurrentSettingsEntity().getMaxCsvBulkUploadRows();
        List<AdminUserDto> createdUsers = new ArrayList<>();
        List<AdminBulkUploadRowErrorDto> errors = new ArrayList<>();
        Set<String> seenEmails = new LinkedHashSet<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new ResourceConflictException("The uploaded file is empty");
            }
            List<String> headers = parseCsvLine(headerLine).stream().map(this::sanitizeHeader).toList();
            Map<String, Integer> headerIndex = indexHeaders(headers);
            for (String requiredHeader : REQUIRED_BULK_UPLOAD_HEADERS) {
                if (!headerIndex.containsKey(requiredHeader.toLowerCase(Locale.ROOT))) {
                    throw new ResourceConflictException("CSV is missing required header: %s".formatted(requiredHeader));
                }
            }

            String line;
            int rowNumber = 1;
            int totalRows = 0;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) {
                    continue;
                }
                totalRows++;
                if (totalRows > maxRows) {
                    errors.add(new AdminBulkUploadRowErrorDto(rowNumber, "Exceeded maximum upload rows (%d)".formatted(maxRows)));
                    continue;
                }
                processBulkUserRow(principal, rowNumber, line, headerIndex, seenEmails, createdUsers, errors);
            }

            writeAudit(principal, "ADMIN_BULK_USER_UPLOAD", "USER", null, Map.of("totalRows", totalRows, "successfulRows", createdUsers.size(), "failedRows", errors.size()));
            return new AdminBulkUploadResultDto(totalRows, createdUsers.size(), errors.size(), createdUsers, errors);
        }
    }

    public String bulkUploadTemplate() {
        return """
                email,firstName,lastName,role,password,phoneNumber
                student.one@example.com,Student,One,STUDENT,TempPass123,+26770000001
                company.admin@example.com,Company,Admin,COMPANY,TempPass123,+26770000002
                admin.ops@example.com,Admin,Ops,ADMIN,TempPass123,+26770000003
                """;
    }

    private AdminDistrictDtos.AdminDistrictItemDto toDistrictDto(District district) {
        List<DistrictAdminProfile> profiles = districtAdminProfileRepository.findByDistrictIdAndActiveTrueAndDeletedFalse(district.getId());
        String username = profiles.stream()
                .map(DistrictAdminProfile::getUserId)
                .map(userRepository::findById)
                .flatMap(Optional::stream)
                .map(User::getUsername)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
        return toDistrictDto(district, profiles, username, null);
    }

    private AdminDistrictDtos.AdminDistrictItemDto toDistrictDto(District district, String username, String temporaryPassword) {
        List<DistrictAdminProfile> profiles = districtAdminProfileRepository.findByDistrictIdAndActiveTrueAndDeletedFalse(district.getId());
        return toDistrictDto(district, profiles, username, temporaryPassword);
    }

    private AdminDistrictDtos.AdminDistrictItemDto toDistrictDto(
            District district,
            List<DistrictAdminProfile> profiles,
            String username,
            String temporaryPassword
    ) {
        long schoolCount = schoolRepository.findByDistrictIdOrderBySchoolNameAsc(district.getId()).size();
        long pendingRegistrations = schoolRegistrationRequestRepository.findByDistrictIdAndStatusOrderBySubmittedAtDesc(
                district.getId(),
                SchoolStatus.PENDING_DISTRICT_APPROVAL
        ).size();
        boolean hasAssignedAdmin = !profiles.isEmpty();
        String warningMessage = hasAssignedAdmin ? null : "No District Admin assigned. School registrations cannot be reviewed.";
        return new AdminDistrictDtos.AdminDistrictItemDto(
                district.getId(),
                district.getDistrictCode(),
                district.getDistrictName(),
                district.getDirectorName(),
                district.getAdminName(),
                district.getAdminEmail(),
                district.getPhoneNumber(),
                district.getAddress(),
                district.getStatus(),
                district.isActive(),
                schoolCount,
                pendingRegistrations,
                hasAssignedAdmin,
                warningMessage,
                username,
                temporaryPassword,
                district.getCreatedAt()
        );
    }

    private AdminCredentials createDistrictAdminAccount(District district) {
        if (!safe(district.getAdminName()).isBlank() && !safe(district.getAdminEmail()).isBlank() && !safe(district.getPhoneNumber()).isBlank()) {
            if (!districtAdminProfileRepository.findByDistrictIdAndActiveTrueAndDeletedFalse(district.getId()).isEmpty()) {
                throw new ResourceConflictException("District already has an assigned District Admin.");
            }
            if (userRepository.existsByEmailIgnoreCase(district.getAdminEmail())) {
                throw new ResourceConflictException("District admin email already exists.");
            }
            String username = generateUniqueDistrictUsername(district.getDistrictName());
            String temporaryPassword = "Temp@12345";
            Role districtAdminRole = roleRepository.findByName("ROLE_DISTRICT_ADMIN").orElseGet(() -> {
                Role role = new Role();
                role.setName("ROLE_DISTRICT_ADMIN");
                return roleRepository.save(role);
            });

            User user = new User();
            user.setEmail(district.getAdminEmail().trim().toLowerCase(Locale.ROOT));
            user.setUsername(username);
            user.setPhoneNumber(trimToNull(district.getPhoneNumber()));
            user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
            user.setFirstName(trimToNull(district.getAdminName()) == null ? district.getDistrictName() : district.getAdminName().trim());
            user.setLastName("District Admin");
            user.setStatus(UserStatus.ACTIVE);
            user.setEmailVerified(true);
            user.setMustChangePassword(true);
            user.getRoles().add(districtAdminRole);
            user = userRepository.save(user);

            DistrictAdminProfile profile = new DistrictAdminProfile();
            profile.setDistrictId(district.getId());
            profile.setUserId(user.getId());
            profile.setTitle("District Admin");
            profile.setActive(district.isActive());
            profile.setDeleted(false);
            districtAdminProfileRepository.save(profile);
            return new AdminCredentials(username, temporaryPassword);
        }
        throw new ResourceConflictException("District Admin name, email, and phone must be assigned before creating a district admin account.");
    }

    private record AdminCredentials(String username, String temporaryPassword) {}

    private boolean matchesUserSearch(User user, String query) {
        String fullName = (safe(user.getFirstName()) + " " + safe(user.getLastName())).trim().toLowerCase(Locale.ROOT);
        return safe(user.getEmail()).toLowerCase(Locale.ROOT).contains(query)
                || fullName.contains(query)
                || user.getRoles().stream().map(Role::getName).anyMatch(role -> role.toLowerCase(Locale.ROOT).contains(query));
    }

    private void ensureCanModifyUser(User actor, User target, String action) {
        if (actor.getId().equals(target.getId())) {
            throw new ResourceConflictException("You cannot %s your own account from admin tools".formatted(action));
        }
        if (hasRole(target, "ROLE_ADMIN") && target.getStatus() == UserStatus.ACTIVE && countActiveAdmins() <= 1) {
            throw new ResourceConflictException("You cannot %s the last active admin".formatted(action));
        }
    }

    private long countActiveAdmins() {
        return userRepository.findAll().stream()
                .filter(this::isLiveUser)
                .filter(user -> hasRole(user, "ROLE_ADMIN"))
                .count();
    }

    private AdminCompanyDto changeCompanyStatus(UUID companyId, CompanyApprovalStatus newStatus, String notes, Principal principal, String auditAction) {
        CompanyProfile company = companyProfileRepository.findById(companyId).orElseThrow(() -> new ResourceConflictException("Company not found"));
        if (company.getDeletedAt() != null) {
            throw new ResourceConflictException("Company has been deleted");
        }
        company.setStatus(newStatus);
        company.setReviewedAt(OffsetDateTime.now());
        company.setReviewedBy(currentUserService.requireUser(principal).getId());
        company.setReviewNotes(trimToNull(notes));
        CompanyProfile saved = companyProfileRepository.save(company);
        writeAudit(principal, auditAction, "COMPANY", saved.getId(), Map.of("status", newStatus.name(), "notes", safe(notes)));
        return toCompanyDto(saved);
    }

    private void ensureBursaryNotDeleted(Bursary bursary) {
        if (bursary.getDeletedAt() != null) {
            throw new ResourceConflictException("Bursary has been deleted");
        }
    }

    private boolean isClosedOrExpired(Bursary bursary) {
        String status = safe(bursary.getStatus()).toUpperCase(Locale.ROOT);
        if (Set.of("CLOSED", "ARCHIVED", "EXPIRED").contains(status)) {
            return true;
        }
        return bursary.getApplicationEndDate() != null
                && bursary.getApplicationEndDate().isBefore(LocalDate.now())
                && !"ACTIVE".equals(status);
    }

    private List<AdminMonthlyMetricDto> toMonthlyMetrics(List<OffsetDateTime> timestamps) {
        YearMonth current = YearMonth.now();
        List<YearMonth> months = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            months.add(current.minusMonths(i));
        }
        Map<YearMonth, Long> grouped = timestamps.stream()
                .filter(ts -> ts != null)
                .map(YearMonth::from)
                .collect(Collectors.groupingBy(month -> month, Collectors.counting()));
        return months.stream()
                .map(month -> new AdminMonthlyMetricDto(month.format(YEAR_MONTH_FORMATTER), grouped.getOrDefault(month, 0L)))
                .toList();
    }

    private AdminUserDto toUserDto(User user, CompanyProfile companyProfile) {
        List<String> roles = user.getRoles().stream().map(Role::getName).sorted().toList();
        String primaryRole = roles.stream()
                .sorted(Comparator.comparingInt(role -> {
                    int index = ROLE_PRIORITY.indexOf(role);
                    return index >= 0 ? index : Integer.MAX_VALUE;
                }))
                .findFirst()
                .orElse(roles.isEmpty() ? null : roles.get(0));
        return new AdminUserDto(
                user.getId(),
                ("%s %s".formatted(safe(user.getFirstName()), safe(user.getLastName()))).trim(),
                user.getEmail(),
                roles,
                primaryRole,
                user.getStatus().name(),
                user.getStatus() == UserStatus.ACTIVE,
                companyProfile == null ? null : companyProfile.getStatus().name(),
                user.getCreatedAt(),
                user.getDeletedAt()
        );
    }

    private AdminCompanyDto toCompanyDto(CompanyProfile company) {
        return new AdminCompanyDto(
                company.getId(),
                company.getUserId(),
                company.getCompanyName(),
                company.getRegistrationNumber(),
                company.getOfficialEmail(),
                company.getIndustry(),
                company.getStatus().name(),
                company.getReviewNotes(),
                company.getReviewedAt(),
                company.getCreatedAt(),
                company.getDeletedAt()
        );
    }

    private AdminBursaryDto toBursaryDto(Bursary bursary, CompanyProfile companyProfile) {
        return new AdminBursaryDto(
                bursary.getId(),
                bursary.getTitle(),
                bursary.getCompanyId(),
                companyProfile == null ? null : companyProfile.getCompanyName(),
                safe(bursary.getStatus()),
                bursary.getApplicationStartDate(),
                bursary.getApplicationEndDate(),
                applicationRepository.countByBursaryId(bursary.getId()),
                bursary.getCreatedAt(),
                bursary.getDeletedAt()
        );
    }

    private void processBulkUserRow(
            Principal principal,
            int rowNumber,
            String line,
            Map<String, Integer> headerIndex,
            Set<String> seenEmails,
            List<AdminUserDto> createdUsers,
            List<AdminBulkUploadRowErrorDto> errors
    ) {
        List<String> values;
        try {
            values = parseCsvLine(line);
        } catch (RuntimeException ex) {
            errors.add(new AdminBulkUploadRowErrorDto(rowNumber, "Malformed CSV row"));
            return;
        }

        String email = valueAt(headerIndex, values, "email").toLowerCase(Locale.ROOT);
        String firstName = valueAt(headerIndex, values, "firstName");
        String lastName = valueAt(headerIndex, values, "lastName");
        String roleInput = valueAt(headerIndex, values, "role");
        String password = valueAt(headerIndex, values, "password");
        String phoneNumber = valueAt(headerIndex, values, "phoneNumber");

        if (email.isBlank() || !email.contains("@")) {
            errors.add(new AdminBulkUploadRowErrorDto(rowNumber, "Invalid email"));
            return;
        }
        if (firstName.isBlank() || lastName.isBlank()) {
            errors.add(new AdminBulkUploadRowErrorDto(rowNumber, "First name and last name are required"));
            return;
        }
        if (password.length() < 8) {
            errors.add(new AdminBulkUploadRowErrorDto(rowNumber, "Password must be at least 8 characters"));
            return;
        }
        if (!seenEmails.add(email)) {
            errors.add(new AdminBulkUploadRowErrorDto(rowNumber, "Duplicate email in CSV: %s".formatted(email)));
            return;
        }
        if (userRepository.existsByEmail(email)) {
            errors.add(new AdminBulkUploadRowErrorDto(rowNumber, "Email already exists: %s".formatted(email)));
            return;
        }
        if (!phoneNumber.isBlank() && userRepository.existsByPhoneNumber(phoneNumber)) {
            errors.add(new AdminBulkUploadRowErrorDto(rowNumber, "Phone number already exists: %s".formatted(phoneNumber)));
            return;
        }

        String roleName;
        try {
            roleName = normalizeRoleName(roleInput);
        } catch (ResourceConflictException ex) {
            errors.add(new AdminBulkUploadRowErrorDto(rowNumber, ex.getMessage()));
            return;
        }
        Role role = roleRepository.findByName(roleName).orElse(null);
        if (role == null) {
            errors.add(new AdminBulkUploadRowErrorDto(rowNumber, "Unknown role: %s".formatted(roleName)));
            return;
        }

        User user = new User();
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber.isBlank() ? null : phoneNumber);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        user.getRoles().add(role);
        User saved = userRepository.save(user);
        createdUsers.add(toUserDto(saved, companyProfileRepository.findByUserId(saved.getId()).orElse(null)));
    }

    private Map<String, Integer> indexHeaders(List<String> headers) {
        Map<String, Integer> index = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            index.put(headers.get(i).toLowerCase(Locale.ROOT), i);
        }
        return index;
    }

    private String valueAt(Map<String, Integer> headerIndex, List<String> rowValues, String key) {
        Integer index = headerIndex.get(key.toLowerCase(Locale.ROOT));
        if (index == null || index < 0 || index >= rowValues.size()) {
            return "";
        }
        return rowValues.get(index).trim();
    }

    private String sanitizeHeader(String header) {
        String trimmed = header == null ? "" : header.trim();
        if (trimmed.startsWith("\uFEFF")) {
            return trimmed.substring(1).trim();
        }
        return trimmed;
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (inQuotes) {
            throw new ResourceConflictException("Malformed CSV: unterminated quoted value");
        }
        values.add(current.toString());
        return values;
    }

    private Map<String, Object> toRoleMap(Role role) {
        List<String> permissions = rolePermissionRepository.findByRoleIdOrderByPermissionCodeAsc(role.getId()).stream()
                .filter(RolePermission::isActive)
                .map(RolePermission::getPermissionCode)
                .toList();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", role.getId());
        body.put("name", role.getName());
        body.put("permissions", permissions);
        body.put("active", true);
        return body;
    }

    private void syncPermissions(Role role, List<String> requestedPermissions) {
        rolePermissionRepository.deleteByRoleId(role.getId());
        List<String> permissions = requestedPermissions.isEmpty() ? DEFAULT_ROLE_PERMISSIONS : requestedPermissions;
        for (String permission : new LinkedHashSet<>(permissions)) {
            RolePermission rp = new RolePermission();
            rp.setRoleId(role.getId());
            rp.setPermissionCode(permission);
            rp.setActive(true);
            rolePermissionRepository.save(rp);
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> extractPermissions(Map<String, Object> payload) {
        Object raw = payload.get("permissions");
        if (raw instanceof Collection<?> values) {
            return values.stream().map(String::valueOf).map(String::trim).filter(v -> !v.isBlank()).toList();
        }
        return List.of();
    }

    private void ensureDefaultPermissions() {
        for (String roleName : List.of(
                "ROLE_STUDENT",
                "ROLE_COMPANY",
                "ROLE_ADMIN",
                "ROLE_DISTRICT_ADMIN",
                "ROLE_DISTRICT_DIRECTOR",
                "ROLE_CIRCUIT_MANAGER",
                "ROLE_SUBJECT_ADVISOR",
                "ROLE_SCHOOL_ADMIN",
                "ROLE_TEACHER",
                "ROLE_SCHOOL_STUDENT"
        )) {
            Role role = roleRepository.findByName(roleName).orElse(null);
            if (role != null && rolePermissionRepository.findByRoleIdOrderByPermissionCodeAsc(role.getId()).isEmpty()) {
                syncPermissions(role, DEFAULT_ROLE_PERMISSIONS);
            }
        }
    }

    private void writeAudit(Principal principal, String action, String entityType, UUID entityId, Object details) {
        AuditLog log = new AuditLog();
        log.setDetails(details == null ? null : objectMapper.valueToTree(details));
        log.setActorId(principal == null ? null : currentUserService.requireUser(principal).getId());
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        auditLogRepository.save(log);
    }

    private CompanyApprovalStatus parseCompanyStatus(String status) {
        String normalized = safe(status).toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return null;
        }
        try {
            return CompanyApprovalStatus.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new ResourceConflictException("Unknown company status: %s".formatted(normalized));
        }
    }
}

