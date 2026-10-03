package com.edurite.subscription.service;

import com.edurite.security.service.CurrentUserService;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.util.*;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.*;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice(basePackages="com.edurite")
public class SubscriptionResponseAdvice implements ResponseBodyAdvice<Object> {
    private final ObjectMapper mapper;
    private final CurrentUserService users;
    private final EntitlementService entitlements;
    private final AiUsageService usage;
    public SubscriptionResponseAdvice(ObjectMapper mapper, CurrentUserService users, EntitlementService entitlements, AiUsageService usage) {
        this.mapper=mapper; this.users=users; this.entitlements=entitlements; this.usage=usage;
    }
    public boolean supports(MethodParameter p, Class<? extends HttpMessageConverter<?>> c) { return true; }
    public Object beforeBodyWrite(Object body, MethodParameter p, MediaType m, Class<? extends HttpMessageConverter<?>> c, ServerHttpRequest req, ServerHttpResponse res) {
        if (!(req instanceof ServletServerHttpRequest servlet) || body == null || body instanceof String || body instanceof byte[]) return body;
        var request=servlet.getServletRequest();
        String path=StudentAccessPolicy.path(request.getRequestURI());
        boolean projection=path.startsWith("/student/career-roadmaps") || path.equals("/student/progress-score") || limitedCatalogue(path);
        if (!projection && request.getAttribute("aiReservation")==null) return body;
        if (res instanceof ServletServerHttpResponse response && response.getServletResponse().getStatus() >= 300) return body;
        // Public catalogue aliases must not unlock the full result set by dropping the JWT.
        if (request.getUserPrincipal()==null) {
            if (!limitedCatalogue(path)) return body;
            JsonNode preview=mapper.valueToTree(body);
            capArrays(preview,5);
            return preview;
        }
        var user=users.requireUser(request.getUserPrincipal());
        if (!entitlements.isStudent(user)) return body;
        JsonNode node=mapper.valueToTree(body);
        if (request.getAttribute("aiReservation") instanceof UUID reservation && res instanceof ServletServerHttpResponse response && response.getServletResponse().getStatus()<300) {
            boolean successful = !node.path("available").isBoolean() || node.path("available").asBoolean();
            successful = successful && !"ERROR".equals(node.path("status").asText()) && !"UNAVAILABLE".equals(node.path("mode").asText());
            usage.finish(reservation,successful);
            var current=usage.current(user.getId());
            res.getHeaders().set("X-AI-Allowance",String.valueOf(current.allowance()));
            res.getHeaders().set("X-AI-Used",String.valueOf(current.used()));
            res.getHeaders().set("X-AI-Remaining",String.valueOf(current.remaining()));
        }
        if (path.startsWith("/student/career-roadmaps") && !entitlements.hasFeature(user.getId(),Feature.CAREER_ROADMAP_ADVANCED)) redactRoadmap(node);
        if (path.equals("/student/progress-score")) {
            if (!entitlements.hasFeature(user.getId(),Feature.PROGRESS_ADVANCED)) redactInsights(node);
            if (!entitlements.hasFeature(user.getId(),Feature.PROGRESS_FULL)) capArrays(node,2);
        }
        if (limitedCatalogue(path) && !entitlements.hasFeature(user.getId(),Feature.CAREER_FULL)) capArrays(node,5);
        return projection ? node : body;
    }
    private static boolean limitedCatalogue(String p) {
        return p.equals("/careers") || p.equals("/courses") || p.equals("/institutions") || p.startsWith("/student/universities/") || p.startsWith("/learning-centre/") || p.startsWith("/student/learning-centre/") || p.equals("/student/career-roadmaps");
    }
    private void capArrays(JsonNode node,int limit) {
        if(node instanceof ArrayNode array) { while(array.size()>limit) array.remove(array.size()-1); }
        else if(node instanceof ObjectNode object) object.elements().forEachRemaining(child -> capArrays(child,limit));
    }
    private void redactRoadmap(JsonNode node) {
        if(node instanceof ObjectNode object) {
            if(object.has("gapAnalysis")) {
                ObjectNode gap=mapper.createObjectNode(); gap.put("riskLevel","Pro insight");
                for(String key:List.of("missingSubjects","subjectsNeedingImprovement","bestFitUniversities","improvementSuggestions")) gap.set(key,mapper.createArrayNode());
                object.set("gapAnalysis",gap);
            }
            if(object.has("studyPlan")) object.set("studyPlan",mapper.createArrayNode());
            object.elements().forEachRemaining(this::redactRoadmap);
        } else if(node.isArray()) node.forEach(this::redactRoadmap);
    }
    private void redactInsights(JsonNode node) {
        if(node instanceof ObjectNode object) {
            if(object.has("recommendations")) object.set("recommendations",mapper.createArrayNode());
            if(object.has("recommendation")) object.put("recommendation", "Upgrade to Pro for detailed insights.");
            object.elements().forEachRemaining(this::redactInsights);
        } else if(node.isArray()) node.forEach(this::redactInsights);
    }
}
