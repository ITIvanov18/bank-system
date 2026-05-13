package com.nbu.bank_system.service.loan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nbu.bank_system.domain.enums.AccountStatus;
import com.nbu.bank_system.domain.enums.LoanReviewDecision;
import com.nbu.bank_system.domain.enums.LoanStatus;
import com.nbu.bank_system.domain.enums.LoanType;
import com.nbu.bank_system.domain.model.customer.Customer;
import com.nbu.bank_system.domain.model.loan.Loan;
import com.nbu.bank_system.domain.model.loan.LoanReviewLog;
import com.nbu.bank_system.dto.loan.GrantLoanRequest;
import com.nbu.bank_system.dto.loan.LoanGrantResponse;
import com.nbu.bank_system.dto.loan.SubmitLoanApplicationRequest;
import com.nbu.bank_system.repository.BankAccountRepository;
import com.nbu.bank_system.repository.CustomerRepository;
import com.nbu.bank_system.repository.LoanRepository;
import com.nbu.bank_system.repository.LoanReviewLogRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;


class LoanGrantingServiceTest {

    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final BankAccountRepository bankAccountRepository = mock(BankAccountRepository.class);
    private final LoanRepository loanRepository = mock(LoanRepository.class);
    private final LoanReviewLogRepository loanReviewLogRepository = mock(LoanReviewLogRepository.class);
    private final LoanGrantingService loanGrantingService = new LoanGrantingService(
            customerRepository,
            bankAccountRepository,
            loanRepository,
            loanReviewLogRepository,
            new LoanProductPolicy(),
            new AnnuityRepaymentScheduleGenerator()
    );

    @Test
    void grantLoanCreatesActiveLoanWithGeneratedRepaymentSchedule() {
        Customer customer = mock(Customer.class);
        when(customer.getId()).thenReturn(7L);
        when(customerRepository.findById(7L)).thenReturn(Optional.of(customer));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoanGrantResponse response = loanGrantingService.grantLoan(new GrantLoanRequest(
                7L,
                LoanType.CONSUMER,
                BigDecimal.valueOf(12_000),
                24
        ));

       ArgumentCaptor<Loan> loanCaptor = ArgumentCaptor.forClass(Loan.class);
        verify(loanRepository).save(loanCaptor.capture());
        Loan savedLoan = loanCaptor.getValue();

        assertThat(savedLoan.getCustomer()).isSameAs(customer);
        assertThat(savedLoan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(savedLoan.getInstallments()).hasSize(24);
        assertThat(response.customerId()).isEqualTo(7L);
        assertThat(response.loanType()).isEqualTo(LoanType.CONSUMER);
        assertThat(response.annualInterestRate()).isBetween(BigDecimal.valueOf(5.20), BigDecimal.valueOf(6.70));
        assertThat(response.repaymentSchedule()).hasSize(24);
        assertThat(response.repaymentSchedule().getLast().remainingBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void submitLoanApplicationCreatesPendingLoanWithoutRepaymentSchedule() {
        Customer customer = mock(Customer.class);
        when(customer.getId()).thenReturn(7L);
        when(customerRepository.findByEmailIgnoreCase("client@bank.bg")).thenReturn(Optional.of(customer));
        when(bankAccountRepository.existsByOwnerIdAndStatus(7L, AccountStatus.ACTIVE)).thenReturn(true);
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoanGrantResponse response = loanGrantingService.submitLoanApplication(
                "client@bank.bg",
                new SubmitLoanApplicationRequest(
                        LoanType.CONSUMER,
                        BigDecimal.valueOf(12_000),
                        24
                )
        );

        ArgumentCaptor<Loan> loanCaptor = ArgumentCaptor.forClass(Loan.class);
        verify(loanRepository).save(loanCaptor.capture());
        Loan savedLoan = loanCaptor.getValue();

        assertThat(savedLoan.getStatus()).isEqualTo(LoanStatus.PENDING);
        assertThat(savedLoan.getStartDate()).isNull();
        assertThat(savedLoan.getInstallments()).isEmpty();
        assertThat(response.status()).isEqualTo(LoanStatus.PENDING);
        assertThat(response.repaymentSchedule()).isEmpty();
    }

    @Test
    void approveApplicationActivatesLoanGeneratesScheduleAndWritesLog() {
        Customer customer = mock(Customer.class);
        when(customer.getId()).thenReturn(7L);
        when(customer.getEmail()).thenReturn("client@bank.bg");
        Loan pendingLoan = new Loan(
                customer,
                LoanType.CONSUMER,
                BigDecimal.valueOf(12_000),
                BigDecimal.valueOf(6.0000),
                24,
                LoanStatus.PENDING,
                null
        );
        when(loanRepository.findById(99L)).thenReturn(Optional.of(pendingLoan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoanGrantResponse response = loanGrantingService.approveApplication(
                99L,
                "employee@bank.bg",
                "Looks good"
        );

        ArgumentCaptor<LoanReviewLog> logCaptor = ArgumentCaptor.forClass(LoanReviewLog.class);
        verify(loanReviewLogRepository).save(logCaptor.capture());

        assertThat(response.status()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(response.startDate()).isNotNull();
        assertThat(response.reviewedAt()).isNotNull();
        assertThat(response.repaymentSchedule()).hasSize(24);
        assertThat(logCaptor.getValue().getDecision()).isEqualTo(LoanReviewDecision.APPROVED);
        assertThat(logCaptor.getValue().getEmployeeEmail()).isEqualTo("employee@bank.bg");
    }
}
