package com.nbu.bank_system.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nbu.bank_system.domain.enums.InstallmentStatus;
import com.nbu.bank_system.domain.enums.AccountStatus;
import com.nbu.bank_system.domain.enums.LoanStatus;
import com.nbu.bank_system.domain.enums.LoanType;
import com.nbu.bank_system.domain.enums.UserRole;
import com.nbu.bank_system.domain.model.account.BankAccount;
import com.nbu.bank_system.domain.model.customer.IndividualCustomer;
import com.nbu.bank_system.domain.model.loan.Loan;
import com.nbu.bank_system.repository.BankAccountRepository;
import com.nbu.bank_system.repository.CustomerRepository;
import com.nbu.bank_system.repository.InstallmentRepository;
import com.nbu.bank_system.repository.LoanRepository;
import com.nbu.bank_system.repository.LoanReviewLogRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class EmployeeLoanControllerIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("bank_system_test")
            .withUsername("bank_user")
            .withPassword("bank_password");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private LoanReviewLogRepository loanReviewLogRepository;

    @Autowired
    private InstallmentRepository installmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private IndividualCustomer customer;

    @DynamicPropertySource
    static void registerTestProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);

        registry.add("spring.flyway.enabled", () -> "false");
       registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");


        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("app.admin.secret", () -> "integration-test-secret");
        registry.add("spring.mail.host", () -> "localhost");
        registry.add("spring.mail.port", () -> "1025");
        registry.add("spring.mail.username", () -> "");
        registry.add("spring.mail.password", () -> "");
        registry.add("spring.mail.properties.mail.smtp.auth", () -> "false");
        registry.add("spring.mail.properties.mail.smtp.starttls.enable", () -> "false");
        registry.add("spring.mail.properties.mail.smtp.ssl.enable", () -> "false");
    }

    @BeforeEach
    void setUp() {
        loanReviewLogRepository.deleteAll();
        installmentRepository.deleteAll();
        loanRepository.deleteAll();
        bankAccountRepository.deleteAll();
        customerRepository.deleteAll();

        customer = new IndividualCustomer("Grigor", "Kamenov", "0248250090");
        customer.assignOnlineBankingCredentials(
                "F115436@students.nbu.bg",
                passwordEncoder.encode("Password@67"),
                false,
                UserRole.CUSTOMER
        );
        customer = customerRepository.save(customer);
    }

    @Test
    void grantLoanPersistsLoanAndRepaymentScheduleThroughRealApplicationLayers() throws Exception {
        BankAccount account = bankAccountRepository.save(new BankAccount(
                "BG99BANK12345678901235",
                BigDecimal.valueOf(1000),
                AccountStatus.ACTIVE,
                customer
        ));

        String requestBody = """
                {
                  "customerId": %d,
                  "loanType": "CONSUMER",
                  "principalAmount": 12000.00,
                  "repaymentTermMonths": 24
                }
                """.formatted(customer.getId());

        mockMvc.perform(post("/api/employee/loans/grant")
                        .with(user("employee@bankai.bg").roles("EMPLOYEE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(customer.getId()))
                .andExpect(jsonPath("$.loanType").value("CONSUMER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.repaymentSchedule.length()").value(24))
                .andExpect(jsonPath("$.repaymentSchedule[0].status").value("PENDING"))
                .andExpect(jsonPath("$.repaymentSchedule[23].remainingBalance").value(0.00));

        List<Loan> loans = loanRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId());

        assertThat(loans).hasSize(1);
        Loan persistedLoan = loans.getFirst();
        assertThat(persistedLoan.getLoanType()).isEqualTo(LoanType.CONSUMER);
        assertThat(persistedLoan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(persistedLoan.getAnnualInterestRate()).isBetween(
                BigDecimal.valueOf(5.20),
                BigDecimal.valueOf(6.70)
        );

        assertThat(installmentRepository.findByLoanIdOrderByInstallmentNumberAsc(persistedLoan.getId()))
                .hasSize(24)
                .first()
                .satisfies(installment -> {
                    assertThat(installment.getInstallmentNumber()).isEqualTo(1);
                    assertThat(installment.getStatus()).isEqualTo(InstallmentStatus.PENDING);
                });

        BankAccount creditedAccount = bankAccountRepository.findById(account.getId()).orElseThrow();
        assertThat(creditedAccount.getBalance()).isEqualByComparingTo("13000.00");
    }

    @Test
    void grantLoanRejectsRequestsWithoutEmployeeRole() throws Exception {
        String requestBody = """
                {
                  "customerId": %d,
                  "loanType": "MORTGAGE",
                  "principalAmount": 100000.00,
                  "repaymentTermMonths": 240
                }
                """.formatted(customer.getId());

        mockMvc.perform(post("/api/employee/loans/grant")
                        .with(user("F115436@students.nbu.bg").roles("CUSTOMER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());

        assertThat(loanRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId())).isEmpty();
    }

    @Test
    void grantLoanRejectsAmountsAboveProductLimit() throws Exception {
        String requestBody = """
                {
                  "customerId": %d,
                  "loanType": "CONSUMER",
                  "principalAmount": 40000.01,
                  "repaymentTermMonths": 120
                }
                """.formatted(customer.getId());

        mockMvc.perform(post("/api/employee/loans/grant")
                        .with(user("employee@bankai.bg").roles("EMPLOYEE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Principal amount exceeds the maximum allowed amount for CONSUMER loans."));

        assertThat(loanRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId())).isEmpty();
    }

    @Test
    void customerCanSubmitLoanApplicationAndEmployeeCanApproveItWithHistoryLog() throws Exception {
        bankAccountRepository.save(new BankAccount(
                "BG99BANK12345678901234",
                BigDecimal.valueOf(1000),
                AccountStatus.ACTIVE,
                customer
        ));

        String applicationBody = """
                {
                  "loanType": "CONSUMER",
                  "principalAmount": 12000.00,
                  "repaymentTermMonths": 24
                }
                """;

        mockMvc.perform(post("/api/customer/loans/applications")
                        .with(user("F115436@students.nbu.bg").roles("CUSTOMER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.loanType").value("CONSUMER"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.principalAmount").value(12000.00))
                .andExpect(jsonPath("$.message").value("Your loan application is waiting for employee review."))
                .andExpect(jsonPath("$.loanId").doesNotExist())
                .andExpect(jsonPath("$.customerId").doesNotExist());

        Loan pendingLoan = loanRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId()).getFirst();
        assertThat(pendingLoan.getStatus()).isEqualTo(LoanStatus.PENDING);
        assertThat(pendingLoan.getStartDate()).isNull();
        assertThat(installmentRepository.findByLoanIdOrderByInstallmentNumberAsc(pendingLoan.getId())).isEmpty();

        String decisionBody = """
                {
                  "decisionNote": "Approved after employee review."
                }
                """;

        mockMvc.perform(post("/api/employee/loans/applications/{loanId}/approve", pendingLoan.getId())
                        .with(user("employee@bankai.bg").roles("EMPLOYEE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(decisionBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.reviewedAt").exists())
                .andExpect(jsonPath("$.repaymentSchedule.length()").value(24));

        Loan approvedLoan = loanRepository.findById(pendingLoan.getId()).orElseThrow();
        assertThat(approvedLoan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(approvedLoan.getReviewedAt()).isNotNull();
        assertThat(installmentRepository.findByLoanIdOrderByInstallmentNumberAsc(approvedLoan.getId())).hasSize(24);
        assertThat(bankAccountRepository.findFirstByOwnerIdOrderByIdAsc(customer.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("13000.00");

        mockMvc.perform(get("/api/employee/loans/applications/history")
                        .with(user("employee@bankai.bg").roles("EMPLOYEE"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].loanId").value(pendingLoan.getId()))
                .andExpect(jsonPath("$[0].customerId").value(customer.getId()))
                .andExpect(jsonPath("$[0].customerEmail").value("F115436@students.nbu.bg"))
                .andExpect(jsonPath("$[0].employeeEmail").value("employee@bankai.bg"))
                .andExpect(jsonPath("$[0].decision").value("APPROVED"))
                .andExpect(jsonPath("$[0].principalAmount").value(12000.00))
                .andExpect(jsonPath("$[0].repaymentTermMonths").value(24))
                .andExpect(jsonPath("$[0].decisionNote").value("Approved after employee review."))
                .andExpect(jsonPath("$[0].decidedAt").exists());
    }
}
