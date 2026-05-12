package com.nbu.bank_system.service;

import com.nbu.bank_system.domain.enums.UserRole;
import com.nbu.bank_system.domain.model.customer.IndividualCustomer;
import com.nbu.bank_system.dto.auth.CreateIndividualCustomerRequest;
import com.nbu.bank_system.dto.auth.OnboardingResponse;
import com.nbu.bank_system.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerOnboardingServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OnboardingEmailService emailService;

    @InjectMocks
    private CustomerOnboardingService customerOnboardingService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(customerOnboardingService, "mailHost", "localhost");
    }

    @Test
    void testOnboardIndividual_Success() {
        String email = "ivan@bank.bg";
        CreateIndividualCustomerRequest request = new CreateIndividualCustomerRequest(
                "Ivan", "Ivanov", "9001011234", email
        );

        lenient().when(customerRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.empty());
        lenient().when(customerRepository.existsByEmailIgnoreCase(email)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_temp_password");

        IndividualCustomer mockCustomer = new IndividualCustomer("Ivan", "Ivanov", "9001011234");
        mockCustomer.assignOnlineBankingCredentials(email, "hashed_temp_password", true, UserRole.CUSTOMER);

        when(customerRepository.save(any(IndividualCustomer.class))).thenReturn(mockCustomer);

        OnboardingResponse response = customerOnboardingService.onboardIndividual(request);

        assertNotNull(response);
        assertEquals(email, response.email());
        assertTrue(response.temporaryPasswordSent());

        verify(emailService, times(1)).sendTemporaryPasswordEmail(
                eq(email),
                anyString(),
                anyString()
        );

        verify(customerRepository, times(1)).save(any(IndividualCustomer.class));
    }
}