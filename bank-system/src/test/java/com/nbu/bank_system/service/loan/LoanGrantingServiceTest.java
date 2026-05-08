package com.nbu.bank_system.service.loan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nbu.bank_system.domain.enums.LoanStatus;
import com.nbu.bank_system.domain.enums.LoanType;
import com.nbu.bank_system.domain.model.customer.Customer;
import com.nbu.bank_system.domain.model.loan.Loan;
import com.nbu.bank_system.dto.loan.GrantLoanRequest;
import com.nbu.bank_system.dto.loan.LoanGrantResponse;
import com.nbu.bank_system.repository.CustomerRepository;
import com.nbu.bank_system.repository.LoanRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit test за бизнес логиката за отпускане на кредит
 * Repository зависимостите са mock-нати, за да тестваме service orchestration-а без реална база данни
 * Импленентирано е Mockito, защото service-ът има външни dependencies,
 * но бизнес поведението му може да се провери изолирано от Spring context и HTTP
 */

class LoanGrantingServiceTest {

    // Mock обекти, за да се види какво връщат repository-тата без да пишем в базата данни
    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final LoanRepository loanRepository = mock(LoanRepository.class);
    private final LoanGrantingService loanGrantingService = new LoanGrantingService(
            customerRepository,
            loanRepository,
            new LoanProductPolicy(),
            new AnnuityRepaymentScheduleGenerator()
    );

    @Test
    void grantLoanCreatesActiveLoanWithGeneratedRepaymentSchedule() {
        // Подготвяме customer и repository behavior, за да може service-ът да работи без грешки
        Customer customer = mock(Customer.class);
        when(customer.getId()).thenReturn(7L);
        when(customerRepository.findById(7L)).thenReturn(Optional.of(customer));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Извикваме service метода директно, без HTTP и без Spring context
        LoanGrantResponse response = loanGrantingService.grantLoan(new GrantLoanRequest(
                7L,
                LoanType.CONSUMER,
                BigDecimal.valueOf(12_000),
                24
        ));

        // ArgumentCaptor ни позволява да проверим какъв Loan entity е подаден към save()
        ArgumentCaptor<Loan> loanCaptor = ArgumentCaptor.forClass(Loan.class);
        verify(loanRepository).save(loanCaptor.capture());
        Loan savedLoan = loanCaptor.getValue();

        assertThat(savedLoan.getCustomer()).isSameAs(customer);
        assertThat(savedLoan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(savedLoan.getInstallments()).hasSize(24);
        assertThat(response.customerId()).isEqualTo(7L);
        assertThat(response.loanType()).isEqualTo(LoanType.CONSUMER);
        assertThat(response.annualInterestRate()).isGreaterThan(BigDecimal.valueOf(6.25));
        assertThat(response.repaymentSchedule()).hasSize(24);
        assertThat(response.repaymentSchedule().getLast().remainingBalance()).isEqualByComparingTo("0.00");
    }
}
