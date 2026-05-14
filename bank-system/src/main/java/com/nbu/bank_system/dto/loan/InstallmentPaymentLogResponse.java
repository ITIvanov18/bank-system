package com.nbu.bank_system.dto.loan;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO за история на плащанията по вноски.
 * Показва loan/installment контекст, платена сума и момент на плащането, без да излага entity графа.
 */

public record InstallmentPaymentLogResponse(
        Long logId,
        Long loanId,
        Long installmentId,
        Integer installmentNumber,
        BigDecimal amountPaid,
        LocalDateTime paidAt
) {
}

