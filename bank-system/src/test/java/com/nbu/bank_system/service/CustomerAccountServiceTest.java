package com.nbu.bank_system.service;

import com.nbu.bank_system.domain.enums.AccountStatus;
import com.nbu.bank_system.domain.enums.UserRole;
import com.nbu.bank_system.domain.model.account.BankAccount;
import com.nbu.bank_system.domain.model.customer.IndividualCustomer;
import com.nbu.bank_system.dto.account.AccountStatusResponse;
import com.nbu.bank_system.repository.BankAccountRepository;
import com.nbu.bank_system.repository.CustomerRepository;
import com.nbu.bank_system.repository.LoanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerAccountServiceTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private LoanRepository loanRepository;

    @InjectMocks
    private CustomerAccountService customerAccountService;

    @Test
    void testGetAccountStatus_Success() {
        String email = "test@bank.bg";

        IndividualCustomer dummyCustomer = new IndividualCustomer("Petko", "Totev", "7561300000");
        dummyCustomer.assignOnlineBankingCredentials(email, "hash123", true, UserRole.CUSTOMER);

        BankAccount mockAccount = new BankAccount(
                "BG12BNKI12345678901234",
                BigDecimal.valueOf(100.50),
                AccountStatus.ACTIVE,
                dummyCustomer
        );

        when(customerRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(dummyCustomer));
        when(bankAccountRepository.findFirstByOwnerIdAndStatusOrderByIdAsc(dummyCustomer.getId(), AccountStatus.ACTIVE))
                .thenReturn(Optional.of(mockAccount));
        when(loanRepository.calculateOutstandingPrincipalDebt(dummyCustomer.getId())).thenReturn(BigDecimal.valueOf(2500));

        AccountStatusResponse response = customerAccountService.getAccountStatus(email);

        assertNotNull(response);
        assertEquals("BG12BNKI12345678901234", response.iban());
        assertEquals(AccountStatus.ACTIVE, response.status());
        assertEquals(BigDecimal.valueOf(100.50), response.balance());
        assertEquals(BigDecimal.valueOf(2500), response.outstandingDebtAmount());

        verify(customerRepository, times(1)).findByEmailIgnoreCase(email);
        verify(bankAccountRepository, times(1))
                .findFirstByOwnerIdAndStatusOrderByIdAsc(dummyCustomer.getId(), AccountStatus.ACTIVE);
    }

    @Test
    void testGetAccountStatus_CustomerNotFound_ThrowsException() {
        String wrongEmail = "wrong@bank.bg";
        when(customerRepository.findByEmailIgnoreCase(wrongEmail)).thenReturn(Optional.empty());

        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> customerAccountService.getAccountStatus(wrongEmail));

        assertEquals("Authenticated user not found", exception.getMessage());
    }
}
