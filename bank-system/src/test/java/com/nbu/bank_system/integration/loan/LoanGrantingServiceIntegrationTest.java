package com.nbu.bank_system.integration.loan;

import com.nbu.bank_system.domain.enums.AccountStatus;
import com.nbu.bank_system.domain.enums.LoanType;
import com.nbu.bank_system.domain.enums.UserRole;
import com.nbu.bank_system.domain.model.account.BankAccount;
import com.nbu.bank_system.domain.model.customer.IndividualCustomer;
import com.nbu.bank_system.domain.model.loan.Loan;
import com.nbu.bank_system.dto.loan.GrantLoanRequest;
import com.nbu.bank_system.dto.loan.LoanGrantResponse;
import com.nbu.bank_system.repository.BankAccountRepository;
import com.nbu.bank_system.repository.CustomerRepository;
import com.nbu.bank_system.repository.LoanRepository;
import com.nbu.bank_system.service.loan.LoanGrantingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Testcontainers(disabledWithoutDocker = true)
class LoanGrantingServiceIntegrationTest {

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
    private LoanGrantingService loanGrantingService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void testIntegration_GrantLoan_Success() {

        IndividualCustomer customer = new IndividualCustomer("Ivan", "Ivanov", "9001010000");
        customer.assignOnlineBankingCredentials("ivan@bank.bg", "hash123", true, UserRole.CUSTOMER);
        customer = customerRepository.saveAndFlush(customer);

        BankAccount account = new BankAccount(
                "BG99NBUB12345678901234",
                BigDecimal.valueOf(100.00),
                AccountStatus.ACTIVE,
                customer
        );
        bankAccountRepository.saveAndFlush(account);

        GrantLoanRequest request = new GrantLoanRequest(
                customer.getId(),
                LoanType.CONSUMER,
                BigDecimal.valueOf(5000.00),
                60
        );


        LoanGrantResponse response = loanGrantingService.grantLoan(request);

        entityManager.flush();
        entityManager.clear();




        assertNotNull(response);
        assertNotNull(response.loanId());
        assertEquals(60, response.repaymentSchedule().size());


        Optional<Loan> savedLoan = loanRepository.findById(response.loanId());
        assertTrue(savedLoan.isPresent(), "Кредитът трябва да е записан в базата данни!");
        assertEquals(60, savedLoan.get().getInstallments().size(), "Трябва да има точно 60 генерирани вноски!");

       BankAccount updatedAccount = bankAccountRepository.findById(account.getId()).orElseThrow();
        assertEquals(0, BigDecimal.valueOf(100.00).compareTo(updatedAccount.getBalance()),
                "Балансът е 100, защото LoanGrantingService все още не превежда парите по сметката.");
    }
}