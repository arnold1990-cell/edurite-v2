package com.edurite.common.exception;

public class EmailVerificationRequiredException extends InvalidCredentialsException {
    public EmailVerificationRequiredException() {
        super("Verify your email before signing in. Use the resend verification email option.");
    }
}
