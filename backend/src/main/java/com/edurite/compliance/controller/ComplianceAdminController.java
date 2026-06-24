package com.edurite.compliance.controller;

import com.edurite.compliance.service.ConsentService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/admin/compliance", "/api/admin/compliance"})
public class ComplianceAdminController {

    private final ConsentService consentService;

    public ComplianceAdminController(ConsentService consentService) {
        this.consentService = consentService;
    }

    @GetMapping("/consents/{userId}")
    public List<Map<String, Object>> consents(@PathVariable UUID userId) {
        return consentService.findByUserId(userId).stream()
                .map(record -> {
                    Map<String, Object> dto = new java.util.LinkedHashMap<>();
                    dto.put("id", record.getId());
                    dto.put("userId", record.getUserId());
                    dto.put("consentType", record.getConsentType());
                    dto.put("consentAccepted", record.getConsentAccepted());
                    dto.put("consentVersion", record.getConsentVersion());
                    dto.put("acceptedAt", record.getAcceptedAt());
                    dto.put("ipAddress", record.getIpAddress());
                    dto.put("userAgent", record.getUserAgent());
                    return dto;
                })
                .toList();
    }
}

