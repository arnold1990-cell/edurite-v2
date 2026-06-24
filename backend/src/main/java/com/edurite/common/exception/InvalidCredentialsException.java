package com.edurite.common.exception;

/**
 * This class named InvalidCredentialsException is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Invalid email or password");
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }
}

