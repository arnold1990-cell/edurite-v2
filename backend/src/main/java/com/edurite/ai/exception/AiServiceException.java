package com.edurite.ai.exception;

import org.springframework.http.HttpStatus;

public class AiServiceException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final String userMessage;

    public AiServiceException(HttpStatus status, String message) {
        this(status, "AI_GUIDANCE_UNAVAILABLE", message, message);
    }

    public AiServiceException(HttpStatus status, String errorCode, String message, String userMessage) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getUserMessage() {
        return userMessage;
    }
}

