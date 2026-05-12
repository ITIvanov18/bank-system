package com.nbu.bank_system.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SmtpOnboardingEmailService implements OnboardingEmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpOnboardingEmailService(
            JavaMailSender mailSender,
            @Value("${app.mail.from}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendTemporaryPasswordEmail(String recipientEmail, String customerDisplayName, String temporaryPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipientEmail);
        message.setSubject("Your temporary online banking password");
        message.setText(buildBody(customerDisplayName, temporaryPassword));

        mailSender.send(message);
    }

    @Override
    public void sendPasswordResetEmail(String recipientEmail, String customerDisplayName, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipientEmail);
        message.setSubject("Reset your online banking password");
        message.setText(buildPasswordResetBody(customerDisplayName, resetLink));

        mailSender.send(message);
    }

    private String buildBody(String customerDisplayName, String temporaryPassword) {
        return "Hello " + customerDisplayName + ",\n\n"
                + "Your online banking profile has been created.\n"
                + "Use the temporary password below to sign in:\n\n"
                + temporaryPassword + "\n\n"
                + "For security reasons, you must change this password at first login.\n\n"
                + "Regards,\n"
                + "BankAi Team";
    }

    private String buildPasswordResetBody(String customerDisplayName, String resetLink) {
        return "Hello " + customerDisplayName + ",\n\n"
                + "We received a request to reset your online banking password.\n"
                + "Use the link below to choose a new password. The link expires in 5 minutes:\n\n"
                + resetLink + "\n\n"
                + "If you did not request this, you can ignore this email.\n\n"
                + "Regards,\n"
                + "BankAi Team";
    }
}

