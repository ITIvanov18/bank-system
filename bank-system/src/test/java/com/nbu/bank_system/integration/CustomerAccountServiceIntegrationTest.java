package com.nbu.bank_system.integration;

import com.nbu.bank_system.domain.enums.AccountStatus;
import com.nbu.bank_system.domain.enums.UserRole;
import com.nbu.bank_system.domain.model.account.BankAccount;
import com.nbu.bank_system.domain.model.customer.IndividualCustomer;
import com.nbu.bank_system.dto.account.AccountStatusResponse;
import com.nbu.bank_system.repository.BankAccountRepository;
import com.nbu.bank_system.repository.CustomerRepository;
import com.nbu.bank_system.service.CustomerAccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CustomerAccountServiceIntegrationTest {

    @Autowired
    private CustomerAccountService customerAccountService;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void testIntegration_GetAccountStatus() {
        String testEmail = "test@bank.bg";

        IndividualCustomer customer = new IndividualCustomer("Ivan", "Ivanov", "9001011234");
        customer.assignOnlineBankingCredentials(testEmail, "hash123", true, UserRole.CUSTOMER);

        customer = customerRepository.saveAndFlush(customer);

        BankAccount account = new BankAccount(
                "BG99TEST12345678901234",
                BigDecimal.valueOf(500.00),
                AccountStatus.ACTIVE,
                customer
        );
        bankAccountRepository.saveAndFlush(account);

        AccountStatusResponse response = customerAccountService.getAccountStatus(testEmail);

        assertNotNull(response);
        assertEquals("BG99TEST12345678901234", response.iban());
        assertEquals(AccountStatus.ACTIVE, response.status());
        assertEquals(BigDecimal.valueOf(500.00).stripTrailingZeros(), response.balance().stripTrailingZeros());
        assertEquals(BigDecimal.ZERO.stripTrailingZeros(), response.outstandingDebtAmount().stripTrailingZeros());
    }
}
