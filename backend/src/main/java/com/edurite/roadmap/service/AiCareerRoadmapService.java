package com.edurite.roadmap.service;

import com.edurite.ai.service.AiProviderOrchestratorService;
import com.edurite.roadmap.dto.CareerRoadmapDtos.CareerOverview;
import com.edurite.roadmap.dto.CareerRoadmapDtos.CareerRoadmapGenerateRequest;
import com.edurite.roadmap.dto.CareerRoadmapDtos.CareerRoadmapGenerateResponse;
import com.edurite.roadmap.dto.CareerRoadmapDtos.PathwayStep;
import com.edurite.roadmap.dto.CareerRoadmapDtos.RoadmapTimelineStage;
import com.edurite.roadmap.dto.CareerRoadmapDtos.StudyPlanStep;
import com.edurite.roadmap.dto.CareerRoadmapDtos.SubjectRequirement;
import com.edurite.student.entity.StudentProfile;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class AiCareerRoadmapService {

    private final AiProviderOrchestratorService aiProviderOrchestratorService;
    private final ObjectMapper objectMapper;

    public AiCareerRoadmapService(
            AiProviderOrchestratorService aiProviderOrchestratorService,
            ObjectMapper objectMapper
    ) {
        this.aiProviderOrchestratorService = aiProviderOrchestratorService;
        this.objectMapper = objectMapper;
    }

    public CareerRoadmapGenerateResponse generate(
            CareerRoadmapGenerateRequest request,
            StudentProfile profile,
            String roadmapContext,
            String universityRequirementsContext,
            Integer learnerAps
    ) {
        try {
            String prompt = """
                    You are generating a South African learner career roadmap.
                    Return ONLY strict JSON. No markdown, no prose outside JSON, no code fences.

                    Required JSON schema:
                    {
                      "careerName": "string",
                      "overview": {
                        "description": "string",
                        "dailyResponsibilities": ["string"],
                        "skillsNeeded": ["string"],
                        "careerDemand": "string",
                        "salaryRange": "string",
                        "professionalBody": "string"
                      },
                      "requiredSubjects": [{"subject":"string","minimumLevel":5,"minimumPass":"Level 5","suggestedMark":"60%+","required":true,"notes":"string"}],
                      "recommendedSubjects": [{"subject":"string","minimumLevel":4,"minimumPass":"Level 4","suggestedMark":"50%+","required":false,"notes":"string"}],
                      "universityPathway": [{"title":"string","description":"string"}],
                      "professionalPathway": [{"title":"string","description":"string"}],
                      "roadmapTimeline": [{"stage":1,"title":"string","description":"string"}],
                      "universityRequirements": [],
                      "apsReadiness": {"learnerAps":31,"requiredAps":34,"apsGap":3,"readinessScore":72,"status":"Almost Eligible","bestFitUniversities":2},
                      "gapAnalysis": {
                        "currentAps":31,
                        "requiredAps":34,
                        "apsGap":3,
                        "missingSubjects":["string"],
                        "subjectsNeedingImprovement":["string"],
                        "bestFitUniversities":["string"],
                        "riskLevel":"Medium",
                        "improvementSuggestions":["string"]
                      },
                      "alternativePathways": ["string"],
                      "studyPlan": [{"title":"string","focus":"string","actions":["string"]}]
                    }

                    Context:
                    Career: %s
                    Learner grade: %s
                    Learner province: %s
                    Learner APS: %s
                    Learner subjects: %s
                    Learner interests/career goals: %s

                    Known career roadmap context:
                    %s

                    Known university requirement context:
                    %s

                    Constraints:
                    - Focus on South Africa.
                    - Do not invent official verified claims. If unsure, keep wording cautious.
                    - University requirements array can be empty because the application will merge structured requirement data separately.
                    - Keep lists concise and useful.
                    """.formatted(
                    safe(request.careerName()),
                    safe(request.grade()),
                    safe(request.province()),
                    learnerAps == null ? "" : learnerAps,
                    safe(subjectSummary(request)),
                    safe(profile == null ? null : profile.getCareerGoals()),
                    safe(roadmapContext),
                    safe(universityRequirementsContext)
            );
            String raw = aiProviderOrchestratorService.generateContent(prompt);
            AiCareerRoadmapPayload payload = objectMapper.readValue(raw, AiCareerRoadmapPayload.class);
            return toResponse(payload);
        } catch (Exception ex) {
            return null;
        }
    }

    private CareerRoadmapGenerateResponse toResponse(AiCareerRoadmapPayload payload) {
        if (payload == null) {
            return null;
        }
        return new CareerRoadmapGenerateResponse(
                safe(payload.careerName()),
                payload.overview() == null
                        ? new CareerOverview("", List.of(), List.of(), "", "", "")
                        : new CareerOverview(
                                safe(payload.overview().description()),
                                list(payload.overview().dailyResponsibilities()),
                                list(payload.overview().skillsNeeded()),
                                safe(payload.overview().careerDemand()),
                                safe(payload.overview().salaryRange()),
                                safe(payload.overview().professionalBody())
                        ),
                mapSubjects(payload.requiredSubjects()),
                mapSubjects(payload.recommendedSubjects()),
                mapSteps(payload.universityPathway()),
                mapSteps(payload.professionalPathway()),
                mapTimeline(payload.roadmapTimeline()),
                List.of(),
                null,
                null,
                list(payload.alternativePathways()),
                mapStudyPlan(payload.studyPlan())
        );
    }

    private List<SubjectRequirement> mapSubjects(List<AiSubjectRequirement> items) {
        if (items == null) {
            return List.of();
        }
        List<SubjectRequirement> mapped = new ArrayList<>();
        for (AiSubjectRequirement item : items) {
            if (item == null || safe(item.subject()).isBlank()) {
                continue;
            }
            mapped.add(new SubjectRequirement(
                    safe(item.subject()),
                    item.minimumLevel(),
                    safe(item.minimumPass()),
                    safe(item.suggestedMark()),
                    item.required(),
                    safe(item.notes())
            ));
        }
        return mapped;
    }

    private List<PathwayStep> mapSteps(List<AiStep> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .filter(item -> item != null && !safe(item.title()).isBlank())
                .map(item -> new PathwayStep(safe(item.title()), safe(item.description())))
                .toList();
    }

    private List<RoadmapTimelineStage> mapTimeline(List<AiTimelineStage> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .filter(item -> item != null && !safe(item.title()).isBlank())
                .map(item -> new RoadmapTimelineStage(item.stage(), safe(item.title()), safe(item.description())))
                .toList();
    }

    private List<StudyPlanStep> mapStudyPlan(List<AiStudyPlanStep> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .filter(item -> item != null && !safe(item.title()).isBlank())
                .map(item -> new StudyPlanStep(safe(item.title()), safe(item.focus()), list(item.actions())))
                .toList();
    }

    private String subjectSummary(CareerRoadmapGenerateRequest request) {
        if (request.subjects() == null || request.subjects().isEmpty()) {
            return "";
        }
        return request.subjects().stream()
                .filter(item -> item != null && item.subjectName() != null)
                .map(item -> {
                    String level = item.level() == null ? "" : " level " + item.level();
                    String mark = item.markPercentage() == null ? "" : " " + item.markPercentage() + "%";
                    return item.subjectName() + mark + level;
                })
                .toList()
                .toString();
    }

    private List<String> list(List<String> values) {
        return values == null ? List.of() : values.stream().filter(value -> value != null && !value.isBlank()).toList();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AiCareerRoadmapPayload(
            String careerName,
            AiOverview overview,
            List<AiSubjectRequirement> requiredSubjects,
            List<AiSubjectRequirement> recommendedSubjects,
            List<AiStep> universityPathway,
            List<AiStep> professionalPathway,
            List<AiTimelineStage> roadmapTimeline,
            List<String> alternativePathways,
            List<AiStudyPlanStep> studyPlan
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AiOverview(
            String description,
            List<String> dailyResponsibilities,
            List<String> skillsNeeded,
            String careerDemand,
            String salaryRange,
            String professionalBody
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AiSubjectRequirement(
            String subject,
            Integer minimumLevel,
            String minimumPass,
            String suggestedMark,
            boolean required,
            String notes
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AiStep(String title, String description) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AiTimelineStage(Integer stage, String title, String description) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AiStudyPlanStep(String title, String focus, List<String> actions) {}
}
