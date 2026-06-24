package com.edurite.auth.service;

public interface OtpService {
    void sendVerificationOtp(String phoneNumber);

    void sendPasswordResetOtp(String phoneNumber);

    boolean verifyVerificationOtp(String phoneNumber, String code);

    boolean verifyPasswordResetOtp(String phoneNumber, String code);
}

