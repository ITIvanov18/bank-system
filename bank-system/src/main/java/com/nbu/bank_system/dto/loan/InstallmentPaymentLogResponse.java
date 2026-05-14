package com.nbu.bank_system.dto.loan;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InstallmentPaymentLogResponse(
        Long logId,
        Long loanId,
        Long installmentId,
        Integer installmentNumber,
        BigDecimal amountPaid,
        LocalDateTime paidAt
) {
}

