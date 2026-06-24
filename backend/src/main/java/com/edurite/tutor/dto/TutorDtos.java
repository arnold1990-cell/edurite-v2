package com.edurite.tutor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class TutorDtos {
    private TutorDtos() {
    }

    public record TutorAskRequest(
            UUID sessionId,
            @NotBlank @Size(max = 80) String subject,
            @NotBlank @Size(max = 5000) String question
    ) {
    }

    public record TutorSessionRequest(
            @NotBlank @Size(max = 80) String subject,
            @Size(max = 255) String title
    ) {
    }

    public record TutorMessageResponse(
            UUID id,
            String sender,
            String message,
            OffsetDateTime createdAt
    ) {
    }

    public record TutorSessionResponse(
            UUID id,
            String subject,
            String title,
            OffsetDateTime lastMessageAt,
            List<TutorMessageResponse> messages
    ) {
    }

    public record TutorAskResponse(
            UUID sessionId,
            String subject,
            String answer,
            List<TutorMessageResponse> messages
    ) {
    }
}

