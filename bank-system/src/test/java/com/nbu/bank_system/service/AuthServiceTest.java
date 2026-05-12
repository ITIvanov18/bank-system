package com.nbu.bank_system.service;

import com.nbu.bank_system.domain.enums.UserRole;
import com.nbu.bank_system.domain.model.customer.IndividualCustomer;
import com.nbu.bank_system.dto.auth.AuthResponse;
import com.nbu.bank_system.dto.auth.ChangePasswordRequest;
import com.nbu.bank_system.dto.auth.LoginRequest;
import com.nbu.bank_system.repository.CustomerRepository;
import com.nbu.bank_system.repository.PasswordResetTokenRepository;
import com.nbu.bank_system.security.BankUserPrincipal;
import com.nbu.bank_system.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OnboardingEmailService emailService;

    @InjectMocks
    private AuthService authService;

    @Test
    void testLogin_Success() {
        String email = "test@bank.bg";
        String password = "Password123!";
        LoginRequest request = new LoginRequest(email, password);

        IndividualCustomer mockCustomer = new IndividualCustomer("Ivan", "Ivanov", "0000000000");
        mockCustomer.assignOnlineBankingCredentials(email, "hashed_password", true, UserRole.CUSTOMER);

        when(customerRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(mockCustomer));
        when(passwordEncoder.matches(password, "hashed_password")).thenReturn(true);
        when(jwtService.generateToken(any(BankUserPrincipal.class))).thenReturn("mocked-jwt-token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mocked-jwt-token", response.token()); // Проверяваме токена
        assertEquals(email, response.email());              // Проверяваме имейла
        assertEquals(UserRole.CUSTOMER, response.role());   // Проверяваме ролята
        assertTrue(response.firstLogin());                  // Проверяваме флага за първи логин
    }

    @Test
    void testChangePassword_Success() {
        String email = "test@bank.bg";
        String currentPassword = "OldPassword123!";
        String newPassword = "NewPassword123!";
        ChangePasswordRequest request = new ChangePasswordRequest(currentPassword, newPassword);

        IndividualCustomer mockCustomer = new IndividualCustomer("Ivan", "Ivanov", "0000000000");
        mockCustomer.assignOnlineBankingCredentials(email, "old_hashed_password", true, UserRole.CUSTOMER);

        when(customerRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(mockCustomer));
        when(passwordEncoder.matches(currentPassword, "old_hashed_password")).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn("new_hashed_password");

        authService.changePassword(email, request);

        assertEquals("new_hashed_password", mockCustomer.getPasswordHash());
        verify(customerRepository, times(1)).save(mockCustomer);
    }

    @Test
    void testChangePassword_WrongCurrentPassword_ThrowsException() {
        String email = "test@bank.bg";
        ChangePasswordRequest request = new ChangePasswordRequest("WrongPass", "NewPass");
        IndividualCustomer mockCustomer = new IndividualCustomer("Ivan", "Ivanov", "0000000000");
        mockCustomer.assignOnlineBankingCredentials(email, "old_hashed", true, UserRole.CUSTOMER);

        when(customerRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(mockCustomer));
        when(passwordEncoder.matches(request.currentPassword(), "old_hashed")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.changePassword(email, request));
        verify(customerRepository, never()).save(any());
    }
}
