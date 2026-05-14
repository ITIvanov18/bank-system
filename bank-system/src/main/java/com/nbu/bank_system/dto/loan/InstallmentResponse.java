package com.nbu.bank_system.dto.loan;

import com.nbu.bank_system.domain.enums.InstallmentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO за една погасителна вноска.
 * Дава на frontend-а падеж, разбивка principal/interest, оставащ баланс, статус и payment timestamp.
 */

public record InstallmentResponse(
        Long installmentId,
        Integer installmentNumber,
        LocalDate dueDate,
        BigDecimal monthlyInstallmentAmount,
        BigDecimal principalPart,
        BigDecimal interestPart,
        BigDecimal remainingBalance,
        InstallmentStatus status,
        LocalDateTime paidAt
) {
}
