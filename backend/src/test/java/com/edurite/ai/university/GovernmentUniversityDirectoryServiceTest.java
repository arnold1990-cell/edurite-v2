package com.edurite.ai.university;

import java.time.*;
import java.util.*;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GovernmentUniversityDirectoryServiceTest {
    private final UniversityRegistryProperties properties=new UniversityRegistryProperties();
    private final UniversitySourceRegistryService registry=new UniversitySourceRegistryService(properties,new UniversityUrlNormalizer());
    private final OfficialPageAccessPolicy access=mock(OfficialPageAccessPolicy.class);
    private final UniversityDirectoryRepository records=mock(UniversityDirectoryRepository.class);
    private final GovernmentUniversityDirectoryService service=new GovernmentUniversityDirectoryService(registry,access,records,
            mock(com.edurite.background.DistributedJobLockService.class));

    @Test void completeSnapshotHasAll26PublicInstitutionsAndSeparatePrivateStatus() {
        var privateEntry=new UniversityRegistryProperties.UniversityRegistryEntry();
        privateEntry.setUniversityName("Private fixture");privateEntry.setBaseDomain("private.com");
        properties.setRegistry(List.of(privateEntry));registry.logRegistryStatus();
        assertThat(registry.getActiveUniversities()).hasSize(26);
        assertThat(registry.getActiveUniversities()).extracting(UniversityRegistryProperties.UniversityRegistryEntry::getUniversityName)
                .contains("University of the Western Cape", "University of Mpumalanga", "Sol Plaatje University");
        assertThat(registry.getActiveUniversities()).extracting(UniversityRegistryProperties.UniversityRegistryEntry::getInstitutionType)
                .contains("TRADITIONAL","COMPREHENSIVE","UNIVERSITY_OF_TECHNOLOGY");
        assertThat(registry.isAllowedUrl("https://private.com/programmes")).isFalse();
        assertThat(registry.getRegistryEntries()).hasSize(27);
        assertThat(registry.getActiveUniversities()).allSatisfy(e -> {
            assertThat(e.getProvince()).isNotBlank();assertThat(e.getRegistrySourceUrl()).isEqualTo(GovernmentUniversityDirectoryService.PUBLIC_SOURCE);
            assertThat(e.getLastVerificationDate()).isNotNull();
        });
    }

    @Test void discoveryUsesGovernmentRowsAndRejectsForeignOrScriptLinks() throws Exception {
        String html="<table><tr><td>New Public University Private Bag 4 <a href='https://new.ac.za/'>website</a></td></tr>"
                +"<tr><td>Impostor University <a href='https://new.ac.za.evil.example/'>website</a></td></tr>"
                +"<tr><td>Script University <a href='javascript:alert(1)'>website</a></td></tr></table>";
        var entries=service.parsePublicDirectory(Jsoup.parse(html,GovernmentUniversityDirectoryService.PUBLIC_SOURCE));
        assertThat(entries).hasSize(1);assertThat(entries.get(0).getBaseDomain()).isEqualTo("new.ac.za");
        when(access.fetch(anyString(),anyInt(),anyInt())).thenReturn(Jsoup.parse(html,GovernmentUniversityDirectoryService.PUBLIC_SOURCE));
        assertThat(service.refresh()).contains("retained");verifyNoInteractions(records);
    }

    @Test void privateRegistrationRequiresCurrentLinkedRegisterAndExactInstitutionEvidence() throws Exception {
        String pdf="https://www.dhet.gov.za/Registers_DocLib/current.pdf";
        when(access.fetch(eq(GovernmentUniversityDirectoryService.PRIVATE_SOURCE),anyInt(),anyInt()))
                .thenReturn(Jsoup.parse("<a href='"+pdf+"'>Register of Private Higher Education Institutions</a>",GovernmentUniversityDirectoryService.PRIVATE_SOURCE));
        String date=LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("d MMMM uuuu",Locale.ENGLISH));
        when(access.fetch(eq(pdf),anyInt(),anyInt())).thenReturn(Jsoup.parse("LAST UPDATE: "+date+" Private Fixture 2001/HE07/008 Registered www.private.co.za"));
        var request=new GovernmentUniversityDirectoryService.PrivateRegistration("Private Fixture","2001/HE07/008","https://www.private.co.za/",pdf);
        assertThat(service.verifyPrivateRegistration(request)).contains("Registration verified");
        assertThat(registry.getActiveUniversities()).hasSize(1);
        assertThat(registry.getActiveUniversities().get(0).getInstitutionType()).isEqualTo("PRIVATE_HIGHER_EDUCATION");
        when(access.fetch(eq(pdf),anyInt(),anyInt())).thenReturn(Jsoup.parse("LAST UPDATE: "+date+" Private Fixture 2001/HE07/008 Registration cancelled www.private.co.za"));
        assertThatThrownBy(()->service.verifyPrivateRegistration(request)).hasMessageContaining("not yet verified");
    }
}
