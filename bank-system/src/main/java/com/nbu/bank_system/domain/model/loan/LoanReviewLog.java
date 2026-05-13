package com.nbu.bank_system.domain.model.loan;

import com.nbu.bank_system.domain.enums.LoanReviewDecision;
import com.nbu.bank_system.domain.enums.LoanType;
import com.nbu.bank_system.domain.model.BaseEntity;
import com.nbu.bank_system.domain.model.customer.Customer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Entity
@Table(name = "loan_review_logs")
public class LoanReviewLog extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "loan_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_loan_review_log_loan")
    )
    private Loan loan;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "customer_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_loan_review_log_customer")
    )
    private Customer customer;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 20)
    private LoanReviewDecision decision;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", nullable = false, length = 20)
    private LoanType loanType;

    @NotNull
    @Digits(integer = 19, fraction = 2)
    @Column(name = "principal_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal principalAmount;

    @NotNull
    @Digits(integer = 5, fraction = 4)
    @Column(name = "annual_interest_rate", nullable = false, precision = 9, scale = 4)
    private BigDecimal annualInterestRate;

    @NotNull
    @Column(name = "repayment_term_months", nullable = false)
    private Integer repaymentTermMonths;

    @NotNull
    @Size(max = 255)
    @Column(name = "customer_email", nullable = false, length = 255)
    private String customerEmail;

    @NotNull
    @Size(max = 255)
    @Column(name = "employee_email", nullable = false, length = 255)
    private String employeeEmail;

    @Size(max = 500)
    @Column(name = "decision_note", length = 500)
    private String decisionNote;

    protected LoanReviewLog() {
    }

    public LoanReviewLog(
            Loan loan,
            Customer customer,
            LoanReviewDecision decision,
            String employeeEmail,
            String decisionNote
    ) {
        this.loan = loan;
        this.customer = customer;
        this.decision = decision;
        this.loanType = loan.getLoanType();
        this.principalAmount = loan.getPrincipalAmount();
        this.annualInterestRate = loan.getAnnualInterestRate();
        this.repaymentTermMonths = loan.getRepaymentTermMonths();
        this.customerEmail = customer.getEmail();
        this.employeeEmail = employeeEmail;
        this.decisionNote = decisionNote;
    }

    public Loan getLoan() {
        return loan;
    }

    public Customer getCustomer() {
        return customer;
    }

    public LoanReviewDecision getDecision() {
        return decision;
    }

    public LoanType getLoanType() {
        return loanType;
    }

    public BigDecimal getPrincipalAmount() {
        return principalAmount;
    }

    public BigDecimal getAnnualInterestRate() {
        return annualInterestRate;
    }

    public Integer getRepaymentTermMonths() {
        return repaymentTermMonths;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public String getEmployeeEmail() {
        return employeeEmail;
    }

    public String getDecisionNote() {
        return decisionNote;
    }
}
