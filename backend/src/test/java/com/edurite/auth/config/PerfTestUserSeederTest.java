package com.edurite.auth.config;

import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.user.entity.Role;
import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import com.edurite.user.repository.RoleRepository;
import com.edurite.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerfTestUserSeederTest {

    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StudentProfileRepository studentProfileRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private final PerfTestUserSeeder seeder = new PerfTestUserSeeder();

    @Test
    void createsSyntheticStudentUsersWithEncodedPasswordAndProfile() {
        Role studentRole = role("ROLE_STUDENT");
        studentRole.setId(UUID.randomUUID());
        when(roleRepository.findByName("ROLE_STUDENT")).thenReturn(Optional.of(studentRole));
        when(roleRepository.findById(studentRole.getId())).thenReturn(Optional.of(studentRole));
        when(userRepository.findAllByLowerEmailIn(List.of("perf-user-1@example.test"))).thenReturn(List.of());
        when(passwordEncoder.encode("PerfTest@12345")).thenReturn("encoded-password");
        when(userRepository.saveAll(anyCollection())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Iterable<User> users = invocation.getArgument(0);
            List<User> savedUsers = toList(users);
            User user = savedUsers.getFirst();
            user.setId(UUID.randomUUID());
            return savedUsers;
        });
        when(studentProfileRepository.saveAll(anyCollection())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Iterable<StudentProfile> profiles = invocation.getArgument(0);
            return toList(profiles);
        });
        when(studentProfileRepository.findByUserIdIn(anyCollection())).thenReturn(List.of());

        seeder.seed(
                roleRepository,
                userRepository,
                studentProfileRepository,
                passwordEncoder,
                1,
                "perf-user",
                "PerfTest@12345"
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<User>> userCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(userRepository).saveAll(userCaptor.capture());
        User saved = toList(userCaptor.getValue()).getFirst();
        assertThat(saved.getEmail()).isEqualTo("perf-user-1@example.test");
        assertThat(saved.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(saved.isEmailVerified()).isTrue();
        assertThat(saved.isMustChangePassword()).isFalse();
        assertThat(saved.getRoles()).anyMatch(role -> "ROLE_STUDENT".equals(role.getName()));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<StudentProfile>> profileCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(studentProfileRepository).saveAll(profileCaptor.capture());
        assertThat(toList(profileCaptor.getValue()).getFirst().getUserId()).isEqualTo(saved.getId());
    }

    @Test
    void doesNotRewriteExistingSyntheticUserWhenAlreadyValid() {
        Role studentRole = role("ROLE_STUDENT");
        studentRole.setId(UUID.randomUUID());
        User existing = new User();
        existing.setId(UUID.randomUUID());
        existing.setEmail("perf-user-1@example.test");
        existing.setFirstName("Perf");
        existing.setLastName("User 1");
        existing.setPasswordHash("encoded-password");
        existing.setStatus(UserStatus.ACTIVE);
        existing.setEmailVerified(true);
        existing.setMustChangePassword(false);
        existing.getRoles().add(studentRole);

        when(roleRepository.findByName("ROLE_STUDENT")).thenReturn(Optional.of(studentRole));
        when(roleRepository.findById(studentRole.getId())).thenReturn(Optional.of(studentRole));
        when(userRepository.findAllByLowerEmailIn(List.of("perf-user-1@example.test"))).thenReturn(List.of(existing));
        StudentProfile profile = new StudentProfile();
        profile.setUserId(existing.getId());
        when(studentProfileRepository.findByUserIdIn(List.of(existing.getId()))).thenReturn(List.of(profile));
        when(passwordEncoder.encode("PerfTest@12345")).thenReturn("encoded-password");

        seeder.seed(
                roleRepository,
                userRepository,
                studentProfileRepository,
                passwordEncoder,
                1,
                "perf-user",
                "PerfTest@12345"
        );

        verify(userRepository, never()).saveAll(anyCollection());
        verify(studentProfileRepository, never()).save(any(StudentProfile.class));
    }

    @Test
    void rejectsUserCountAboveMaximum() {
        assertThatThrownBy(() -> seeder.seed(
                roleRepository,
                userRepository,
                studentProfileRepository,
                passwordEncoder,
                50_001,
                "perf-user",
                "PerfTest@12345"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PERF_TEST_USER_COUNT");
    }

    private Role role(String name) {
        Role role = new Role();
        role.setName(name);
        return role;
    }

    private static <T> List<T> toList(Iterable<T> values) {
        java.util.ArrayList<T> list = new java.util.ArrayList<>();
        values.forEach(list::add);
        return list;
    }
}
