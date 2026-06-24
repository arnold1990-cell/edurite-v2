package com.edurite.common.exception;

/**
 * This class named DuplicateEmailException is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String email) {
        super("An account with email '%s' already exists".formatted(email));
    }
}

