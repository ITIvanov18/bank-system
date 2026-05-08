package com.nbu.bank_system.service.loan;

import com.nbu.bank_system.domain.model.customer.Customer;
import com.nbu.bank_system.domain.model.loan.Installment;
import com.nbu.bank_system.domain.model.loan.Loan;
import com.nbu.bank_system.domain.enums.LoanStatus;
import com.nbu.bank_system.dto.loan.GrantLoanRequest;
import com.nbu.bank_system.dto.loan.InstallmentResponse;
import com.nbu.bank_system.dto.loan.LoanGrantResponse;
import com.nbu.bank_system.repository.CustomerRepository;
import com.nbu.bank_system.repository.LoanRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Този клас координира use case-а: намира клиента, валидира кредитния продукт,
 * изчислява лихвата, генерира погасителния план и записва всичко транзакционно.
 * Реалните правила са делегирани към LoanProductPolicy и AnnuityRepaymentScheduleGenerator,
 * за да не се смесват persistence, validation и financial calculation логиките
 */

@Service
public class LoanGrantingService {

    private final CustomerRepository customerRepository;
    private final LoanRepository loanRepository;
    private final LoanProductPolicy loanProductPolicy;
    private final AnnuityRepaymentScheduleGenerator repaymentScheduleGenerator;

    public LoanGrantingService(
            CustomerRepository customerRepository,
            LoanRepository loanRepository,
            LoanProductPolicy loanProductPolicy,
            AnnuityRepaymentScheduleGenerator repaymentScheduleGenerator
    ) {
        this.customerRepository = customerRepository;
        this.loanRepository = loanRepository;
        this.loanProductPolicy = loanProductPolicy;
        this.repaymentScheduleGenerator = repaymentScheduleGenerator;
    }

    @Transactional
    public LoanGrantResponse grantLoan(GrantLoanRequest request) {
        // Кредит може да бъде отпуснат само на реално съществуващ customer
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer was not found."));

        // Product policy пази лимитите и правилата за конкретния LoanType
        loanProductPolicy.validateLoanRequest(
                request.loanType(),
                request.principalAmount(),
                request.repaymentTermMonths()
        );

        BigDecimal annualInterestRate = loanProductPolicy.calculateAnnualInterestRate(
                request.loanType(),
                request.principalAmount(),
                request.repaymentTermMonths()
        );
        LocalDate startDate = LocalDate.now();
        Loan loan = new Loan(
                customer,
                request.loanType(),
                request.principalAmount(),
                annualInterestRate,
                request.repaymentTermMonths(),
                LoanStatus.ACTIVE,
                startDate
        );

        List<Installment> installments = repaymentScheduleGenerator.generate(
                request.principalAmount(),
                annualInterestRate,
                request.repaymentTermMonths(),
                startDate
        );
        installments.forEach(loan::addInstallment);

        // CascadeType.ALL в Loan->Installment записва и погасителния план заедно с кредита
        Loan savedLoan = loanRepository.save(loan);
        return toLoanGrantResponse(savedLoan);
    }

    private LoanGrantResponse toLoanGrantResponse(Loan loan) {
        // Response DTO пази API слоя отделен от JPA entity моделите и форматира данните за клиента
        List<InstallmentResponse> repaymentSchedule = loan.getInstallments().stream()
                .sorted(Comparator.comparing(Installment::getInstallmentNumber))
                .map(this::toInstallmentResponse)
                .toList();
        BigDecimal monthlyInstallmentAmount = repaymentSchedule.isEmpty()
                ? BigDecimal.ZERO
                : repaymentSchedule.getFirst().monthlyInstallmentAmount();

        return new LoanGrantResponse(
                loan.getId(),
                loan.getCustomer().getId(),
                loan.getLoanType(),
                loan.getPrincipalAmount(),
                loan.getAnnualInterestRate(),
                loan.getRepaymentTermMonths(),
                loan.getStatus(),
                loan.getStartDate(),
                monthlyInstallmentAmount,
                repaymentSchedule,
                "Loan granted successfully."
        );
    }

    private InstallmentResponse toInstallmentResponse(Installment installment) {
        return new InstallmentResponse(
                installment.getId(),
                installment.getInstallmentNumber(),
                installment.getDueDate(),
                installment.getMonthlyInstallmentAmount(),
                installment.getPrincipalPart(),
                installment.getInterestPart(),
                installment.getRemainingBalance(),
                installment.getStatus()
        );
    }
}
