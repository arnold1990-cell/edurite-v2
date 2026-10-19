package com.edurite.ai.research;

import com.edurite.ai.university.UniversitySourcePageResult;
import java.util.*;
import java.util.regex.*;

/** Conservative labelled-field extraction. Institution-specific scores are never inferred. */
public class PublishedProgrammeEvidenceExtractor {
    public PublishedProgrammeEvidence extract(UniversitySourcePageResult page, boolean specific) {
        if(!specific) return new PublishedProgrammeEvidence(null,null,Map.of(),null,null,null,null,null,null,null,null,Map.of(),page.sourceUrl(),false,
                List.of("Information not yet verified. Listing-page requirements cannot be assigned to a programme."));
        String text=Objects.toString(page.cleanedText(), "");
        Integer aps=uniqueNumber(text,"(?i)(?:minimum\\s+(?:APS|admission point score)|APS\\s*(?:minimum|requirement|score)?)[\\s:=-]*(\\d{2})(?!\\d)");
        if(aps!=null && (aps<6 || aps>56)) aps=null;
        String rule=null;
        if(text.matches("(?is).*best (?:six|6) subjects.{0,80}exclud(?:es?|ing) Life Orientation.*")) rule="NSC_BEST_6_EXCL_LO";
        Map<String,String> subjects=new LinkedHashMap<>();
        for(String subject:List.of("Mathematics","Mathematical Literacy","English","Accounting","Physical Sciences","Life Sciences","Life Orientation","Geography","Business Studies","Information Technology","Computer Applications Technology")) {
            Matcher matcher=Pattern.compile("(?i)\\b"+Pattern.quote(subject)+"\\s*(?::|minimum|at least|achievement)?\\s*(?:level\\s*([1-7])|(\\d{1,3})\\s*%)(?!\\d)").matcher(text);
            Set<String> values=new LinkedHashSet<>();
            while(matcher.find()) {
                if(matcher.group(1)!=null) values.add("Level "+matcher.group(1));
                else if(Integer.parseInt(matcher.group(2))<=100) values.add(matcher.group(2)+"%");
            }
            if(values.size()==1) subjects.put(subject,values.iterator().next());
        }
        // Alternatives/negation cannot be safely turned into compulsory conditions.
        boolean alternatives=Pattern.compile("(?i)\\b(?:or|either|not required|not compulsory|recommended)\\b").matcher(text).find();
        if(alternatives) subjects.clear();
        Map<String,String> funding=new LinkedHashMap<>();
        String application=null;
        for(var link:page.officialLinks().entrySet()) {
            if(link.getValue().matches("(?is).*(bursar|scholarship|financial aid|funding).*")) funding.put(link.getKey(),link.getValue());
            if(application==null && link.getValue().matches("(?is).*(apply|application portal|online application).*")) application=link.getKey();
        }
        List<String> limits=new ArrayList<>();
        limits.add("Published minimums do not guarantee admission; selection, qualification completion and available places require confirmation.");
        if(aps!=null && rule==null) limits.add("Institution-specific APS calculation rule is not verified.");
        if(alternatives) limits.add("Subject alternatives or advisory/negative wording require review; no compulsory thresholds inferred.");
        // A citation establishes what was retrieved, not completeness of every admission condition.
        limits.add("Completeness of published requirements and current intake must be confirmed.");
        return new PublishedProgrammeEvidence(aps,rule,Map.copyOf(subjects),uniqueNumber(text,"(?i)(?:intake|admission year|academic year)\\s*[:=-]?\\s*(20\\d{2})"),
                excerpt(text,"(?i)(?:admission|entry|minimum) requirements?\\s*[:=-]?\\s*(.{1,900})"),
                labelled(text,"applications? open(?:ing)?(?: date)?"),labelled(text,"applications? clos(?:e|ing)(?: date)?"),
                labelled(text,"tuition fees?"),labelled(text,"campus"),labelled(text,"faculty"),application,Map.copyOf(funding),page.sourceUrl(),true,List.copyOf(limits));
    }

    private Integer uniqueNumber(String text,String pattern) {
        Matcher m=Pattern.compile(pattern).matcher(text); Set<Integer> values=new LinkedHashSet<>();
        while(m.find()) values.add(Integer.valueOf(m.group(1)));
        return values.size()==1?values.iterator().next():null;
    }
    private String labelled(String text,String label) {
        return excerpt(text,"(?i)\\b"+label+"\\s*[:=-]\\s*([^\\n;.!?]{1,160})");
    }
    private String excerpt(String text,String pattern) {
        Matcher m=Pattern.compile(pattern).matcher(text);
        return m.find()?m.group().strip():null;
    }
}
