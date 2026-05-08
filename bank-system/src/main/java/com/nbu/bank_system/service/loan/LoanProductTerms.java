package com.nbu.bank_system.service.loan;

import com.nbu.bank_system.domain.enums.LoanType;
import java.math.BigDecimal;

public record LoanProductTerms(
        LoanType loanType,
        BigDecimal baseAnnualInterestRate,
        BigDecimal maximumPrincipalAmount,
        int maximumRepaymentTermMonths
) {
}
