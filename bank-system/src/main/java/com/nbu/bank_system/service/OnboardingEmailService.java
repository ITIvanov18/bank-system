package com.nbu.bank_system.service;

/**
 * Абстракция за email delivery в onboarding и password reset flows.
 * Позволява service слоят да зависи от contract, а конкретният SMTP transport да остане сменяем.
 */

public interface OnboardingEmailService {

    void sendTemporaryPasswordEmail(String recipientEmail, String customerDisplayName, String temporaryPassword);

    void sendPasswordResetEmail(String recipientEmail, String customerDisplayName, String resetLink);
}

