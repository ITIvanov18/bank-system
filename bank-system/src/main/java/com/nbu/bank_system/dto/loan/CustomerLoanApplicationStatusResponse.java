package com.nbu.bank_system.dto.loan;

import com.nbu.bank_system.domain.enums.LoanStatus;
import com.nbu.bank_system.domain.enums.LoanType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Customer-facing DTO за последното кредитно заявление.
 * Събира параметрите на продукта, review timestamps и message, за да може dashboard-ът да показва текущ статус без JPA entity детайли.
 */

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
