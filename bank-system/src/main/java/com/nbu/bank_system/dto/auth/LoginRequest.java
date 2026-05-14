package com.nbu.bank_system.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO за login endpoint-а.
 * Държи само credentials полетата и оставя автентикацията, normalizing-а и token generation-а на AuthService.
 */

public record LoginRequest(
        @Email(message = "Please provide a valid email")
        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {
}

