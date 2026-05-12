package com.nbu.bank_system.service;

import com.nbu.bank_system.dto.auth.AuthResponse;
import com.nbu.bank_system.dto.auth.ChangePasswordRequest;
import com.nbu.bank_system.dto.auth.LoginRequest;
import com.nbu.bank_system.dto.auth.PasswordResetRequest;
import com.nbu.bank_system.dto.auth.ResetPasswordRequest;
import com.nbu.bank_system.domain.model.PasswordResetToken;
import com.nbu.bank_system.domain.model.customer.CorporateCustomer;
import com.nbu.bank_system.domain.model.customer.Customer;
import com.nbu.bank_system.domain.model.customer.IndividualCustomer;
import com.nbu.bank_system.repository.CustomerRepository;
import com.nbu.bank_system.repository.PasswordResetTokenRepository;
import com.nbu.bank_system.security.BankUserPrincipal;
import com.nbu.bank_system.security.JwtService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final int PASSWORD_RESET_TOKEN_BYTES = 32;
    private static final int PASSWORD_RESET_EXPIRATION_MINUTES = 5;

    private final CustomerRepository customerRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OnboardingEmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.frontend.password-reset-url}")
    private String passwordResetUrl;

    public AuthService(
            CustomerRepository customerRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            OnboardingEmailService emailService
    ) {
        this.customerRepository = customerRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        Customer customer = customerRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), customer.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        BankUserPrincipal principal = BankUserPrincipal.from(customer);
        String token = jwtService.generateToken(principal);

        return new AuthResponse(
                token,
                customer.getId(),
                customer.getEmail(),
                resolveDisplayName(customer),
                customer.getUserRole(),
                customer.getCustomerType(),
                customer.isFirstLogin()
        );
    }

    private String resolveDisplayName(Customer customer) {
        if (customer instanceof IndividualCustomer individualCustomer) {
            return individualCustomer.getFirstName() + " " + individualCustomer.getLastName();
        }

        if (customer instanceof CorporateCustomer corporateCustomer) {
            return corporateCustomer.getCompanyName();
        }

        return customer.getEmail();
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        Customer customer = customerRepository.findByEmailIgnoreCase(email.trim().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Authenticated user not found"));

        if (!passwordEncoder.matches(request.currentPassword(), customer.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        customer.changePassword(passwordEncoder.encode(request.newPassword()));
        customerRepository.save(customer);
    }

    @Transactional
    public void requestPasswordReset(PasswordResetRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        customerRepository.findByEmailIgnoreCase(normalizedEmail).ifPresent(customer -> {
            String token = generateResetToken();
            String tokenHash = hashToken(token);

            passwordResetTokenRepository.deleteByCustomer_IdAndUsedAtIsNull(customer.getId());
            passwordResetTokenRepository.save(new PasswordResetToken(
                    customer,
                    tokenHash,
                    LocalDateTime.now().plusMinutes(PASSWORD_RESET_EXPIRATION_MINUTES)
            ));

            emailService.sendPasswordResetEmail(customer.getEmail(), resolveDisplayName(customer), buildPasswordResetLink(token));
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHashAndUsedAtIsNull(hashToken(request.token().trim()))
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired password reset link"));

        LocalDateTime now = LocalDateTime.now();
        if (resetToken.isExpired(now)) {
            resetToken.markUsed(now);
            passwordResetTokenRepository.save(resetToken);
            throw new BadCredentialsException("Invalid or expired password reset link");
        }

        Customer customer = resetToken.getCustomer();
        customer.changePassword(passwordEncoder.encode(request.newPassword()));
        resetToken.markUsed(now);

        customerRepository.save(customer);
        passwordResetTokenRepository.save(resetToken);
    }

    private String generateResetToken() {
        byte[] tokenBytes = new byte[PASSWORD_RESET_TOKEN_BYTES];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String buildPasswordResetLink(String token) {
        return passwordResetUrl + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}

