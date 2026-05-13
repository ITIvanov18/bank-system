package com.nbu.bank_system.dto.account;

import com.nbu.bank_system.domain.enums.AccountStatus;
import java.math.BigDecimal;

public record AccountStatusResponse(
        boolean hasAccount,
        Long accountId,
        String iban,
        BigDecimal balance,
        BigDecimal outstandingDebtAmount,
        AccountStatus status
) {
}

