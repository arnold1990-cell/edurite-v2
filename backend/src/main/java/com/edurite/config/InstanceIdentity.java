package com.edurite.config;

import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InstanceIdentity {

    private final String instanceId;

    public InstanceIdentity(
            @Value("${edurite.instance.id:${HOSTNAME:${COMPUTERNAME:}}}") String configuredInstanceId
    ) {
        this.instanceId = normalize(configuredInstanceId)
                .orElseGet(() -> "local-" + UUID.randomUUID());
    }

    public String id() {
        return instanceId;
    }

    private Optional<String> normalize(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String sanitized = value.trim().replaceAll("[^A-Za-z0-9_.-]", "-");
        if (sanitized.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(sanitized.length() > 80 ? sanitized.substring(0, 80) : sanitized);
    }
}
