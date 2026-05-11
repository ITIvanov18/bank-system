package com.nbu.bank_system.dto.auth;

import com.nbu.bank_system.domain.enums.UserRole;
import com.nbu.bank_system.domain.enums.CustomerType;

public record AuthResponse(
        String token,
        Long customerId,
        String email,
        String displayName,
        UserRole role,
        CustomerType customerType,
        boolean firstLogin
) {
}

