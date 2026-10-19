package com.edurite.ai.research;

import java.util.*;

/** Published excerpts, not AI-generated facts. Null fields mean information not yet verified. */
public record PublishedProgrammeEvidence(
        Integer minimumAps, String apsRule, Map<String,String> subjectRequirements,
        Integer intakeYear, String publishedRequirements, String applicationOpening,
        String applicationClosing, String tuitionFees, String campus, String faculty,
        String applicationUrl, Map<String,String> fundingLinks, String evidenceUrl,
        boolean programmeSpecific, List<String> limitations) {
    private static final com.fasterxml.jackson.databind.ObjectMapper JSON = new com.fasterxml.jackson.databind.ObjectMapper();
    public String toJson() {
        try { return JSON.writeValueAsString(this); }
        catch(Exception ex) { throw new IllegalArgumentException("Evidence serialization failed",ex); }
    }
    public static PublishedProgrammeEvidence fromJson(String value) {
        if(value==null || value.isBlank()) return null;
        try { return JSON.readValue(value,PublishedProgrammeEvidence.class); }
        catch(Exception ex) { return null; }
    }
}
