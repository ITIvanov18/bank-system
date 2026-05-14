package com.nbu.bank_system.service.loan;

import com.nbu.bank_system.domain.model.customer.Customer;
import com.nbu.bank_system.domain.model.customer.CorporateCustomer;
import com.nbu.bank_system.domain.model.customer.IndividualCustomer;
import com.nbu.bank_system.domain.model.loan.Installment;
import com.nbu.bank_system.domain.model.loan.Loan;
import com.nbu.bank_system.domain.model.loan.LoanReviewLog;
import com.nbu.bank_system.domain.model.loan.InstallmentPaymentLog;
import com.nbu.bank_system.domain.model.account.BankAccount;
import com.nbu.bank_system.domain.enums.AccountStatus;
import com.nbu.bank_system.domain.enums.LoanStatus;
import com.nbu.bank_system.domain.enums.LoanReviewDecision;
import com.nbu.bank_system.dto.loan.CustomerLoanApplicationStatusResponse;
import com.nbu.bank_system.dto.loan.GrantLoanRequest;
import com.nbu.bank_system.dto.loan.InstallmentResponse;
import com.nbu.bank_system.dto.loan.LoanGrantResponse;
import com.nbu.bank_system.dto.loan.LoanReviewLogResponse;
import com.nbu.bank_system.dto.loan.InstallmentPaymentLogResponse;
import com.nbu.bank_system.dto.loan.SubmitLoanApplicationRequest;
import com.nbu.bank_system.repository.BankAccountRepository;
import com.nbu.bank_system.repository.CustomerRepository;
import com.nbu.bank_system.repository.LoanRepository;
import com.nbu.bank_system.repository.LoanReviewLogRepository;
import com.nbu.bank_system.repository.InstallmentPaymentLogRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
    private final BankAccountRepository bankAccountRepository;
    private final LoanRepository loanRepository;
    private final LoanReviewLogRepository loanReviewLogRepository;
    private final InstallmentPaymentLogRepository installmentPaymentLogRepository;
    private final LoanProductPolicy loanProductPolicy;
    private final AnnuityRepaymentScheduleGenerator repaymentScheduleGenerator;

    public LoanGrantingService(
            CustomerRepository customerRepository,
            BankAccountRepository bankAccountRepository,
            LoanRepository loanRepository,
            LoanReviewLogRepository loanReviewLogRepository,
            InstallmentPaymentLogRepository installmentPaymentLogRepository,
            LoanProductPolicy loanProductPolicy,
            AnnuityRepaymentScheduleGenerator repaymentScheduleGenerator
    ) {
        this.customerRepository = customerRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.loanRepository = loanRepository;
        this.loanReviewLogRepository = loanReviewLogRepository;
        this.installmentPaymentLogRepository = installmentPaymentLogRepository;
        this.loanProductPolicy = loanProductPolicy;
        this.repaymentScheduleGenerator = repaymentScheduleGenerator;
    }

    @Transactional
    public LoanGrantResponse submitLoanApplication(String customerEmail, SubmitLoanApplicationRequest request) {
        Customer customer = customerRepository.findByEmailIgnoreCase(customerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Customer was not found."));
        if (!bankAccountRepository.existsByOwnerIdAndStatus(customer.getId(), AccountStatus.ACTIVE)) {
            throw new IllegalArgumentException("Customer must have an active bank account before applying for a loan.");
        }
        if (loanRepository.existsByCustomerIdAndStatus(customer.getId(), LoanStatus.PENDING)) {
            throw new IllegalArgumentException("You already have a loan application waiting for employee review.");
        }

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

        Loan loan = new Loan(
                customer,
                request.loanType(),
                request.principalAmount(),
                annualInterestRate,
                request.repaymentTermMonths(),
                LoanStatus.PENDING,
                null
        );

        Loan savedLoan = loanRepository.save(loan);
        return toLoanGrantResponse(savedLoan, "Loan application submitted for employee review.");
    }

    @Transactional(readOnly = true)
    public Optional<CustomerLoanApplicationStatusResponse> getLatestCustomerLoanApplication(String customerEmail) {
        Customer customer = customerRepository.findByEmailIgnoreCase(customerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Customer was not found."));

        return loanRepository.findFirstByCustomerIdOrderByCreatedAtDesc(customer.getId())
                .map(this::toCustomerLoanApplicationStatusResponse);
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

        BankAccount activeAccount = getActiveAccount(customer.getId());
        List<Installment> installments = repaymentScheduleGenerator.generate(
                request.principalAmount(),
                annualInterestRate,
                request.repaymentTermMonths(),
                startDate
        );
        installments.forEach(loan::addInstallment);
        activeAccount.credit(request.principalAmount());

        // CascadeType.ALL в Loan->Installment записва и погасителния план заедно с кредита
        Loan savedLoan = loanRepository.save(loan);
        return toLoanGrantResponse(savedLoan, "Loan granted successfully.");
    }

    @Transactional(readOnly = true)
    public List<LoanGrantResponse> getPendingApplications() {
        return loanRepository.findByStatusOrderByCreatedAtAsc(LoanStatus.PENDING).stream()
                .map(loan -> toLoanGrantResponse(loan, "Loan application is waiting for employee review."))
                .toList();
    }

    public List<LoanGrantResponse> getAllCustomerLoans(String customerEmail) {
        Customer customer = customerRepository.findByEmailIgnoreCase(customerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        return loanRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId()).stream()
                .map(loan -> toLoanGrantResponse(loan, "Loaded from records."))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InstallmentPaymentLogResponse> getCustomerPaymentLogs(String customerEmail) {
        Customer customer = customerRepository.findByEmailIgnoreCase(customerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        return installmentPaymentLogRepository.findAllByCustomerIdOrderByPaidAtDesc(customer.getId()).stream()
                .map(log -> new InstallmentPaymentLogResponse(
                        log.getId(),
                        log.getLoan().getId(),
                        log.getInstallment().getId(),
                        log.getInstallment().getInstallmentNumber(),
                        log.getAmountPaid(),
                        log.getPaidAt()
                ))
                .toList();
    }

    @Transactional
    public LoanGrantResponse repayInstallment(Long loanId, String customerEmail) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found"));

        if (!loan.getCustomer().getEmail().equalsIgnoreCase(customerEmail)) {
            throw new IllegalArgumentException("You don't own this loan.");
        }

        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new IllegalStateException("Only active loans can be repaid.");
        }

        Installment nextInstallment = loan.getInstallments().stream()
                .filter(i -> i.getStatus() == com.nbu.bank_system.domain.enums.InstallmentStatus.PENDING)
                .min(Comparator.comparing(Installment::getInstallmentNumber))
                .orElseThrow(() -> new IllegalStateException("No pending installments left."));

        BankAccount activeAccount = getActiveAccount(loan.getCustomer().getId());
        activeAccount.debit(nextInstallment.getMonthlyInstallmentAmount());
        
        LocalDateTime payTime = LocalDateTime.now();
        nextInstallment.markPaid(payTime);
        
        installmentPaymentLogRepository.save(new InstallmentPaymentLog(
                loan,
                nextInstallment,
                nextInstallment.getMonthlyInstallmentAmount(),
                payTime
        ));
        
        boolean allPaid = loan.getInstallments().stream().allMatch(i -> i.getStatus() == com.nbu.bank_system.domain.enums.InstallmentStatus.PAID);
        if (allPaid) {
            loan.close();
        }

        Loan savedLoan = loanRepository.save(loan);
        return toLoanGrantResponse(savedLoan, "Installment paid successfully.");
    }

    @Transactional
    public LoanGrantResponse approveApplication(Long loanId, String employeeEmail, String decisionNote) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan application was not found."));

        ensurePendingApplication(loan);

        LocalDate startDate = LocalDate.now();
        loan.approve(startDate, LocalDateTime.now());
        BankAccount activeAccount = getActiveAccount(loan.getCustomer().getId());

        List<Installment> installments = repaymentScheduleGenerator.generate(
                loan.getPrincipalAmount(),
                loan.getAnnualInterestRate(),
                loan.getRepaymentTermMonths(),
                startDate
        );
        installments.forEach(loan::addInstallment);
        activeAccount.credit(loan.getPrincipalAmount());

        Loan savedLoan = loanRepository.save(loan);
        loanReviewLogRepository.save(new LoanReviewLog(
                savedLoan,
                savedLoan.getCustomer(),
                LoanReviewDecision.APPROVED,
                employeeEmail,
                normalizeDecisionNote(decisionNote)
        ));

        return toLoanGrantResponse(savedLoan, "Loan application approved.");
    }

    @Transactional
    public LoanGrantResponse rejectApplication(Long loanId, String employeeEmail, String decisionNote) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan application was not found."));

        ensurePendingApplication(loan);
        loan.reject(LocalDateTime.now());

        Loan savedLoan = loanRepository.save(loan);
        loanReviewLogRepository.save(new LoanReviewLog(
                savedLoan,
                savedLoan.getCustomer(),
                LoanReviewDecision.REJECTED,
                employeeEmail,
                normalizeDecisionNote(decisionNote)
        ));

        return toLoanGrantResponse(savedLoan, "Loan application rejected.");
    }

    @Transactional(readOnly = true)
    public List<LoanReviewLogResponse> getReviewHistory() {
        return loanReviewLogRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toLoanReviewLogResponse)
                .toList();
    }

    private LoanGrantResponse toLoanGrantResponse(Loan loan, String message) {
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
                loan.getCustomer().getEmail(),
                resolveCustomerDisplayName(loan.getCustomer()),
                loan.getCustomer().getCustomerType(),
                loan.getLoanType(),
                loan.getPrincipalAmount(),
                loan.getAnnualInterestRate(),
                loan.getRepaymentTermMonths(),
                loan.getStatus(),
                loan.getStartDate(),
                loan.getReviewedAt(),
                monthlyInstallmentAmount,
                repaymentSchedule,
                message
        );
    }

    public CustomerLoanApplicationStatusResponse toCustomerLoanApplicationStatusResponse(Loan loan) {
        List<InstallmentResponse> repaymentSchedule = loan.getInstallments().stream()
                .sorted(Comparator.comparing(Installment::getInstallmentNumber))
                .map(this::toInstallmentResponse)
                .toList();
        BigDecimal monthlyInstallmentAmount = repaymentSchedule.isEmpty()
                ? BigDecimal.ZERO
                : repaymentSchedule.getFirst().monthlyInstallmentAmount();

        return new CustomerLoanApplicationStatusResponse(
                loan.getLoanType(),
                loan.getPrincipalAmount(),
                loan.getAnnualInterestRate(),
                loan.getRepaymentTermMonths(),
                loan.getStatus(),
                loan.getStartDate(),
                loan.getCreatedAt(),
                loan.getReviewedAt(),
                monthlyInstallmentAmount,
                resolveCustomerLoanStatusMessage(loan)
        );
    }

    private String resolveCustomerLoanStatusMessage(Loan loan) {
        return switch (loan.getStatus()) {
            case PENDING -> "Your loan application is waiting for employee review.";
            case REJECTED -> "Your loan application was not approved.";
            case ACTIVE -> "Your loan application was approved and funds were transferred.";
            case CLOSED -> "This loan has been closed.";
        };
    }

    private LoanReviewLogResponse toLoanReviewLogResponse(LoanReviewLog log) {
        return new LoanReviewLogResponse(
                log.getId(),
                log.getLoan().getId(),
                log.getCustomer().getId(),
                log.getCustomerEmail(),
                log.getEmployeeEmail(),
                log.getDecision(),
                log.getLoanType(),
                log.getPrincipalAmount(),
                log.getAnnualInterestRate(),
                log.getRepaymentTermMonths(),
                log.getDecisionNote(),
                log.getCreatedAt()
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
                installment.getStatus(),
                installment.getPaidAt()
        );
    }

    private String resolveCustomerDisplayName(Customer customer) {
        if (customer instanceof IndividualCustomer individualCustomer) {
            return individualCustomer.getFirstName() + " " + individualCustomer.getLastName();
        }

        if (customer instanceof CorporateCustomer corporateCustomer) {
            return corporateCustomer.getCompanyName();
        }

        return customer.getEmail();
    }

    private void ensurePendingApplication(Loan loan) {
        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new IllegalStateException("Only pending loan applications can be reviewed.");
        }
    }

    private BankAccount getActiveAccount(Long customerId) {
        return bankAccountRepository.findFirstByOwnerIdAndStatusOrderByIdAsc(customerId, AccountStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Customer must have an active bank account before receiving loan funds."));
    }

    private String normalizeDecisionNote(String decisionNote) {
        if (decisionNote == null || decisionNote.isBlank()) {
            return null;
        }
        return decisionNote.trim();
    }
}
