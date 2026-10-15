package com.edurite.roadmap.service;

import com.edurite.career.repository.CareerRepository;
import com.edurite.common.exception.ResourceConflictException;
import com.edurite.student.service.StudentContextService;
import com.edurite.student.repository.StudentProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.security.Principal;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CareerSelectionService {
    private final CareerRepository careers;
    private final StudentContextService context;
    private final StudentProfileRepository profiles;
    private final ObjectMapper mapper;
    @org.springframework.beans.factory.annotation.Autowired
    private com.edurite.roadmap.repository.CareerProgramRequirementRepository programmes;
    public CareerSelectionService(CareerRepository careers, StudentContextService context, StudentProfileRepository profiles, ObjectMapper mapper) {
        this.careers = careers; this.context = context; this.profiles = profiles; this.mapper = mapper;
    }
    public String requireCareer(String name) {
        return careers.findFirstByTitleIgnoreCase(name == null ? "" : name.trim())
            .orElseThrow(() -> new ResourceConflictException("Choose a supported career from the catalogue.")).getTitle();
    }
    @Transactional
    public Map<String, String> selected(Principal principal, String name) {
        var profile = context.requireStudent(principal);
        try {
            ObjectNode preferences = readPreferences(profile.getPreferencesJson());
            if (name != null) {
                if (!name.equalsIgnoreCase(preferences.path("selectedCareer").asText(""))) preferences.remove("selectedProgramme");
                if (name.isBlank()) preferences.remove("selectedCareer");
                else preferences.put("selectedCareer", requireCareer(name));
                profile.setPreferencesJson(preferences.toString());
                profiles.save(profile);
            }
            return Map.of("careerName", preferences.path("selectedCareer").asText(""), "programmeId", preferences.path("selectedProgramme").asText(""));
        } catch (com.fasterxml.jackson.core.JsonProcessingException | ClassCastException ex) {
            throw new ResourceConflictException("Your saved preferences could not be read. Nothing was changed.");
        }
    }
    @Transactional
    public Map<String, String> programme(Principal principal, java.util.UUID id) {
        var profile = context.requireStudent(principal);
        try {
            ObjectNode preferences = readPreferences(profile.getPreferencesJson());
            if (id == null) preferences.remove("selectedProgramme");
            else {
                var programme = programmes.findById(id).orElseThrow(() -> new ResourceConflictException("Programme not found"));
                if (!programme.getCareerName().equalsIgnoreCase(preferences.path("selectedCareer").asText("")))
                    throw new ResourceConflictException("Choose a programme belonging to your selected career.");
                preferences.put("selectedProgramme", id.toString());
            }
            profile.setPreferencesJson(preferences.toString()); profiles.save(profile);
            return selected(principal, null);
        } catch (com.fasterxml.jackson.core.JsonProcessingException | ClassCastException ex) {
            throw new ResourceConflictException("Your saved preferences could not be read. Nothing was changed.");
        }
    }
    private ObjectNode readPreferences(String stored) throws com.fasterxml.jackson.core.JsonProcessingException {
        if (stored == null || stored.isBlank()) return mapper.createObjectNode();
        var parsed = mapper.readTree(stored);
        if (parsed == null || parsed.isNull()) return mapper.createObjectNode();
        if (!parsed.isObject()) throw new ResourceConflictException("Your saved preferences could not be read. Nothing was changed.");
        return (ObjectNode) parsed;
    }
}
