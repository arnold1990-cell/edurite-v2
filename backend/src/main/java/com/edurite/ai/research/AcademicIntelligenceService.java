package com.edurite.ai.research;

import static com.edurite.ai.research.CareerResearchDtos.*;

import com.edurite.student.dto.*;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AcademicIntelligenceService {
    public AcademicAnalysis analyse(StudentProfileDto profile) {
        return analyse(profile, List.of());
    }

    public AcademicAnalysis analyse(
            StudentProfileDto profile, List<StudentSubjectAchievementDto> history) {
        var subjects =
                profile.subjectAchievements() == null
                        ? List.<StudentSubjectAchievementDto>of()
                        : profile.subjectAchievements();
        var marks =
                subjects.stream()
                        .filter(
                                s ->
                                        s.markPercentage() != null
                                                && s.markPercentage() >= 0
                                                && s.markPercentage() <= 100)
                        .toList();
        var contexts =
                marks.stream()
                        .map(
                                s ->
                                        Objects.toString(s.curriculum(), "unknown")
                                                + "|"
                                                + Objects.toString(s.academicYear(), "unknown")
                                                + "|"
                                                + Objects.toString(s.examination(), "unknown")
                                                + "|"
                                                + Objects.toString(s.gradingScale(), "unknown"))
                        .distinct()
                        .toList();
        List<String> limits = new ArrayList<>();
        boolean duplicateSubjects =
                marks.stream()
                                .map(
                                        s ->
                                                Objects.toString(s.subjectName(), "")
                                                        .strip()
                                                        .toLowerCase(Locale.ROOT))
                                .distinct()
                                .count()
                        != marks.size();
        if (duplicateSubjects)
            limits.add(
                    "Duplicate subject results need review. No combined average or strength ranking"
                            + " is inferred.");
        if (contexts.size() > 1)
            limits.add(
                    "Results include different curricula or examination periods. No combined"
                            + " average or cross-period strength ranking is inferred.");
        if (marks.size() < subjects.size())
            limits.add(
                    "Some subjects have levels or missing marks. Levels are not converted to"
                            + " percentages.");
        if (subjects.stream().anyMatch(s -> !s.verified()))
            limits.add(
                    "Some results are self-reported or awaiting transcript review; eligibility is"
                            + " provisional.");
        if (subjects.isEmpty())
            limits.add(
                    "No academic results have been recorded. Career potential can still be explored"
                            + " through interests and goals.");
        var ordered =
                contexts.size() <= 1 && !duplicateSubjects
                        ? marks.stream()
                                .sorted(
                                        Comparator.comparing(
                                                        StudentSubjectAchievementDto
                                                                ::markPercentage)
                                                .reversed()
                                                .thenComparing(
                                                        StudentSubjectAchievementDto::subjectName))
                                .toList()
                        : List.<StudentSubjectAchievementDto>of();
        var weakest = new ArrayList<>(ordered);
        Collections.reverse(weakest);
        // Relative priorities are not pass/fail labels or a judgement of career potential.
        var strengths =
                ordered.isEmpty()
                        ? ordered
                        : ordered.stream()
                                .filter(
                                        s ->
                                                s.markPercentage()
                                                        .equals(
                                                                ordered.getFirst()
                                                                        .markPercentage()))
                                .toList();
        var priorities =
                weakest.isEmpty()
                        ? List.<StudentSubjectAchievementDto>of()
                        : weakest.stream()
                                .filter(
                                        s ->
                                                s.markPercentage()
                                                                .equals(
                                                                        weakest.getFirst()
                                                                                .markPercentage())
                                                        && !strengths.contains(s))
                                .toList();
        return new AcademicAnalysis(
                profile.selectedGrade(),
                contexts.size() <= 1 && !duplicateSubjects && !marks.isEmpty()
                        ? marks.stream()
                                .mapToInt(StudentSubjectAchievementDto::markPercentage)
                                .average()
                                .orElseThrow()
                        : null,
                subjects,
                strengths,
                priorities,
                subjects.stream()
                        .map(StudentSubjectAchievementDto::curriculum)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList(),
                profile.interests() == null ? List.of() : profile.interests(),
                profile.skills() == null ? List.of() : profile.skills(),
                profile.careerGoals(),
                List.copyOf(limits),
                progression(subjects, history));
    }

    private List<AcademicProgression> progression(
            List<StudentSubjectAchievementDto> current,
            List<StudentSubjectAchievementDto> history) {
        List<AcademicProgression> changes = new ArrayList<>();
        for (var subject : current) {
            if (!comparable(subject)) continue;
            var candidates =
                    history.stream()
                            .filter(this::comparable)
                            .filter(
                                    old ->
                                            old.subjectName()
                                                            .equalsIgnoreCase(subject.subjectName())
                                                    && old.curriculum()
                                                            .equalsIgnoreCase(subject.curriculum())
                                                    && old.gradingScale()
                                                            .equalsIgnoreCase(
                                                                    subject.gradingScale())
                                                    && old.examination()
                                                            .equalsIgnoreCase(subject.examination())
                                                    && old.academicYear()
                                                                    .compareTo(
                                                                            subject.academicYear())
                                                            < 0)
                            .toList();
            String latest =
                    candidates.stream()
                            .map(StudentSubjectAchievementDto::academicYear)
                            .max(String::compareTo)
                            .orElse(null);
            var latestMarks =
                    candidates.stream()
                            .filter(s -> Objects.equals(s.academicYear(), latest))
                            .toList();
            var older =
                    latestMarks.stream()
                                            .map(StudentSubjectAchievementDto::markPercentage)
                                            .distinct()
                                            .count()
                                    == 1
                            ? latestMarks.getFirst()
                            : null;
            if (older != null)
                changes.add(
                        new AcademicProgression(
                                subject.subjectName(),
                                subject.curriculum(),
                                older.academicYear(),
                                subject.academicYear(),
                                older.markPercentage(),
                                subject.markPercentage(),
                                subject.markPercentage() - older.markPercentage()));
        }
        return List.copyOf(changes);
    }

    private boolean comparable(StudentSubjectAchievementDto s) {
        return s != null
                && s.subjectName() != null
                && !s.subjectName().isBlank()
                && s.markPercentage() != null
                && s.markPercentage() >= 0
                && s.markPercentage() <= 100
                && s.curriculum() != null
                && !s.curriculum().isBlank()
                && s.gradingScale() != null
                && !s.gradingScale().isBlank()
                && s.examination() != null
                && !s.examination().isBlank()
                && s.academicYear() != null
                && s.academicYear().matches("\\d{4}");
    }
}
