package com.edurite.school.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edurite.common.exception.ResourceConflictException;
import com.edurite.school.dto.SchoolLinkDtos;
import com.edurite.school.portal.entity.School;
import com.edurite.school.portal.entity.SchoolUserProfile;
import com.edurite.school.portal.entity.StudentSchoolLink;
import com.edurite.school.portal.repository.LearnerEnrollmentRepository;
import com.edurite.school.portal.repository.SchoolClassRepository;
import com.edurite.school.portal.repository.SchoolRepository;
import com.edurite.school.portal.repository.SchoolUserProfileRepository;
import com.edurite.school.portal.repository.StudentSchoolLinkRepository;
import com.edurite.security.service.CurrentUserService;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MySchoolServiceTest {

    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final SchoolRepository schoolRepository = mock(SchoolRepository.class);
    private final StudentSchoolLinkRepository studentSchoolLinkRepository = mock(StudentSchoolLinkRepository.class);
    private final SchoolUserProfileRepository schoolUserProfileRepository = mock(SchoolUserProfileRepository.class);
    private final StudentProfileRepository studentProfileRepository = mock(StudentProfileRepository.class);
    private final LearnerEnrollmentRepository learnerEnrollmentRepository = mock(LearnerEnrollmentRepository.class);
    private final SchoolClassRepository schoolClassRepository = mock(SchoolClassRepository.class);
    private final Principal principal = () -> "learner@example.com";

    private MySchoolService service;

    @BeforeEach
    void setUp() {
        service = new MySchoolService(
                currentUserService,
                schoolRepository,
                studentSchoolLinkRepository,
                schoolUserProfileRepository,
                studentProfileRepository,
                learnerEnrollmentRepository,
                schoolClassRepository
        );
    }

    @Test
    void requestJoinRejectsDuplicatePendingRequest() {
        UUID studentId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        User learner = activeUser(studentId, "Learner", "One");
        StudentSchoolLink existing = link(UUID.randomUUID(), studentId, schoolId, MySchoolService.STATUS_PENDING);
        when(currentUserService.requireUser(principal)).thenReturn(learner);
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId, "Bhekizulu SSS", "BSS")));
        when(studentSchoolLinkRepository.findByStudentId(studentId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.requestJoin(principal, schoolId))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("pending school request");
    }

    @Test
    void approveRequestLinksLearnerToAuthenticatedSchool() {
        UUID requestId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        UUID approverId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        StudentSchoolLink pending = link(requestId, studentId, schoolId, MySchoolService.STATUS_PENDING);
        User learner = activeUser(studentId, "John", "Doe");
        when(studentSchoolLinkRepository.findById(requestId)).thenReturn(Optional.of(pending));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId, "Bhekizulu SSS", "BSS")));
        when(currentUserService.requireUserById(studentId)).thenReturn(learner);
        when(schoolUserProfileRepository.findByUserIdAndDeletedFalse(studentId)).thenReturn(Optional.empty());
        when(studentProfileRepository.findByUserId(studentId)).thenReturn(Optional.empty());
        when(studentSchoolLinkRepository.save(any(StudentSchoolLink.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(schoolUserProfileRepository.save(any(SchoolUserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(learnerEnrollmentRepository.findBySchoolIdAndLearnerUserIdAndActiveTrue(schoolId, studentId)).thenReturn(List.of());

        SchoolLinkDtos.SchoolJoinRequestItemDto result = service.approveRequest(schoolId, approverId, requestId);

        assertThat(result.status()).isEqualTo(MySchoolService.STATUS_APPROVED);
        assertThat(pending.getApprovedBy()).isEqualTo(approverId);
        assertThat(pending.getGeneratedUsername()).startsWith("john");
        verify(schoolUserProfileRepository).save(any(SchoolUserProfile.class));
    }

    @Test
    void schoolCannotApproveAnotherSchoolsRequest() {
        UUID requestId = UUID.randomUUID();
        UUID owningSchoolId = UUID.randomUUID();
        UUID otherSchoolId = UUID.randomUUID();
        StudentSchoolLink pending = link(requestId, UUID.randomUUID(), owningSchoolId, MySchoolService.STATUS_PENDING);
        when(studentSchoolLinkRepository.findById(requestId)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.approveRequest(otherSchoolId, UUID.randomUUID(), requestId))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("your school");
    }

    @Test
    void rejectRequestStoresRejectedStatusWithoutLinkingLearner() {
        UUID requestId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        UUID rejectedBy = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        StudentSchoolLink pending = link(requestId, studentId, schoolId, MySchoolService.STATUS_PENDING);
        when(studentSchoolLinkRepository.findById(requestId)).thenReturn(Optional.of(pending));
        when(studentSchoolLinkRepository.save(any(StudentSchoolLink.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(activeSchool(schoolId, "Bhekizulu SSS", "BSS")));
        when(currentUserService.requireUserById(studentId)).thenReturn(activeUser(studentId, "Mary", "Smith"));

        SchoolLinkDtos.SchoolJoinRequestItemDto result = service.rejectRequest(schoolId, rejectedBy, requestId);

        assertThat(result.status()).isEqualTo(MySchoolService.STATUS_REJECTED);
        assertThat(pending.getRejectedBy()).isEqualTo(rejectedBy);
        assertThat(pending.getApprovedBy()).isNull();
    }

    private StudentSchoolLink link(UUID id, UUID studentId, UUID schoolId, String status) {
        StudentSchoolLink link = new StudentSchoolLink();
        link.setId(id);
        link.setStudentId(studentId);
        link.setSchoolId(schoolId);
        link.setSchoolCode("BSS");
        link.setStatus(status);
        link.setRequestedAt(OffsetDateTime.now());
        return link;
    }

    private School activeSchool(UUID id, String name, String code) {
        School school = new School();
        school.setId(id);
        school.setSchoolName(name);
        school.setSchoolCode(code);
        school.setRegistrationNumber("200000001");
        school.setStatus("ACTIVE");
        return school;
    }

    private User activeUser(UUID id, String firstName, String lastName) {
        User user = new User();
        user.setId(id);
        user.setEmail(firstName.toLowerCase() + "." + lastName.toLowerCase() + "@example.com");
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
