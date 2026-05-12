package com.nbu.bank_system.integration;

import com.nbu.bank_system.domain.enums.UserRole;
import com.nbu.bank_system.domain.model.customer.IndividualCustomer;
import com.nbu.bank_system.dto.auth.AuthResponse;
import com.nbu.bank_system.dto.auth.ChangePasswordRequest;
import com.nbu.bank_system.dto.auth.LoginRequest;
import com.nbu.bank_system.repository.CustomerRepository;
import com.nbu.bank_system.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Testcontainers(disabledWithoutDocker = true)
class AuthServiceIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @DynamicPropertySource
    static void registerTestProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);

        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");

        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("app.admin.secret", () -> "test-secret");

         registry.add("spring.mail.host", () -> "localhost");
        registry.add("spring.mail.port", () -> "1025");
        registry.add("spring.mail.username", () -> "");
        registry.add("spring.mail.password", () -> "");
        registry.add("spring.mail.properties.mail.smtp.auth", () -> "false");
        registry.add("spring.mail.properties.mail.smtp.starttls.enable", () -> "false");
        registry.add("spring.mail.properties.mail.smtp.ssl.enable", () -> "false");
    }

    @Autowired
    private AuthService authService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void testIntegration_Login_And_ChangePassword() {
        String email = "ivan@bank.bg";
        String initialPassword = "Password123!";
        String newPassword = "NewSecurePassword456!";

        IndividualCustomer customer = new IndividualCustomer("Ivan", "Ivanov", "9001010000");
        customer.assignOnlineBankingCredentials(email, passwordEncoder.encode(initialPassword), true, UserRole.CUSTOMER);
        customerRepository.saveAndFlush(customer);

        LoginRequest loginRequest = new LoginRequest(email, initialPassword);
        AuthResponse authResponse = authService.login(loginRequest);

        assertNotNull(authResponse);
        assertNotNull(authResponse.token());

       ChangePasswordRequest changeRequest = new ChangePasswordRequest(initialPassword, newPassword);
        authService.changePassword(email, changeRequest); // Методът е void

        IndividualCustomer updatedCustomer = (IndividualCustomer) customerRepository.findByEmailIgnoreCase(email).get();
        assertTrue(passwordEncoder.matches(newPassword, updatedCustomer.getPasswordHash()));

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
    }
}