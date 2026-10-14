package com.edurite.student.service;

import com.edurite.common.exception.ResourceConflictException;
import com.edurite.course.repository.CourseRepository;
import com.edurite.career.repository.CareerRepository;
import com.edurite.student.dto.StudentProfileAssociationDto;
import com.edurite.student.dto.StudentProfileAssociationDto.Type;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.StudentProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Learner-owned associations, stored alongside the existing profile preferences. */
@Service
public class StudentProfileAssociationService {
    private final StudentContextService context;
    private final com.edurite.security.service.CurrentUserService currentUser;
    private final StudentProfileRepository profiles;
    private final CourseRepository courses;
    private final CareerRepository careers;
    private final ObjectMapper mapper;
    @org.springframework.beans.factory.annotation.Autowired
    private com.edurite.roadmap.repository.CareerProgramRequirementRepository programmes;

    public StudentProfileAssociationService(StudentContextService context, com.edurite.security.service.CurrentUserService currentUser, StudentProfileRepository profiles,
            CourseRepository courses, CareerRepository careers, ObjectMapper mapper) {
        this.context = context; this.currentUser = currentUser; this.profiles = profiles; this.courses = courses; this.careers = careers; this.mapper = mapper;
    }

    @Transactional
    public List<StudentProfileAssociationDto> list(Principal principal) {
        return associations(preferences(context.requireStudent(principal)));
    }

    @Transactional
    public List<StudentProfileAssociationDto> link(Principal principal, Type type, UUID targetId) {
        String title = switch (type) {
            case COURSE -> courses.findById(targetId).orElseThrow(() -> missing("Course")).getName();
            case CAREER -> careers.findById(targetId).orElseThrow(() -> missing("Career")).getTitle();
            case PROGRAMME -> {
                var programme = programmes.findById(targetId).orElseThrow(() -> missing("Programme"));
                yield programme.getInstitutionName() + " - " + programme.getQualificationName();
            }
        };
        StudentProfile profile = lockedProfile(principal);
        ObjectNode preferences = preferences(profile);
        List<StudentProfileAssociationDto> links = associations(preferences);
        if (links.stream().anyMatch(item -> item.type() == type && item.targetId().equals(targetId))) return links;
        String careerName = type == Type.PROGRAMME ? programmes.findById(targetId).orElseThrow(() -> missing("Programme")).getCareerName() : null;
        links.add(new StudentProfileAssociationDto(type, targetId, title, OffsetDateTime.now().toString(), careerName));
        return save(profile, preferences, links);
    }

    @Transactional
    public List<StudentProfileAssociationDto> unlink(Principal principal, Type type, UUID targetId) {
        StudentProfile profile = lockedProfile(principal);
        ObjectNode preferences = preferences(profile);
        List<StudentProfileAssociationDto> links = associations(preferences);
        links.removeIf(item -> item.type() == type && item.targetId().equals(targetId));
        return save(profile, preferences, links);
    }

    private StudentProfile lockedProfile(Principal principal) {
        UUID userId = currentUser.requireUser(principal).getId();
        return profiles.findByUserIdForUpdate(userId).orElseGet(() -> context.requireStudent(principal));
    }

    private List<StudentProfileAssociationDto> save(StudentProfile profile, ObjectNode preferences, List<StudentProfileAssociationDto> links) {
        preferences.set("profileAssociations", mapper.valueToTree(links));
        profile.setPreferencesJson(preferences.toString());
        profiles.save(profile);
        return links;
    }

    private ObjectNode preferences(StudentProfile profile) {
        try {
            var json = mapper.readTree(profile.getPreferencesJson() == null ? "{}" : profile.getPreferencesJson());
            if (!(json instanceof ObjectNode object)) throw new IllegalArgumentException();
            return object;
        } catch (Exception ex) {
            throw new ResourceConflictException("Your profile preferences could not be read. Please contact support; nothing was changed.");
        }
    }

    private List<StudentProfileAssociationDto> associations(ObjectNode preferences) {
        var items = preferences.get("profileAssociations");
        if (items == null) return new ArrayList<>();
        try {
            return new ArrayList<>(mapper.readerForListOf(StudentProfileAssociationDto.class).readValue(items));
        } catch (Exception ex) {
            throw new ResourceConflictException("Your linked courses and careers could not be read. Please try again or contact support.");
        }
    }

    private ResponseStatusException missing(String name) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, name + " is no longer available.");
    }
}
