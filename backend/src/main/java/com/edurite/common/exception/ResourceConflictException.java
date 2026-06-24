package com.edurite.common.exception;

/**
 * This class named ResourceConflictException is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class ResourceConflictException extends RuntimeException {
    public ResourceConflictException(String message) {
        super(message);
    }
}

