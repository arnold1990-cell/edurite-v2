package com.edurite.ai.university;

import com.edurite.ai.university.UniversityRegistryProperties.UniversityRegistryEntry;
import com.edurite.ai.university.entity.UniversityDirectoryRecord;
import com.edurite.background.DistributedJobLockService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.net.URI;
import java.util.*;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;

/** Only the configured government directory may introduce public institution domains. */
@Service
public class GovernmentUniversityDirectoryService {
    public static final String PUBLIC_SOURCE = "https://www.education.gov.za/FurtherStudies/Universities.aspx";
    public static final String PRIVATE_SOURCE = "https://www.dhet.gov.za/SitePages/DocRegisters.aspx";
    private final UniversitySourceRegistryService registry;
    private final OfficialPageAccessPolicy access;
    private final UniversityDirectoryRepository records;
    private final DistributedJobLockService locks;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private volatile String lastOutcome = "Using dated government directory snapshot; live directory update not yet verified.";

    public GovernmentUniversityDirectoryService(UniversitySourceRegistryService registry, OfficialPageAccessPolicy access,
            UniversityDirectoryRepository records, DistributedJobLockService locks) {
        this.registry=registry; this.access=access; this.records=records; this.locks=locks;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void restore() {
        List<UniversityRegistryEntry> restored = new ArrayList<>();
        for (var record : records.findAll()) {
            try {
                var entry = mapper.readValue(record.getMetadataJson(), UniversityRegistryEntry.class);
                if (PUBLIC_SOURCE.equals(entry.getRegistrySourceUrl()) && "PUBLIC_ESTABLISHED".equals(entry.getRegistrationStatus())
                        || "PRIVATE_HIGHER_EDUCATION".equals(entry.getInstitutionType()) && approvedPrivateSource(entry.getRegistrySourceUrl())) restored.add(entry);
            } catch (Exception ex) { lastOutcome="A cached directory record needs review; bundled evidence retained."; }
        }
        registry.mergeGovernmentEntries(restored);
    }

    @Scheduled(cron="${edurite.university.directory-cron:0 0 1 * * MON}")
    public void scheduledRefresh() {
        locks.runOnce("university.government-directory", Duration.ofMinutes(10), this::refresh);
    }

    public String refresh() {
        try {
            var entries=parsePublicDirectory(access.fetch(PUBLIC_SOURCE, 10000, 1500000));
            // Partial/challenge pages must not silently replace the complete baseline.
            if (entries.size()<26 || entries.size()>60) throw new IllegalArgumentException("Incomplete official directory response");
            for (var entry:entries) {
                var record=records.findByUniversityName(entry.getUniversityName()).orElseGet(UniversityDirectoryRecord::new);
                record.setUniversityName(entry.getUniversityName());
                record.setMetadataJson(mapper.writeValueAsString(entry));
                records.save(record);
            }
            registry.mergeGovernmentEntries(entries);
            lastOutcome="Government public directory verified: "+entries.size()+" institutions at "+OffsetDateTime.now();
        } catch (Exception ex) {
            lastOutcome="Government directory unavailable or unverified; dated registry retained. "+Objects.toString(ex.getMessage(), "Access restricted");
        }
        return lastOutcome;
    }

    public String lastOutcome() { return lastOutcome; }

    public record PrivateRegistration(String institutionName,String registrationNumber,String officialWebsite,String registerPdfUrl) {}

    /** A configured private brand is not registration evidence. Verify against the current linked DHET register. */
    public String verifyPrivateRegistration(PrivateRegistration request) {
        try {
            if(request.institutionName()==null || request.institutionName().isBlank() || request.institutionName().length()>160)
                throw new IllegalArgumentException("A registered institution name is required");
            if(request.registrationNumber()==null || !request.registrationNumber().matches("20\\d{2}/HE\\d{2}/\\d{3}"))
                throw new IllegalArgumentException("A DHET higher education registration number is required");
            if(!approvedPrivateSource(request.registerPdfUrl())) throw new IllegalArgumentException("An official DHET register PDF is required");
            var directory=access.fetch(PRIVATE_SOURCE,10000,1500000);
            if(directory.select("a[href]").stream().noneMatch(a -> a.absUrl("href").equals(request.registerPdfUrl())))
                throw new IllegalArgumentException("The document is not linked by the current DHET registers directory");
            var document=access.fetch(request.registerPdfUrl(),10000,1500000);
            String text=document.text();
            var updated=java.util.regex.Pattern.compile("(?i)LAST UPDATE[D]?:?\\s*(\\d{1,2} [A-Za-z]+ 20\\d{2})").matcher(text);
            if(!updated.find() || LocalDate.parse(updated.group(1),java.time.format.DateTimeFormatter.ofPattern("d MMMM uuuu",Locale.ENGLISH)).isBefore(LocalDate.now().minusDays(90)))
                throw new IllegalArgumentException("Current register publication date cannot be verified");
            // Limit evidence to one registration block; names and websites elsewhere cannot authorize this institution.
            int position=text.indexOf(request.registrationNumber());
            if(position<0) throw new IllegalArgumentException("Registration was not found in the accessible register pages");
            int start=Math.max(0,position-250), end=Math.min(text.length(),position+1800);
            var next=java.util.regex.Pattern.compile("20\\d{2}/HE\\d{2}/\\d{3}").matcher(text.substring(position+request.registrationNumber().length()));
            if(next.find()) end=Math.min(end,position+request.registrationNumber().length()+next.start());
            String block=text.substring(start,end).toLowerCase(Locale.ROOT);
            var website=URI.create(request.officialWebsite());
            if(!"https".equals(website.getScheme()) || website.getUserInfo()!=null || website.getHost()==null
                    || !block.contains(website.getHost().replaceFirst("^www\\.", "").toLowerCase(Locale.ROOT))
                    || !block.contains(request.institutionName().toLowerCase(Locale.ROOT))
                    || block.matches("(?s).*(cancelled|canceled|lapsed|deregistered|provisional|withdrawn).*")
                    || !block.contains("registered"))
                throw new IllegalArgumentException("Institution identity and active registration need manual source review");
            var entry=new UniversityRegistryEntry();
            entry.setUniversityName(request.institutionName());entry.setBaseDomain(website.getHost().replaceFirst("^www\\.",""));
            entry.setOfficialWebsite(request.officialWebsite());entry.setSeedUrls(List.of(request.officialWebsite()));
            entry.setCountry("South Africa");entry.setInstitutionType("PRIVATE_HIGHER_EDUCATION");
            entry.setRegistrationNumber(request.registrationNumber());entry.setRegistrationStatus("REGISTERED");
            entry.setRegistrySourceUrl(request.registerPdfUrl());entry.setLastVerificationDate(LocalDate.now());
            var record=records.findByUniversityName(entry.getUniversityName()).orElseGet(UniversityDirectoryRecord::new);
            record.setUniversityName(entry.getUniversityName());record.setMetadataJson(mapper.writeValueAsString(entry));records.save(record);
            registry.mergeGovernmentEntries(List.of(entry));
            return "Registration verified; individual programme accreditation still requires verification.";
        } catch(Exception ex) { throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
                "Information not yet verified. "+Objects.toString(ex.getMessage(),"Official register unavailable")); }
    }

