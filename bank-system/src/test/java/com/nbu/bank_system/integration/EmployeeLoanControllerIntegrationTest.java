package com.nbu.bank_system.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nbu.bank_system.domain.enums.InstallmentStatus;
import com.nbu.bank_system.domain.enums.LoanStatus;
import com.nbu.bank_system.domain.enums.LoanType;
import com.nbu.bank_system.domain.enums.UserRole;
import com.nbu.bank_system.domain.model.customer.IndividualCustomer;
import com.nbu.bank_system.domain.model.loan.Loan;
import com.nbu.bank_system.repository.CustomerRepository;
import com.nbu.bank_system.repository.InstallmentRepository;
import com.nbu.bank_system.repository.LoanRepository;
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
    private LoanRepository loanRepository;

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
        installmentRepository.deleteAll();
        loanRepository.deleteAll();
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
}
