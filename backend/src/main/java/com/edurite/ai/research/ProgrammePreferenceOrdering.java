package com.edurite.ai.research;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/** Tuition preference ordering is separate from academic eligibility and full-cost affordability. */
public final class ProgrammePreferenceOrdering {
    private ProgrammePreferenceOrdering() {}
    public static int budgetOrder(PublishedProgrammeEvidence evidence,BigDecimal budget) {
        if(budget==null || evidence==null || evidence.tuitionFees()==null) return 1;
        var match=Pattern.compile("(?i)^tuition fees?\\s*:\\s*(?:R|ZAR)\\s*([0-9 ]+(?:\\.[0-9]{2})?)\\s*(?:per (?:academic )?year|annually)\\s*$")
                .matcher(evidence.tuitionFees());
        if(!match.matches()) return 1;
        try { return new BigDecimal(match.group(1).replace(" ", "")).compareTo(budget)<=0?0:2; }
        catch(NumberFormatException ex) { return 1; }
    }
}
