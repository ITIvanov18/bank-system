package com.nbu.bank_system.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO за заявка за password reset линк.
 * Съдържа само email, защото самото генериране, hashing и изпращане на token-а са отговорност на AuthService.
 */

public record PasswordResetRequest(
        @NotBlank
        @Email
        String email
) {
}
