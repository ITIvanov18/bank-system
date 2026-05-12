package com.nbu.bank_system.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SmtpOnboardingEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private SmtpOnboardingEmailService emailService;

    @BeforeEach
    void setUp() {
        // Създаваме сървиса ръчно, подавайки му Mock-натия mailSender и тестов имейл за подател
        emailService = new SmtpOnboardingEmailService(mailSender, "no-reply@bank.bg");
    }

    @Test
    void testSendTemporaryPasswordEmail() {
        emailService.sendTemporaryPasswordEmail("client@test.com", "Ivan Ivanov", "TempPass123!");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();

        assertEquals("no-reply@bank.bg", sentMessage.getFrom());
        assertEquals("client@test.com", Objects.requireNonNull(sentMessage.getTo())[0]);
        assertEquals("Your temporary online banking password", sentMessage.getSubject());

        assertTrue(Objects.requireNonNull(sentMessage.getText()).contains("Ivan Ivanov"));
        assertTrue(sentMessage.getText().contains("TempPass123!"));
    }

    @Test
    void testSendPasswordResetEmail() {
        emailService.sendPasswordResetEmail("client@test.com", "Ivan Ivanov", "http://localhost:5173/reset-password?token=abc");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();

        assertEquals("no-reply@bank.bg", sentMessage.getFrom());
        assertEquals("client@test.com", Objects.requireNonNull(sentMessage.getTo())[0]);
        assertEquals("Reset your online banking password", sentMessage.getSubject());

        assertTrue(Objects.requireNonNull(sentMessage.getText()).contains("Ivan Ivanov"));
        assertTrue(sentMessage.getText().contains("http://localhost:5173/reset-password?token=abc"));
    }
}
