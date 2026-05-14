package com.nbu.bank_system.dto.account;

import com.nbu.bank_system.domain.enums.AccountStatus;
import java.math.BigDecimal;

/**
 * Response DTO за customer account dashboard-а.
 * Комбинира наличност на сметка, текущ баланс, непогасена главница по активни кредити и account status.
 */

public record AccountStatusResponse(
        boolean hasAccount,
        Long accountId,
        String iban,
        BigDecimal balance,
        BigDecimal outstandingDebtAmount,
        AccountStatus status
) {
}

