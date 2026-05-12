package com.nbu.bank_system.integration;

import com.nbu.bank_system.domain.model.customer.Customer;
import com.nbu.bank_system.dto.auth.CreateIndividualCustomerRequest;
import com.nbu.bank_system.dto.auth.OnboardingResponse;
import com.nbu.bank_system.repository.CustomerRepository;
import com.nbu.bank_system.service.CustomerOnboardingService;
import com.nbu.bank_system.service.OnboardingEmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Transactional
@Testcontainers(disabledWithoutDocker = true)
class CustomerOnboardingServiceIntegrationTest {

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
    }

    @Autowired
    private CustomerOnboardingService onboardingService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private OnboardingEmailService emailService;

    @Test
    void testIntegration_OnboardIndividual_FullFlow() {
        String newEmail = "petar@bank.bg";
        CreateIndividualCustomerRequest request = new CreateIndividualCustomerRequest(
                "Petar", "Petrov", "8502021122", newEmail
        );

        OnboardingResponse response = onboardingService.onboardIndividual(request);

        entityManager.flush();

        assertNotNull(response);
        assertEquals(newEmail, response.email());
        assertNotNull(response.customerId());

        Optional<Customer> savedCustomerOpt = customerRepository.findByEmailIgnoreCase(newEmail);
        assertTrue(savedCustomerOpt.isPresent());
        Customer savedCustomer = savedCustomerOpt.get();
        assertTrue(savedCustomer.isFirstLogin());

        verify(emailService, times(1)).sendTemporaryPasswordEmail(
                eq(newEmail),
                anyString(),
                anyString()
        );
    }
}