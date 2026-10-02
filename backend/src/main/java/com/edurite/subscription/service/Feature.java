package com.edurite.subscription.service;

import com.edurite.subscription.entity.PlanType;

/** The single feature-to-plan catalogue. Sent to clients; never accepted from them. */
public enum Feature {
    PROFILE(PlanType.BASIC, "Create learner profile"),
    CAREER_BASIC(PlanType.BASIC, "Explore basic career information"),
    APS_BASIC(PlanType.BASIC, "Basic APS calculator"),
    INSTITUTION_SEARCH_LIMITED(PlanType.BASIC, "Limited course and institution search"),
    BURSARIES_BASIC(PlanType.BASIC, "Browse bursaries and scholarships"),
    PROGRESS_BASIC(PlanType.BASIC, "Basic progress tracking"),
    LEARNING_RESOURCES_LIMITED(PlanType.BASIC, "Limited learning resources"),
    AI_SUPPORT(PlanType.BASIC, "AI support"),
    CAREER_FULL(PlanType.PREMIUM, "Full career profiles"),
    CAREER_ASSESSMENT(PlanType.PREMIUM, "Full career assessment"),
    PERSONALISED_CAREER_RECOMMENDATIONS(PlanType.PREMIUM, "Personalised career recommendations"),
    SUBJECT_CAREER_MATCHING(PlanType.PREMIUM, "Subject-to-career matching"),
    APS_FULL(PlanType.PREMIUM, "Full APS calculator and progress tracking"),
    INSTITUTION_SEARCH_FULL(PlanType.PREMIUM, "Explore universities and TVET colleges"),
    BURSARIES_FULL(PlanType.PREMIUM, "Full access to bursaries and scholarships"),
    SAVE_OPPORTUNITIES(PlanType.PREMIUM, "Save opportunities"),
    CAREER_ROADMAP_PERSONALISED(PlanType.PREMIUM, "Personalised career roadmap"),
    LEARNING_RESOURCES_FULL(PlanType.PREMIUM, "Full learning resources"),
    STUDY_RECOMMENDATIONS(PlanType.PREMIUM, "Study option recommendations"),
    PROGRESS_FULL(PlanType.PREMIUM, "Progress dashboard"),
    ADVANCED_MATCHING(PlanType.PRO, "Advanced career and qualification matching"),
    PRIORITY_OPPORTUNITY_MATCHING(PlanType.PRO, "Priority bursary and opportunity matching"),
    CAREER_ROADMAP_ADVANCED(PlanType.PRO, "Advanced personalised career roadmap"),
    ACADEMIC_ANALYSIS(PlanType.PRO, "Detailed academic analysis and improvement plan"),
    AI_OPPORTUNITY_ASSISTANT(PlanType.PRO, "AI opportunity assistant: applications, essays and CV assistance"),
    APPLICATION_SUPPORT(PlanType.PRO, "Application preparation and guidance"),
    PROGRESS_ADVANCED(PlanType.PRO, "Advanced progress dashboard and insights"),
    EARLY_ACCESS(PlanType.PRO, "Early access to new features"),
    PARENT_GUARDIAN_INSIGHTS(PlanType.PRO, "Parent / guardian insights when available");

    public final PlanType minimumPlan;
    public final String label;
    Feature(PlanType minimumPlan, String label) { this.minimumPlan = minimumPlan; this.label = label; }
    public boolean allowed(PlanType plan) { return plan.ordinal() >= minimumPlan.ordinal(); }
}
