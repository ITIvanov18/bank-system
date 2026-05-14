package com.nbu.bank_system.dto.auth;

import com.nbu.bank_system.domain.enums.UserRole;
import com.nbu.bank_system.domain.enums.CustomerType;

/**
 * Response DTO при успешен login.
 * Съдържа JWT token и минималните user данни, нужни на frontend-а за роля, име и first-login поведение.
 */

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

