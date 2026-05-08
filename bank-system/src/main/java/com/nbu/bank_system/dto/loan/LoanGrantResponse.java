package com.nbu.bank_system.dto.loan;

import com.nbu.bank_system.domain.enums.LoanStatus;
import com.nbu.bank_system.domain.enums.LoanType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record LoanGrantResponse(
        Long loanId,
        Long customerId,
        LoanType loanType,
        BigDecimal principalAmount,
        BigDecimal annualInterestRate,
        Integer repaymentTermMonths,
        LoanStatus status,
        LocalDate startDate,
        BigDecimal monthlyInstallmentAmount,
        List<InstallmentResponse> repaymentSchedule,
        String message
) {
}
