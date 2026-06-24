package com.edurite.student.service;

import com.edurite.common.exception.ResourceConflictException;
import com.edurite.security.service.CurrentUserService;
import com.edurite.student.dto.StudentPreferencesDto;
import com.edurite.student.entity.StudentPreference;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.StudentPreferenceRepository;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.user.entity.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentPreferenceService {

    private final CurrentUserService currentUserService;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentPreferenceRepository studentPreferenceRepository;
    private final ObjectMapper objectMapper;

    public StudentPreferenceService(
            CurrentUserService currentUserService,
            StudentProfileRepository studentProfileRepository,
            StudentPreferenceRepository studentPreferenceRepository,
            ObjectMapper objectMapper
    ) {
        this.currentUserService = currentUserService;
        this.studentProfileRepository = studentProfileRepository;
        this.studentPreferenceRepository = studentPreferenceRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public StudentPreferencesDto get(Principal principal) {
        StudentPreference preference = getOrCreate(principal);
        return toDto(preference);
    }

    @Transactional
    public StudentPreferencesDto update(Principal principal, StudentPreferencesDto request) {
        StudentPreference preference = getOrCreate(principal);
        preference.setPreferredIndustries(join(request.preferredIndustries()));
        preference.setPreferredLocations(join(request.preferredLocations()));
        preference.setNotificationPreferences(writeJson(request.notificationPreferences()));
        preference.setExtra(writeJson(request.extra()));
        studentPreferenceRepository.save(preference);
        return toDto(preference);
    }

    private StudentPreference getOrCreate(Principal principal) {
        User user = currentUserService.requireUser(principal);
        StudentProfile profile = studentProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceConflictException("Student profile not found."));
        return studentPreferenceRepository.findByStudentId(profile.getId()).orElseGet(() -> {
            StudentPreference preference = new StudentPreference();
            preference.setStudentId(profile.getId());
            preference.setNotificationPreferences("{}");
            preference.setExtra("{}");
            return studentPreferenceRepository.save(preference);
        });
    }

    private StudentPreferencesDto toDto(StudentPreference preference) {
        return new StudentPreferencesDto(
                split(preference.getPreferredIndustries()),
                split(preference.getPreferredLocations()),
                readJson(preference.getNotificationPreferences()),
                readJson(preference.getExtra())
        );
    }

    private String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return String.join(",", values.stream().map(item -> item == null ? "" : item.trim()).filter(item -> !item.isBlank()).toList());
    }

    private List<String> split(String values) {
        if (values == null || values.isBlank()) {
            return List.of();
        }
        return Arrays.stream(values.split(",")).map(String::trim).filter(item -> !item.isBlank()).toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readJson(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    private String writeJson(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new ResourceConflictException("Could not store student preferences.");
        }
    }
}

