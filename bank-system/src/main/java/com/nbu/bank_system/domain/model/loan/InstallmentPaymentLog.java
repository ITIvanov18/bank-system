package com.nbu.bank_system.domain.model.loan;

import com.nbu.bank_system.domain.model.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "installment_payments_log")
public class InstallmentPaymentLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "installment_id", nullable = false)
    private Installment installment;

    @Column(name = "amount_paid", nullable = false, precision = 19, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;

    protected InstallmentPaymentLog() {
    }

    public InstallmentPaymentLog(Loan loan, Installment installment, BigDecimal amountPaid, LocalDateTime paidAt) {
        this.loan = loan;
        this.installment = installment;
        this.amountPaid = amountPaid;
        this.paidAt = paidAt;
    }

    public Loan getLoan() {
        return loan;
    }

    public Installment getInstallment() {
        return installment;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }
}

