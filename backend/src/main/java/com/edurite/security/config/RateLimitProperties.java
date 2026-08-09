package com.edurite.security.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "edurite.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private boolean failOpen = true;
    private Map<String, Rule> rules = new LinkedHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isFailOpen() {
        return failOpen;
    }

    public void setFailOpen(boolean failOpen) {
        this.failOpen = failOpen;
    }

    public Map<String, Rule> getRules() {
        return rules;
    }

    public void setRules(Map<String, Rule> rules) {
        this.rules = rules == null ? new LinkedHashMap<>() : rules;
    }

    public static class Rule {
        private String pathPrefixes = "";
        private long limit = 600;
        private long windowSeconds = 60;

        public String getPathPrefixes() {
            return pathPrefixes;
        }

        public void setPathPrefixes(String pathPrefixes) {
            this.pathPrefixes = pathPrefixes == null ? "" : pathPrefixes;
        }

        public long getLimit() {
            return limit;
        }

        public void setLimit(long limit) {
            this.limit = Math.max(1, limit);
        }

        public long getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(long windowSeconds) {
            this.windowSeconds = Math.max(1, windowSeconds);
        }
    }
}
