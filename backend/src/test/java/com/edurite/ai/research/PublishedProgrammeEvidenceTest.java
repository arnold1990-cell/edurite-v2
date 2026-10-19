package com.edurite.ai.research;

import com.edurite.ai.university.*;
import com.edurite.institution.universityinfo.entity.UniversityProgramme;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PublishedProgrammeEvidenceTest {
    @Test void eligibilityCategoryRemainsCompatibleWithPersistedReportRoundTrips() throws Exception {
        var value=ResearchCatalogueService.unknown("Information not yet verified.");
        var json=new com.fasterxml.jackson.databind.ObjectMapper();
        assertThat(json.readValue(json.writeValueAsString(value),CareerResearchDtos.Eligibility.class)).isEqualTo(value);
    }
    @Test void budgetOrderingUsesOnlyExplicitAnnualZarTuitionAndNeverInventsMissingFees() {
        var evidence=new PublishedProgrammeEvidenceExtractor().extract(page("Tuition fees: R40 000 per academic year;"),true);
        assertThat(ProgrammePreferenceOrdering.budgetOrder(evidence,new java.math.BigDecimal("50000"))).isZero();
        assertThat(ProgrammePreferenceOrdering.budgetOrder(evidence,new java.math.BigDecimal("20000"))).isEqualTo(2);
        var semester=new PublishedProgrammeEvidenceExtractor().extract(page("Tuition fees: R20 000 per semester;"),true);
        assertThat(ProgrammePreferenceOrdering.budgetOrder(semester,new java.math.BigDecimal("50000"))).isEqualTo(1);
        assertThat(ProgrammePreferenceOrdering.budgetOrder(null,new java.math.BigDecimal("50000"))).isEqualTo(1);
    }
    private UniversitySourcePageResult page(String text) {
        return new UniversitySourcePageResult("https://official.ac.za/accounting","Bachelor of Accounting",UniversityPageType.PROGRAMME_DETAIL,
                text,Set.of("accounting"),List.of("Bachelor of Accounting"),true,null,null,
                Map.of("https://official.ac.za/apply","Apply online","https://official.ac.za/funding","Scholarships"));
    }
    @Test void extractsOnlyPublishedLabelledFactsAndRetainsOfficialEvidence() {
        var result=new PublishedProgrammeEvidenceExtractor().extract(page("Admission year: 2027; Minimum APS: 30; Best six subjects excluding Life Orientation. "
                +"Entry requirements: Mathematics level 5; English 60%; Tuition fees: R40 000 per academic year; Campus: Main Campus; "
                +"Applications open: 1 April 2026; Applications close: 30 September 2026; Faculty: Commerce;"),true);
        assertThat(result.minimumAps()).isEqualTo(30);assertThat(result.apsRule()).isEqualTo("NSC_BEST_6_EXCL_LO");
        assertThat(result.subjectRequirements()).containsEntry("Mathematics","Level 5").containsEntry("English","60%");
        assertThat(result.intakeYear()).isEqualTo(2027);assertThat(result.tuitionFees()).contains("R40 000");
        assertThat(result.applicationClosing()).contains("30 September 2026");
        assertThat(result.applicationUrl()).isEqualTo("https://official.ac.za/apply");
        assertThat(result.fundingLinks()).containsKey("https://official.ac.za/funding");
        assertThat(PublishedProgrammeEvidence.fromJson(result.toJson())).isEqualTo(result);
    }
    @Test void multipleProgrammeListsConflictsAndSubjectAlternativesNeverBecomeVerifiedThresholds() {
        var extractor=new PublishedProgrammeEvidenceExtractor();
        var listing=extractor.extract(page("APS: 30; Mathematics level 5; tuition fees: R100;"),false);
        assertThat(listing.minimumAps()).isNull();assertThat(listing.subjectRequirements()).isEmpty();assertThat(listing.tuitionFees()).isNull();
        var ambiguous=extractor.extract(page("Minimum APS: 30; Minimum APS: 36; Mathematics level 4 or Mathematical Literacy level 6;"),true);
        assertThat(ambiguous.minimumAps()).isNull();assertThat(ambiguous.subjectRequirements()).isEmpty();
        var unknown=extractor.extract(page("Please contact admissions for fees and requirements."),true);
        assertThat(unknown.minimumAps()).isNull();assertThat(unknown.tuitionFees()).isNull();assertThat(unknown.apsRule()).isNull();
    }
    @Test void incompleteExtractedRequirementsCannotGuaranteeAdmission() {
        var row=new UniversityProgramme();row.setLastVerifiedAt(OffsetDateTime.now());row.setRetrievalStatus("SOURCE_CONFIRMED");
        row.setPublishedEvidenceJson(new PublishedProgrammeEvidenceExtractor().extract(page("Admission year: 2027; Mathematics level 5; APS: 30;"),true).toJson());
        var result=new ProgrammeEligibilityService().assessPublished(row,List.of(),"South Africa");
        assertThat(result.status()).isEqualTo("Eligibility cannot be determined");assertThat(result.category()).isEqualTo("Eligibility cannot be determined");
        assertThat(result.uncheckedConditions()).anyMatch(s->s.contains("do not guarantee admission"));
    }
}
