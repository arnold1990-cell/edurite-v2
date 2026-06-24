package com.edurite.student.service;

import com.edurite.student.entity.StudentProfile;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class StudentProfileCompletionService {

    private static final int PROFILE_COMPLETION_THRESHOLD = 60;

    public int calculateCompleteness(StudentProfile profile) {
        int score = 0;
        if (notBlank(profile.getFirstName())) score += 10;
        if (notBlank(profile.getLastName())) score += 10;
        if (notBlank(profile.getPhone())) score += 10;
        if (profile.getDateOfBirth() != null && profile.getDateOfBirth().isBefore(LocalDate.now())) score += 10;
        if (notBlank(profile.getQualificationLevel())) score += 10;
        if (notBlank(profile.getSkills())) score += 15;
        if (notBlank(profile.getInterests())) score += 10;
        if (notBlank(profile.getCvFileUrl())) score += 15;
        if (notBlank(profile.getTranscriptFileUrl())) score += 10;
        return score;
    }

    public boolean isProfileCompleted(StudentProfile profile) {
        return calculateCompleteness(profile) >= PROFILE_COMPLETION_THRESHOLD;
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}

