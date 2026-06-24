package com.edurite.bursary.source;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "edurite.bursary")
public class BursarySourceProperties {

    private Fallback fallback = new Fallback();

    public Fallback getFallback() {
        return fallback;
    }

    public void setFallback(Fallback fallback) {
        this.fallback = fallback;
    }

    public static class Fallback {
        private List<String> trustedDomains = new ArrayList<>(List.of(
                "bursaries-southafrica.co.za",
                "zabursaries.co.za",
                "careerwise.co.za",
                "gov.za"
        ));

        private int maxResults = 5;

        public List<String> getTrustedDomains() {
            return trustedDomains;
        }

        public void setTrustedDomains(List<String> trustedDomains) {
            this.trustedDomains = trustedDomains;
        }

        public int getMaxResults() {
            return maxResults;
        }

        public void setMaxResults(int maxResults) {
            this.maxResults = maxResults;
        }
    }
}

