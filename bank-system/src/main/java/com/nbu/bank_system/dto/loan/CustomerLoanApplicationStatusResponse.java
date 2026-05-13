package com.nbu.bank_system.dto.loan;

import com.nbu.bank_system.domain.enums.LoanStatus;
import com.nbu.bank_system.domain.enums.LoanType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CustomerLoanApplicationStatusResponse(
        LoanType loanType,
        BigDecimal principalAmount,
        BigDecimal annualInterestRate,
        Integer repaymentTermMonths,
        LoanStatus status,
        LocalDate startDate,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt,
        BigDecimal monthlyInstallmentAmount,
        String message
) {
}
