package com.edurite.subscription.service;

import java.util.List;
import org.springframework.util.AntPathMatcher;

/** API aliases and browser routes share this server-owned policy. Role checks remain separate. */
public final class StudentAccessPolicy {
    private StudentAccessPolicy() {}
    public record ApiRule(String method, String path, Feature feature, boolean ai) {}
    public record RouteRule(String path, String section, Feature feature) {}
    private static final AntPathMatcher MATCHER = new AntPathMatcher();
    public static final List<ApiRule> API = List.of(
        new ApiRule("*", "/student/psychometric/**", Feature.CAREER_ASSESSMENT, false),
        new ApiRule("*", "/public/psychometric/**", Feature.CAREER_ASSESSMENT, false),
        new ApiRule("POST", "/student/career-roadmaps/generate", Feature.CAREER_ROADMAP_PERSONALISED, true),
        new ApiRule("*", "/student/career-roadmaps/saved", Feature.CAREER_ROADMAP_PERSONALISED, false),
        new ApiRule("*", "/student/career-roadmaps/save", Feature.CAREER_ROADMAP_PERSONALISED, false),
        new ApiRule("*", "/student/career-roadmaps/requirements", Feature.STUDY_RECOMMENDATIONS, false),
        new ApiRule("*", "/student/university-applications/**", Feature.APPLICATION_SUPPORT, false),
        new ApiRule("POST", "/student/scholarship-applications/*/motivation-letter", Feature.AI_OPPORTUNITY_ASSISTANT, true),
        new ApiRule("*", "/student/scholarship-applications/**", Feature.APPLICATION_SUPPORT, false),
        new ApiRule("GET", "/student/cv/ai-suggestions", Feature.AI_OPPORTUNITY_ASSISTANT, true),
        new ApiRule("*", "/student/cv/**", Feature.APPLICATION_SUPPORT, false),
        new ApiRule("POST", "/bursaries/*/applications", Feature.APPLICATION_SUPPORT, false),
        new ApiRule("*", "/applications/**", Feature.APPLICATION_SUPPORT, false),
        new ApiRule("*", "/student/opportunities/*/*/save", Feature.SAVE_OPPORTUNITIES, false),
        new ApiRule("*", "/student/careers/*/save", Feature.SAVE_OPPORTUNITIES, false),
        new ApiRule("*", "/student/bursaries/*/save", Feature.SAVE_OPPORTUNITIES, false),
        new ApiRule("*", "/student/careers/saved", Feature.SAVE_OPPORTUNITIES, false),
        new ApiRule("*", "/student/bursaries/saved", Feature.SAVE_OPPORTUNITIES, false),
        new ApiRule("*", "/student/bursaries/bookmarks", Feature.SAVE_OPPORTUNITIES, false),
        new ApiRule("*", "/recommendations/**", Feature.PERSONALISED_CAREER_RECOMMENDATIONS, false),
        new ApiRule("*", "/bursaries/recommendations/**", Feature.PRIORITY_OPPORTUNITY_MATCHING, false),
        new ApiRule("*", "/ai/bursary-guidance/me", Feature.PRIORITY_OPPORTUNITY_MATCHING, false),
        new ApiRule("POST", "/ai/career-advice", Feature.PERSONALISED_CAREER_RECOMMENDATIONS, true),
        new ApiRule("GET", "/ai/career-advice/me", Feature.PERSONALISED_CAREER_RECOMMENDATIONS, true),
        new ApiRule("POST", "/ai/analyse-university-sources", Feature.ADVANCED_MATCHING, true),
        new ApiRule("GET", "/ai/dashboard-summary", Feature.PROGRESS_ADVANCED, true),
        new ApiRule("GET", "/ai/gemini-health", Feature.AI_SUPPORT, true),
        new ApiRule("POST", "/student/tutor/ask", Feature.AI_SUPPORT, true),
        new ApiRule("*", "/student/learning-centre/recommended", Feature.LEARNING_RESOURCES_FULL, false),
        new ApiRule("*", "/learning-centre/recommended", Feature.LEARNING_RESOURCES_FULL, false)
    );
    public static final List<RouteRule> ROUTES = List.of(
        new RouteRule("/student/career-explorer", "career-path", Feature.CAREER_ROADMAP_PERSONALISED),
        new RouteRule("/student/career-explorer", "learning-path", Feature.CAREER_ROADMAP_PERSONALISED),
        new RouteRule("/student/career-explorer", "readiness", Feature.ACADEMIC_ANALYSIS),
        new RouteRule("/student/career-explorer", "guidance", Feature.PERSONALISED_CAREER_RECOMMENDATIONS),
        new RouteRule("/student/career-explorer", "career-match", Feature.CAREER_ASSESSMENT),
        new RouteRule("/student/career-explorer", "interests", Feature.CAREER_ASSESSMENT),
        new RouteRule("/student/career-explorer", "saved", Feature.CAREER_ROADMAP_PERSONALISED),
        new RouteRule("/student/profile", "cv", Feature.APPLICATION_SUPPORT),
        new RouteRule("/student/learning", "guidance", Feature.PERSONALISED_CAREER_RECOMMENDATIONS),
        new RouteRule("/student/learning", "study-plan", Feature.CAREER_ROADMAP_PERSONALISED),
        new RouteRule("/student/funding", "applications", Feature.APPLICATION_SUPPORT),
        new RouteRule("/student/funding", "scholarships", Feature.APPLICATION_SUPPORT),
        new RouteRule("/student/funding", "saved", Feature.SAVE_OPPORTUNITIES),
        new RouteRule("/student/funding", "matches", Feature.PRIORITY_OPPORTUNITY_MATCHING),
        new RouteRule("/student/institutions", "applications", Feature.APPLICATION_SUPPORT),
        new RouteRule("/student/institutions", "saved", Feature.APPLICATION_SUPPORT)
    );
    public static String path(String uri) { return uri.replaceFirst("^/api(?:/v1)?", "").replaceAll("/+$", ""); }
    // Existing account/profile/settings and billing endpoints are deliberately outside this gate.
    public static boolean trialProtected(String uri) {
        String p = path(uri);
        if (p.equals("/student/profile") || p.startsWith("/student/profile/saved")
                || p.equals("/student/settings") || p.equals("/student/preferences")) return false;
        return List.of("/student", "/careers", "/courses", "/institutions", "/bursaries",
                "/learning-centre", "/recommendations", "/ai", "/applications", "/jobs")
                .stream().anyMatch(prefix -> p.equals(prefix) || p.startsWith(prefix + "/"));
    }

    public static ApiRule rule(String method, String uri) {
        String path = path(uri);
        return API.stream().filter(r -> (r.method.equals("*") || r.method.equals(method)) && MATCHER.match(r.path, path)).findFirst().orElse(null);
    }
}
