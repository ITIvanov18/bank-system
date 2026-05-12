package com.nbu.bank_system.service;

public interface OnboardingEmailService {

    void sendTemporaryPasswordEmail(String recipientEmail, String customerDisplayName, String temporaryPassword);

    void sendPasswordResetEmail(String recipientEmail, String customerDisplayName, String resetLink);
}

