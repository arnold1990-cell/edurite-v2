package com.edurite.student.service;

import com.edurite.security.service.CurrentUserService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.user.entity.User;
import java.security.Principal;
import org.springframework.stereotype.Service;

@Service
public class StudentContextService {

    private final CurrentUserService currentUserService;
    private final StudentProfileRepository studentProfileRepository;

    public StudentContextService(CurrentUserService currentUserService, StudentProfileRepository studentProfileRepository) {
        this.currentUserService = currentUserService;
        this.studentProfileRepository = studentProfileRepository;
    }

    public StudentProfile requireStudent(Principal principal) {
        User user = currentUserService.requireUser(principal);
        return studentProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> createDefaultProfile(user));
    }

    private StudentProfile createDefaultProfile(User user) {
        StudentProfile profile = new StudentProfile();
        profile.setUserId(user.getId());
        profile.setFirstName(user.getFirstName());
        profile.setLastName(user.getLastName());
        profile.setProfileCompleted(false);
        profile.setInAppNotificationsEnabled(true);
        profile.setEmailNotificationsEnabled(false);
        profile.setSmsNotificationsEnabled(false);
        profile.setPreferencesJson("{}");
        return studentProfileRepository.save(profile);
    }
}

