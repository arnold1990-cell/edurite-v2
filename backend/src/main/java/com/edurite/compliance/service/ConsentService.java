package com.edurite.compliance.service;

import com.edurite.compliance.entity.ConsentRecord;
import com.edurite.compliance.repository.ConsentRecordRepository;
import com.edurite.user.entity.User;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ConsentService {

    public static final String POPIA_CONSENT_TYPE = "POPIA_ACCOUNT_CREATION";

    private final ConsentRecordRepository consentRecordRepository;
    private final String defaultConsentVersion;

    public ConsentService(
            ConsentRecordRepository consentRecordRepository,
            @Value("${app.popi.consent-version:v1.0}") String defaultConsentVersion
    ) {
        this.consentRecordRepository = consentRecordRepository;
        this.defaultConsentVersion = defaultConsentVersion;
    }

    public ConsentRecord recordPopiaConsent(User user, String consentVersion) {
        ConsentRecord record = new ConsentRecord();
        record.setUserId(user.getId());
        record.setConsentType(POPIA_CONSENT_TYPE);
        record.setConsentAccepted(Boolean.TRUE);
        record.setConsentVersion(consentVersion == null || consentVersion.isBlank() ? defaultConsentVersion : consentVersion.trim());
        record.setAcceptedAt(OffsetDateTime.now());
        return consentRecordRepository.save(record);
    }

    public java.util.List<ConsentRecord> findByUserId(UUID userId) {
        return consentRecordRepository.findByUserIdOrderByAcceptedAtDesc(userId);
    }
}

