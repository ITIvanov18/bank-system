package com.nbu.bank_system.dto.account;

import com.nbu.bank_system.domain.enums.AccountStatus;
import java.math.BigDecimal;

/**
 * Response DTO за опит за откриване на банкова сметка.
 * Връща дали е създадена нова сметка или е намерена съществуваща, заедно с публичните account данни.
 */

public record AccountOpeningResponse(
        boolean created,
        Long accountId,
        String iban,
        BigDecimal balance,
        AccountStatus status,
        String message
) {
}

