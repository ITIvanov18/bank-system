package com.nbu.bank_system.dto.loan;

import com.nbu.bank_system.domain.enums.LoanStatus;
import com.nbu.bank_system.domain.enums.LoanType;
import com.nbu.bank_system.domain.enums.CustomerType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Обобщен response DTO за loan operations.
 * Комбинира loan параметрите, customer display информация и repayment schedule, използвано от employee и customer endpoints.
 */

public record LoanGrantResponse(
        Long loanId,
        Long customerId,
        String customerEmail,
        String customerDisplayName,
        CustomerType customerType,
        LoanType loanType,
        BigDecimal principalAmount,
        BigDecimal annualInterestRate,
        Integer repaymentTermMonths,
        LoanStatus status,
        LocalDate startDate,
        LocalDateTime reviewedAt,
        BigDecimal monthlyInstallmentAmount,
        List<InstallmentResponse> repaymentSchedule,
        String message
) {
}
