package com.edurite.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edurite.account.service.AccountService;
import com.edurite.admin.dto.AdminAnalyticsDto;
import com.edurite.admin.dto.AdminBulkUploadResultDto;
import com.edurite.admin.dto.AdminPlatformSettingsDto;
import com.edurite.admin.dto.AdminPlatformSettingsUpdateRequest;
import com.edurite.admin.entity.AuditLog;
import com.edurite.admin.entity.PlatformSetting;
import com.edurite.admin.repository.AuditLogRepository;
import com.edurite.admin.repository.RolePermissionRepository;
import com.edurite.admin.service.AdminService;
import com.edurite.admin.service.PlatformSettingsService;
import com.edurite.application.entity.ApplicationRecord;
import com.edurite.application.repository.ApplicationRepository;
import com.edurite.bursary.entity.Bursary;
import com.edurite.bursary.repository.BursaryRepository;
import com.edurite.company.entity.CompanyApprovalStatus;
import com.edurite.company.entity.CompanyProfile;
import com.edurite.company.repository.CompanyProfileRepository;
import com.edurite.district.repository.DistrictAdminProfileRepository;
import com.edurite.district.repository.DistrictRepository;
import com.edurite.security.service.CurrentUserService;
import com.edurite.school.portal.repository.SchoolRegistrationRequestRepository;
import com.edurite.school.portal.repository.SchoolRepository;
import com.edurite.user.entity.Role;
import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import com.edurite.user.repository.RoleRepository;
import com.edurite.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

class AdminServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final RolePermissionRepository rolePermissionRepository = mock(RolePermissionRepository.class);
    private final AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
    private final BursaryRepository bursaryRepository = mock(BursaryRepository.class);
    private final ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
    private final CompanyProfileRepository companyProfileRepository = mock(CompanyProfileRepository.class);
    private final DistrictRepository districtRepository = mock(DistrictRepository.class);
    private final DistrictAdminProfileRepository districtAdminProfileRepository = mock(DistrictAdminProfileRepository.class);
    private final SchoolRepository schoolRepository = mock(SchoolRepository.class);
    private final SchoolRegistrationRequestRepository schoolRegistrationRequestRepository = mock(SchoolRegistrationRequestRepository.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final PlatformSettingsService platformSettingsService = mock(PlatformSettingsService.class);
    private final AccountService accountService = mock(AccountService.class);

    private AdminService adminService;
    private User actor;
    private Principal principal;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(
                userRepository,
                roleRepository,
                rolePermissionRepository,
                auditLogRepository,
                bursaryRepository,
                applicationRepository,
                companyProfileRepository,
                districtRepository,
                districtAdminProfileRepository,
                schoolRepository,
                schoolRegistrationRequestRepository,
                currentUserService,
                passwordEncoder,
                new ObjectMapper(),
                platformSettingsService,
                accountService
        );
        actor = userWithRole("admin@edurite.local", "ROLE_ADMIN", UserStatus.ACTIVE, false);
        actor.setId(UUID.randomUUID());
        principal = () -> actor.getEmail();

        when(currentUserService.requireUser(any())).thenReturn(actor);
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(companyProfileRepository.findByUserId(any())).thenReturn(Optional.empty());
    }

    @Test
    void analyticsReturnsLivePlatformCounts() {
        User student = userWithRole("student@edurite.local", "ROLE_STUDENT", UserStatus.ACTIVE, false);
        User admin = userWithRole("ops@edurite.local", "ROLE_ADMIN", UserStatus.ACTIVE, false);
        User deletedUser = userWithRole("old@edurite.local", "ROLE_STUDENT", UserStatus.DELETED, true);

        CompanyProfile pending = company("Pending Co", CompanyApprovalStatus.PENDING, false);
        CompanyProfile approved = company("Approved Co", CompanyApprovalStatus.APPROVED, false);
        CompanyProfile suspended = company("Suspended Co", CompanyApprovalStatus.SUSPENDED, false);
        CompanyProfile deletedCompany = company("Deleted Co", CompanyApprovalStatus.APPROVED, true);

        Bursary active = bursary("Active Bursary", "ACTIVE", false, LocalDate.now().plusDays(14));
        Bursary suspendedBursary = bursary("Suspended Bursary", "SUSPENDED", false, LocalDate.now().plusDays(3));
        Bursary closed = bursary("Closed Bursary", "CLOSED", false, LocalDate.now().minusDays(1));
        Bursary deletedBursary = bursary("Deleted Bursary", "ACTIVE", true, LocalDate.now().plusDays(21));

        ApplicationRecord app1 = application(active.getId());
        ApplicationRecord app2 = application(active.getId());
        ApplicationRecord app3 = application(closed.getId());

        when(userRepository.findAll()).thenReturn(List.of(student, admin, deletedUser));
        when(companyProfileRepository.findAll()).thenReturn(List.of(pending, approved, suspended, deletedCompany));
        when(bursaryRepository.findAll()).thenReturn(List.of(active, suspendedBursary, closed, deletedBursary));
        when(applicationRepository.findAll()).thenReturn(List.of(app1, app2, app3));

        AdminAnalyticsDto analytics = adminService.analytics();

        assertThat(analytics.totalUsers()).isEqualTo(2);
        assertThat(analytics.totalStudents()).isEqualTo(1);
        assertThat(analytics.totalAdmins()).isEqualTo(1);
        assertThat(analytics.totalCompanies()).isEqualTo(3);
        assertThat(analytics.pendingCompanyApprovals()).isEqualTo(1);
        assertThat(analytics.approvedCompanies()).isEqualTo(1);
        assertThat(analytics.suspendedCompanies()).isEqualTo(1);
        assertThat(analytics.totalBursaries()).isEqualTo(3);
        assertThat(analytics.activeBursaries()).isEqualTo(1);
        assertThat(analytics.suspendedBursaries()).isEqualTo(1);
        assertThat(analytics.closedOrExpiredBursaries()).isEqualTo(1);
        assertThat(analytics.totalApplicationsSubmitted()).isEqualTo(3);
        assertThat(analytics.applicationsPerBursary())
                .extracting(item -> item.bursaryTitle() + ":" + item.totalApplications())
                .contains("Active Bursary:2", "Closed Bursary:1", "Suspended Bursary:0");
    }

    @Test
    void usersExcludesDeletedByDefault() {
        User activeUser = userWithRole("active@edurite.local", "ROLE_STUDENT", UserStatus.ACTIVE, false);
        User statusDeletedWithoutDeletedAt = userWithRole("ghost@edurite.local", "ROLE_STUDENT", UserStatus.DELETED, false);
        User deletedAtUser = userWithRole("deleted@edurite.local", "ROLE_STUDENT", UserStatus.ACTIVE, true);

        when(userRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(activeUser, statusDeletedWithoutDeletedAt, deletedAtUser));
        when(companyProfileRepository.findAll()).thenReturn(List.of());

        var users = adminService.users(null, null, null, null, false);

        assertThat(users).hasSize(1);
        assertThat(users.getFirst().email()).isEqualTo("active@edurite.local");
    }

    @Test
    void suspendCompanyPersistsStatusChange() {
        CompanyProfile company = company("Suspend Me", CompanyApprovalStatus.PENDING, false);
        UUID companyId = company.getId();
        when(companyProfileRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(companyProfileRepository.save(any(CompanyProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var dto = adminService.suspendCompany(companyId, "violated policy", principal);

        assertThat(dto.status()).isEqualTo("SUSPENDED");
        assertThat(dto.reviewNotes()).isEqualTo("violated policy");
        verify(companyProfileRepository, times(1)).save(company);
        verify(auditLogRepository, atLeastOnce()).save(any(AuditLog.class));
    }

    @Test
    void deleteBursarySoftDeletesRecord() {
        Bursary bursary = bursary("Delete Me", "ACTIVE", false, LocalDate.now().plusDays(5));
        UUID bursaryId = bursary.getId();
        when(bursaryRepository.findById(bursaryId)).thenReturn(Optional.of(bursary));
        when(bursaryRepository.save(any(Bursary.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(companyProfileRepository.findById(eq(bursary.getCompanyId()))).thenReturn(Optional.empty());

        var dto = adminService.deleteBursary(bursaryId, "removed by admin", principal);

        assertThat(dto.status()).isEqualTo("DELETED");
        assertThat(dto.deletedAt()).isNotNull();
        assertThat(bursary.getDeletedBy()).isEqualTo(actor.getId());
        assertThat(bursary.getDeletionReason()).isEqualTo("removed by admin");
        verify(auditLogRepository, atLeastOnce()).save(any(AuditLog.class));
    }

    @Test
    void updateSettingsWritesAuditAndReturnsUpdatedValue() {
        AdminPlatformSettingsUpdateRequest request = new AdminPlatformSettingsUpdateRequest(
                true, true, true, true, true, false, false, "support@edurite.local", "Help Desk", 900
        );
        AdminPlatformSettingsDto expected = new AdminPlatformSettingsDto(
                UUID.randomUUID(), true, true, true, true, true, false, false,
                "support@edurite.local", "Help Desk", 900, OffsetDateTime.now()
        );
        when(platformSettingsService.updateSettings(request)).thenReturn(expected);

        AdminPlatformSettingsDto actual = adminService.updateSettings(request, principal);

        assertThat(actual).isEqualTo(expected);
        verify(platformSettingsService, times(1)).updateSettings(request);
        verify(auditLogRepository, atLeastOnce()).save(any(AuditLog.class));
    }

    @Test
    void bulkUploadUsersProcessesValidRowsAndReportsFailures() throws Exception {
        PlatformSetting settings = new PlatformSetting();
        settings.setMaxCsvBulkUploadRows(100);
        when(platformSettingsService.getCurrentSettingsEntity()).thenReturn(settings);

        Role studentRole = new Role();
        studentRole.setId(UUID.randomUUID());
        studentRole.setName("ROLE_STUDENT");
        when(roleRepository.findByName("ROLE_STUDENT")).thenReturn(Optional.of(studentRole));
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(UUID.randomUUID());
            }
            user.setCreatedAt(OffsetDateTime.now());
            return user;
        });

        String csv = """
                email,firstName,lastName,role,password,phoneNumber
                learner.one@example.com,Learner,One,STUDENT,StrongPass123,+26770000001
                invalid-email,Learner,Bad,STUDENT,StrongPass123,+26770000002
                learner.one@example.com,Learner,Duplicate,STUDENT,StrongPass123,+26770000003
                """;
        MockMultipartFile file = new MockMultipartFile("file", "users.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        AdminBulkUploadResultDto result = adminService.bulkUploadUsers(file, principal);

        assertThat(result.totalRows()).isEqualTo(3);
        assertThat(result.successfulRows()).isEqualTo(1);
        assertThat(result.failedRows()).isEqualTo(2);
        assertThat(result.errors()).extracting(error -> error.message())
                .anyMatch(message -> message.contains("Invalid email"))
                .anyMatch(message -> message.contains("Duplicate email in CSV"));
        assertThat(result.createdUsers()).hasSize(1);
        verify(auditLogRepository, atLeastOnce()).save(any(AuditLog.class));
    }

    @Test
    void bulkUploadUsersAcceptsUtf8BomHeader() throws Exception {
        PlatformSetting settings = new PlatformSetting();
        settings.setMaxCsvBulkUploadRows(100);
        when(platformSettingsService.getCurrentSettingsEntity()).thenReturn(settings);

        Role studentRole = new Role();
        studentRole.setId(UUID.randomUUID());
        studentRole.setName("ROLE_STUDENT");
        when(roleRepository.findByName("ROLE_STUDENT")).thenReturn(Optional.of(studentRole));
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(UUID.randomUUID());
            }
            user.setCreatedAt(OffsetDateTime.now());
            return user;
        });

        String csv = "\uFEFFemail,firstName,lastName,role,password,phoneNumber\n"
                + "bom.user@example.com,Bom,User,STUDENT,StrongPass123,+26770000100\n";
        MockMultipartFile file = new MockMultipartFile("file", "users.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        AdminBulkUploadResultDto result = adminService.bulkUploadUsers(file, principal);

        assertThat(result.totalRows()).isEqualTo(1);
        assertThat(result.successfulRows()).isEqualTo(1);
        assertThat(result.failedRows()).isEqualTo(0);
        assertThat(result.createdUsers()).hasSize(1);
    }

    private User userWithRole(String email, String roleName, UserStatus status, boolean deleted) {
        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setName(roleName);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setFirstName("First");
        user.setLastName("Last");
        user.setPasswordHash("hash");
        user.setStatus(status);
        user.setEmailVerified(true);
        user.setCreatedAt(OffsetDateTime.now().minusDays(3));
        if (deleted) {
            user.setDeletedAt(OffsetDateTime.now().minusDays(1));
        }
        user.getRoles().add(role);
        return user;
    }

    private CompanyProfile company(String name, CompanyApprovalStatus status, boolean deleted) {
        CompanyProfile company = new CompanyProfile();
        company.setId(UUID.randomUUID());
        company.setUserId(UUID.randomUUID());
        company.setCompanyName(name);
        company.setRegistrationNumber(name.replace(" ", "").toUpperCase());
        company.setOfficialEmail(name.replace(" ", "").toLowerCase() + "@example.com");
        company.setIndustry("Education");
        company.setStatus(status);
        company.setCreatedAt(OffsetDateTime.now().minusDays(2));
        if (deleted) {
            company.setDeletedAt(OffsetDateTime.now().minusDays(1));
        }
        return company;
    }

    private Bursary bursary(String title, String status, boolean deleted, LocalDate endDate) {
        Bursary bursary = new Bursary();
        bursary.setId(UUID.randomUUID());
        bursary.setCompanyId(UUID.randomUUID());
        bursary.setTitle(title);
        bursary.setStatus(status);
        bursary.setApplicationStartDate(LocalDate.now().minusDays(10));
        bursary.setApplicationEndDate(endDate);
        bursary.setCreatedAt(OffsetDateTime.now().minusDays(1));
        if (deleted) {
            bursary.setDeletedAt(OffsetDateTime.now());
        }
        return bursary;
    }

    private ApplicationRecord application(UUID bursaryId) {
        ApplicationRecord record = new ApplicationRecord();
        record.setId(UUID.randomUUID());
        record.setBursaryId(bursaryId);
        record.setStudentId(UUID.randomUUID());
        record.setStatus("SUBMITTED");
        record.setCreatedAt(OffsetDateTime.now().minusDays(1));
        return record;
    }
}

