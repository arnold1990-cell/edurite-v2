package com.edurite.ai.university;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UniversitySourceRegistryServiceTest {

    @Test
    void supportsLargeRegistryAndValidatesDomains() {
        UniversitySourceRegistryService service = new UniversitySourceRegistryService(buildProperties(50), new UniversityUrlNormalizer());

        assertThat(service.configuredUniversityCount()).isEqualTo(50);
        assertThat(service.getDefaultSources()).hasSizeGreaterThan(40);
        assertThat(service.isAllowedUrl("https://www.university-1.ac.za/programmes")).isTrue();
        assertThat(service.isAllowedUrl("https://evil.example.com/programmes")).isFalse();
    }

    @Test
    void deduplicateNormalizesEquivalentUrls() {
        UniversitySourceRegistryService service = new UniversitySourceRegistryService(buildProperties(2), new UniversityUrlNormalizer());

        List<String> deduped = service.deduplicate(List.of(
                "https://www.university-1.ac.za/programmes/",
                "https://www.university-1.ac.za/programmes?utm_source=ads",
                "https://www.university-2.ac.za/a"
        ));

        assertThat(deduped).containsExactly(
                "https://www.university-1.ac.za/programmes",
                "https://www.university-2.ac.za/a"
        );
    }

    private UniversityRegistryProperties buildProperties(int count) {
        UniversityRegistryProperties properties = new UniversityRegistryProperties();
        for (int index = 1; index <= count; index++) {
            UniversityRegistryProperties.UniversityRegistryEntry entry = new UniversityRegistryProperties.UniversityRegistryEntry();
            entry.setUniversityName("University " + index);
            entry.setBaseDomain("university-" + index + ".ac.za");
            entry.setAllowedDomains(List.of("university-" + index + ".ac.za"));
            entry.setSeedUrls(List.of("https://www.university-" + index + ".ac.za/programmes"));
            entry.setQualificationLevelsSupported(List.of("Undergraduate"));
            entry.setActive(true);
            entry.setCrawlPriority(index);
            properties.getRegistry().add(entry);
        }
        return properties;
    }
}

