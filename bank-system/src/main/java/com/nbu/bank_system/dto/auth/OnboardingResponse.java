package com.nbu.bank_system.dto.auth;

import com.nbu.bank_system.domain.enums.CustomerType;

/**
 * Response DTO след успешно създаване на клиентски online banking профил.
 * Освен identity данните връща дали временната парола е изпратена и през кой mail relay е минала.
 */

public record OnboardingResponse(
        Long customerId,
        String email,
        CustomerType customerType,
        boolean temporaryPasswordSent,
        String emailDeliveryChannel,
        String emailRelay
) {
}

