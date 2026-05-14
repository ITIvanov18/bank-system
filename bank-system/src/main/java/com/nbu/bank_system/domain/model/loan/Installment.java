package com.nbu.bank_system.domain.model.loan;

import com.nbu.bank_system.domain.model.BaseEntity;
import com.nbu.bank_system.domain.enums.InstallmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Представя единична месечна вноска от repayment schedule-а на заема.
 * Съхранява разбивка на анюитетна вноска, оставащ баланс, падежна дата и payment status за клиентския погасителен план.
 */

@Entity
@Table(
        name = "installments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_installment_loan_installment_number",
                        columnNames = {"loan_id", "installment_number"}
                )
        }
)
public class Installment extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "loan_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_installment_loan")
    )
    private Loan loan;

    @NotNull
    @Min(1)
    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber;

    @NotNull
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @NotNull
    @Digits(integer = 19, fraction = 2)
    @DecimalMin(value = "0.00", inclusive = true)
    @Column(name = "monthly_installment_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlyInstallmentAmount;

    @NotNull
    @Digits(integer = 19, fraction = 2)
    @DecimalMin(value = "0.00", inclusive = true)
    @Column(name = "principal_part", nullable = false, precision = 19, scale = 2)
    private BigDecimal principalPart;

    @NotNull
    @Digits(integer = 19, fraction = 2)
    @DecimalMin(value = "0.00", inclusive = true)
    @Column(name = "interest_part", nullable = false, precision = 19, scale = 2)
    private BigDecimal interestPart;

    @NotNull
    @Digits(integer = 19, fraction = 2)
    @DecimalMin(value = "0.00", inclusive = true)
    @Column(name = "remaining_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal remainingBalance;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InstallmentStatus status = InstallmentStatus.PENDING;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    protected Installment() {
    }

    public Installment(
            Integer installmentNumber,
            LocalDate dueDate,
            BigDecimal monthlyInstallmentAmount,
            BigDecimal principalPart,
            BigDecimal interestPart,
            BigDecimal remainingBalance
    ) {
        this.installmentNumber = installmentNumber;
        this.dueDate = dueDate;
        this.monthlyInstallmentAmount = monthlyInstallmentAmount;
        this.principalPart = principalPart;
        this.interestPart = interestPart;
        this.remainingBalance = remainingBalance;
        this.status = InstallmentStatus.PENDING;
    }

    public Loan getLoan() {
        return loan;
    }

    void setLoan(Loan loan) {
        this.loan = loan;
    }

    public Integer getInstallmentNumber() {
        return installmentNumber;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public BigDecimal getMonthlyInstallmentAmount() {
        return monthlyInstallmentAmount;
    }

    public BigDecimal getPrincipalPart() {
        return principalPart;
    }

    public BigDecimal getInterestPart() {
        return interestPart;
    }

    public BigDecimal getRemainingBalance() {
        return remainingBalance;
    }

    public InstallmentStatus getStatus() {
        return status;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void markPaid(LocalDateTime paidAt) {
        this.status = InstallmentStatus.PAID;
        this.paidAt = paidAt;
    }
}