    private boolean approvedPrivateSource(String url) {
        try { var uri=URI.create(url);return "https".equals(uri.getScheme()) && "www.dhet.gov.za".equals(uri.getHost())
                && uri.getPath().startsWith("/Registers_DocLib/") && uri.getPath().endsWith(".pdf"); }
        catch(Exception ex) { return false; }
    }

    public List<UniversityRegistryEntry> parsePublicDirectory(Document document) {
        Map<String,UniversityRegistryEntry> entries=new LinkedHashMap<>();
        for (var row:document.select("tr")) {
            var cell=row.selectFirst("td");
            if (cell==null) continue;
            var link=cell.select("a[href]").stream().filter(a -> {
                try { var u=URI.create(a.absUrl("href")); return Set.of("http","https").contains(u.getScheme()) && u.getHost()!=null && u.getHost().endsWith(".ac.za"); }
                catch(Exception ex) { return false; }
            }).findFirst().orElse(null);
            if(link==null) continue;
            String host=URI.create(link.absUrl("href")).getHost().replaceFirst("^www\\.", "");
            var prior=registry.getRegistryEntries().stream().filter(e -> e.getBaseDomain().equals(host) || e.getAllowedDomains().contains(host)).findFirst().orElse(null);
            String name=cell.text().split("(?i)\\s+(?:P\\s*O Box|Private Bag)")[0].strip();
            if (prior!=null) name=prior.getUniversityName();
            if(!name.toLowerCase(Locale.ROOT).contains("university") || name.length()>160) continue;
            var entry=new UniversityRegistryEntry();
            entry.setUniversityName(name); entry.setBaseDomain(host); entry.setAllowedDomains(List.of(host));
            entry.setOfficialWebsite(link.absUrl("href")); entry.setSeedUrls(List.of(link.absUrl("href")));
            entry.setCountry("South Africa"); entry.setInstitutionType(prior==null?"PUBLIC_TYPE_UNVERIFIED":prior.getInstitutionType());
            entry.setProvince(prior==null?null:prior.getProvince()); entry.setRegistrationStatus("PUBLIC_ESTABLISHED");
            entry.setRegistrySourceUrl(PUBLIC_SOURCE); entry.setLastVerificationDate(LocalDate.now());
            entries.putIfAbsent(host,entry);
        }
        return List.copyOf(entries.values());
    }
}
