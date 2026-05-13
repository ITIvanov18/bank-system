package com.nbu.bank_system.dto.loan;

import com.nbu.bank_system.domain.enums.LoanReviewDecision;
import com.nbu.bank_system.domain.enums.LoanType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LoanReviewLogResponse(
        Long logId,
        Long loanId,
        Long customerId,
        String customerEmail,
        String employeeEmail,
        LoanReviewDecision decision,
        LoanType loanType,
        BigDecimal principalAmount,
        BigDecimal annualInterestRate,
        Integer repaymentTermMonths,
        String decisionNote,
        LocalDateTime decidedAt
) {
}
