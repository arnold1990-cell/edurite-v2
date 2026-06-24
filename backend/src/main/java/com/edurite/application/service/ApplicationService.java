package com.edurite.application.service;

import com.edurite.application.entity.ApplicationRecord;
import com.edurite.application.repository.ApplicationRepository;
import com.edurite.bursary.entity.Bursary;
import com.edurite.bursary.repository.BursaryRepository;
import com.edurite.common.exception.ResourceConflictException;
import com.edurite.security.service.CurrentUserService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.service.StudentService;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

// @Service marks a class that contains business logic.
@Service
/**
 * This class named ApplicationService is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class ApplicationService {

    private final ApplicationRepository repository;
    private final BursaryRepository bursaryRepository;
    private final CurrentUserService currentUserService;
    private final StudentService studentService;

    public ApplicationService(ApplicationRepository repository, BursaryRepository bursaryRepository, CurrentUserService currentUserService, StudentService studentService) {
        this.repository = repository;
        this.bursaryRepository = bursaryRepository;
        this.currentUserService = currentUserService;
        this.studentService = studentService;
    }

    /**
     * this method handles the "submit" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public ApplicationRecord submit(UUID bursaryId, Principal principal) {
        Bursary bursary = bursaryRepository.findById(bursaryId).orElseThrow(() -> new ResourceConflictException("Bursary not found"));
        if (bursary.getDeletedAt() != null || !"ACTIVE".equalsIgnoreCase(bursary.getStatus())) {
            throw new ResourceConflictException("This bursary is not open for applications");
        }
        StudentProfile profile = requireStudent(principal);
        ApplicationRecord record = new ApplicationRecord();
        record.setBursaryId(bursaryId);
        record.setStudentId(profile.getId());
        record.setStatus("SUBMITTED");
        return repository.save(record);
    }

    /**
     * this method handles the "listMine" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public List<ApplicationRecord> listMine(Principal principal) {
        return repository.findByStudentIdOrderByCreatedAtDesc(requireStudent(principal).getId());
    }

    /**
     * this method handles the "requireStudent" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private StudentProfile requireStudent(Principal principal) {
        currentUserService.requireUser(principal);
        return studentService.getProfileEntity(principal);
    }
}

