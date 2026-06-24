package com.edurite.bursary;

import com.edurite.bursary.dto.BursaryResultDto;
import com.edurite.bursary.dto.BursarySearchRequest;
import com.edurite.bursary.service.BursaryAggregationService;
import com.edurite.bursary.source.ProviderBursarySource;
import com.edurite.bursary.source.WebFallbackBursarySource;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BursaryAggregationServiceTest {

    @Test
    void prefersProviderAndFallsBackWhenInsufficientAndDeduplicates() {
        ProviderBursarySource provider = mock(ProviderBursarySource.class);
        WebFallbackBursarySource web = mock(WebFallbackBursarySource.class);
        BursaryAggregationService service = new BursaryAggregationService(provider, web);

        BursarySearchRequest request = new BursarySearchRequest("science", "Degree", "Gauteng", "citizen", 0, 5);
        BursaryResultDto p1 = new BursaryResultDto("1", "Science Fund", "NSF", "desc", "Degree", "Gauteng", "citizen", LocalDate.now(), "https://a", "OFFICIAL_PROVIDER", 90, List.of("https://a"), true, false, null);
        BursaryResultDto wDup = new BursaryResultDto("2", "Science Fund", "NSF", "desc", "Degree", "Gauteng", "citizen", null, "https://a", "TRUSTED_PUBLIC_FALLBACK", 65, List.of("https://a"), false, true, "Unverified");
        BursaryResultDto w2 = new BursaryResultDto("3", "Tech Grant", "Web", "desc", "Degree", "Gauteng", "citizen", null, "https://b", "TRUSTED_PUBLIC_FALLBACK", 70, List.of("https://b"), false, true, "Unverified");

        when(provider.fetch(request)).thenReturn(List.of(p1));
        when(web.fetch(request)).thenReturn(List.of(wDup, w2));

        var response = service.search(request);

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).title()).isEqualTo("Science Fund");
        assertThat(response.items().get(0).sourceType()).isEqualTo("OFFICIAL_PROVIDER");
        assertThat(response.items().get(1).incomplete()).isTrue();
    }
}

