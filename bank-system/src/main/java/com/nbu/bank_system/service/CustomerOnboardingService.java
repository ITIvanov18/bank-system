package com.nbu.bank_system.service;

import com.nbu.bank_system.dto.auth.CreateCorporateCustomerRequest;
import com.nbu.bank_system.dto.auth.CreateIndividualCustomerRequest;
import com.nbu.bank_system.dto.auth.OnboardingResponse;
import com.nbu.bank_system.domain.enums.UserRole;
import com.nbu.bank_system.domain.model.customer.CorporateCustomer;
import com.nbu.bank_system.domain.model.customer.Customer;
import com.nbu.bank_system.domain.model.customer.IndividualCustomer;
import com.nbu.bank_system.repository.CustomerRepository;
import java.security.SecureRandom;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service слой за onboarding на индивидуални и корпоративни клиенти.
 * Създава customer entity, генерира временна парола, записва hash и изпраща onboarding email в една бизнес транзакция.
 */

@Service
public class CustomerOnboardingService {

    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
    private static final int TEMP_PASSWORD_LENGTH = 12;

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final OnboardingEmailService onboardingEmailService;
    private final String mailHost;
    private final SecureRandom secureRandom = new SecureRandom();

    public CustomerOnboardingService(
            CustomerRepository customerRepository,
            PasswordEncoder passwordEncoder,
            OnboardingEmailService onboardingEmailService,
            @Value("${spring.mail.host}") String mailHost
    ) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.onboardingEmailService = onboardingEmailService;
        this.mailHost = mailHost;
    }

    @Transactional
    public OnboardingResponse onboardIndividual(CreateIndividualCustomerRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        ensureEmailIsFree(normalizedEmail);

        IndividualCustomer customer = new IndividualCustomer(
                request.firstName(),
                request.lastName(),
                request.egn()
        );

        String tempPassword = generateTemporaryPassword();
        applyCredentials(customer, normalizedEmail, tempPassword);

        Customer savedCustomer = customerRepository.save(customer);
        dispatchTemporaryPasswordEmail(savedCustomer.getEmail(), request.firstName() + " " + request.lastName(), tempPassword);

        return buildOnboardingResponse(savedCustomer);
    }

    @Transactional
    public OnboardingResponse onboardCorporate(CreateCorporateCustomerRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        ensureEmailIsFree(normalizedEmail);

        CorporateCustomer customer = new CorporateCustomer(
                request.companyName(),
                request.eik(),
                request.representativeFirstName(),
                request.representativeLastName()
        );

        String tempPassword = generateTemporaryPassword();
        applyCredentials(customer, normalizedEmail, tempPassword);

        Customer savedCustomer = customerRepository.save(customer);
        dispatchTemporaryPasswordEmail(savedCustomer.getEmail(), request.companyName(), tempPassword);

        return buildOnboardingResponse(savedCustomer);
    }

    private void applyCredentials(Customer customer, String email, String tempPassword) {
        customer.assignOnlineBankingCredentials(
                email,
                passwordEncoder.encode(tempPassword),
                true,
                UserRole.CUSTOMER
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private void ensureEmailIsFree(String email) {
        if (customerRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Customer with this email already exists");
        }
    }

    private String generateTemporaryPassword() {
        StringBuilder tempPassword = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            int index = secureRandom.nextInt(PASSWORD_CHARS.length());
            tempPassword.append(PASSWORD_CHARS.charAt(index));
        }
        return tempPassword.toString();
    }

    private void dispatchTemporaryPasswordEmail(String recipientEmail, String displayName, String tempPassword) {
        try {
            onboardingEmailService.sendTemporaryPasswordEmail(recipientEmail, displayName, tempPassword);
        } catch (MailException mailException) {
            throw new IllegalStateException("Temporary password email could not be sent", mailException);
        }
    }

    private OnboardingResponse buildOnboardingResponse(Customer customer) {
        return new OnboardingResponse(
                customer.getId(),
                customer.getEmail(),
                customer.getCustomerType(),
                true,
                resolveDeliveryChannel(),
                mailHost
        );
    }

    private String resolveDeliveryChannel() {
        String normalizedHost = mailHost.toLowerCase(Locale.ROOT);
        if ("localhost".equals(normalizedHost) || "127.0.0.1".equals(normalizedHost) || "mailhog".equals(normalizedHost)) {
            return "LOCAL_MAILHOG";
        }

        return "EXTERNAL_SMTP";
    }
}

