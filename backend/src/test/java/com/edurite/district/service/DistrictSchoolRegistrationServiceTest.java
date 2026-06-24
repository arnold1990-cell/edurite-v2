package com.edurite.district.service;

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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DistrictSchoolRegistrationServiceTest {

    @Mock private SchoolRegistrationRequestRepository schoolRegistrationRequestRepository;
    @Mock private SchoolRepository schoolRepository;
    @Mock private SchoolUserProfileRepository schoolUserProfileRepository;
    @Mock private SchoolCircuitAssignmentRepository schoolCircuitAssignmentRepository;
    @Mock private NotificationService notificationService;

    private DistrictSchoolRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new DistrictSchoolRegistrationService(
                schoolRegistrationRequestRepository,
                schoolRepository,
                schoolUserProfileRepository,
                schoolCircuitAssignmentRepository,
                notificationService
        );
    }

    @Test
    void approveCreatesSchoolAndSchoolAdminProfile() {
        UUID districtId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SchoolRegistrationRequest request = new SchoolRegistrationRequest();
        request.setId(UUID.randomUUID());
        request.setDistrictId(districtId);
        request.setUserId(userId);
        request.setSchoolName("Kgale Secondary");
        request.setEmisNumber("EMIS-001");
        request.setDistrictName("Gaborone");
        request.setProvince("South East");
        request.setSchoolEmail("school@example.com");
        request.setPhoneNumber("+26771234567");
        request.setPhysicalAddress("Plot 10 Gaborone");
        request.setStatus(SchoolStatus.PENDING_DISTRICT_APPROVAL);

        when(schoolRegistrationRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(schoolRepository.existsByRegistrationNumberIgnoreCase("EMIS-001")).thenReturn(false);
        when(schoolRepository.save(any(School.class))).thenAnswer(invocation -> {
            School school = invocation.getArgument(0);
            school.setId(UUID.randomUUID());
            return school;
        });
        when(schoolUserProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.empty());
        when(schoolUserProfileRepository.save(any(SchoolUserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(schoolRegistrationRequestRepository.save(any(SchoolRegistrationRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DistrictDtos.SchoolRegistrationRequestItemDto response = service.decide(
                districtId,
                request.getId(),
                UUID.randomUUID(),
                new DistrictDtos.SchoolRegistrationDecisionRequest("APPROVE", null)
        );

        ArgumentCaptor<SchoolUserProfile> profileCaptor = ArgumentCaptor.forClass(SchoolUserProfile.class);
        verify(schoolUserProfileRepository).save(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getRoleName()).isEqualTo("ROLE_SCHOOL_ADMIN");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(request.getSchoolId()).isNotNull();
    }

    @Test
    void requestsTrimsSearchTextBeforeRepositoryQuery() {
        UUID districtId = UUID.randomUUID();
        when(schoolRegistrationRequestRepository.findByDistrictIdOrderBySubmittedAtDesc(districtId)).thenReturn(java.util.List.of());
        when(schoolRegistrationRequestRepository.searchByDistrictAndStatus(districtId, SchoolStatus.PENDING_DISTRICT_APPROVAL, "Kgale")).thenReturn(java.util.List.of());

        DistrictDtos.SchoolRegistrationRequestsResponse response = service.requests(districtId, "PENDING", "  Kgale  ");

        verify(schoolRegistrationRequestRepository).findByDistrictIdOrderBySubmittedAtDesc(districtId);
        verify(schoolRegistrationRequestRepository).searchByDistrictAndStatus(districtId, SchoolStatus.PENDING_DISTRICT_APPROVAL, "Kgale");
        assertThat(response.total()).isEqualTo(0);
    }

    @Test
    void requestsConvertsBlankSearchTextToNull() {
        UUID districtId = UUID.randomUUID();
        when(schoolRegistrationRequestRepository.findByDistrictIdOrderBySubmittedAtDesc(districtId)).thenReturn(java.util.List.of());
        when(schoolRegistrationRequestRepository.findByDistrictIdAndStatusOrderBySubmittedAtDesc(districtId, SchoolStatus.PENDING_DISTRICT_APPROVAL))
                .thenReturn(java.util.List.of());

        service.requests(districtId, "PENDING", "   ");

        verify(schoolRegistrationRequestRepository).findByDistrictIdAndStatusOrderBySubmittedAtDesc(districtId, SchoolStatus.PENDING_DISTRICT_APPROVAL);
    }

    @Test
    void requestsUsesSearchWithoutStatusWhenStatusFilterIsMissing() {
        UUID districtId = UUID.randomUUID();
        when(schoolRegistrationRequestRepository.findByDistrictIdOrderBySubmittedAtDesc(districtId)).thenReturn(java.util.List.of());
        when(schoolRegistrationRequestRepository.searchByDistrict(districtId, "Kgale")).thenReturn(java.util.List.of());

        service.requests(districtId, null, "Kgale");

        verify(schoolRegistrationRequestRepository).searchByDistrict(districtId, "Kgale");
    }
}
