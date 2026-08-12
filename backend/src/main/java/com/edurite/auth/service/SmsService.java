package com.edurite.auth.service;

public interface SmsService {
    void sendOtp(String mobileNumber, String otp);
}
