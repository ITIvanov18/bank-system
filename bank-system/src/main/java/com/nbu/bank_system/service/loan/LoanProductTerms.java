package com.nbu.bank_system.service.loan;

import com.nbu.bank_system.domain.enums.LoanType;
import java.math.BigDecimal;

/**
 * Immutable value object за параметрите на конкретен кредитен продукт.
 * LoanProductPolicy го използва като единен източник за лимити, базова лихва и допустим repayment term диапазон.
 */

public record LoanProductTerms(
        LoanType loanType,
        BigDecimal baseAnnualInterestRate,
        BigDecimal minimumAnnualInterestRate,
        BigDecimal maximumAnnualInterestRate,
        BigDecimal minimumPrincipalAmount,
        BigDecimal maximumPrincipalAmount,
        BigDecimal principalStepAmount,
        Integer minimumRepaymentTermMonths,
        int maximumRepaymentTermMonths
) {
}
