package com.nbu.bank_system.domain.model.loan;

import com.nbu.bank_system.domain.model.BaseEntity;
import com.nbu.bank_system.domain.model.customer.Customer;
import com.nbu.bank_system.domain.enums.LoanStatus;
import com.nbu.bank_system.domain.enums.LoanType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Представя loan или loan application, свързан с конкретен customer.
 * Съдържа principal, loan type, interest configuration, repayment term и lifecycle status, а domain методите управляват approve/reject/close преходите.
 */

@Getter
@Entity
@Table(name = "loans")
public class Loan extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "customer_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_loan_customer")
    )
    private Customer customer;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", nullable = false, length = 20)
    private LoanType loanType;

    @NotNull
    @Digits(integer = 19, fraction = 2)
    @DecimalMin(value = "0.01")
    @Column(name = "principal_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal principalAmount;

    @NotNull
    @Digits(integer = 5, fraction = 4)
    @DecimalMin(value = "0.0001")
    @Column(name = "annual_interest_rate", nullable = false, precision = 9, scale = 4)
    private BigDecimal annualInterestRate;

    @Min(1)
    @Max(360)
    @Column(name = "repayment_term_months", nullable = false)
    private Integer repaymentTermMonths;

    @Setter
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LoanStatus status;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Installment> installments = new ArrayList<>();

    protected Loan() {
    }

    public Loan(
            Customer customer,
            LoanType loanType,
            BigDecimal principalAmount,
            BigDecimal annualInterestRate,
            Integer repaymentTermMonths,
            LoanStatus status,
            LocalDate startDate
    ) {
        this.customer = customer;
        this.loanType = loanType;
        this.principalAmount = principalAmount;
        this.annualInterestRate = annualInterestRate;
        this.repaymentTermMonths = repaymentTermMonths;
        this.status = status;
        this.startDate = startDate;
    }

    public void approve(LocalDate startDate, LocalDateTime reviewedAt) {
        this.status = LoanStatus.ACTIVE;
        this.startDate = startDate;
        this.reviewedAt = reviewedAt;
    }

    public void reject(LocalDateTime reviewedAt) {
        this.status = LoanStatus.REJECTED;
        this.reviewedAt = reviewedAt;
    }

    public void close() {
        this.status = LoanStatus.CLOSED;
    }

    public void addInstallment(Installment installment) {
        installment.setLoan(this);
        this.installments.add(installment);
    }
}
