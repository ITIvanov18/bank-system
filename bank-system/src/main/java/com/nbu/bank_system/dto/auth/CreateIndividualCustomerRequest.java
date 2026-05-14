package com.nbu.bank_system.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO за onboarding на индивидуален клиент.
 * Валидациите описват публичния API contract за имена, ЕГН и email.
 */

public record CreateIndividualCustomerRequest(
        @NotBlank
        @Size(max = 100)
        String firstName,

        @NotBlank
        @Size(max = 100)
        String lastName,

        @NotBlank
        @Pattern(regexp = "^[0-9]{10}$", message = "EGN must be exactly 10 digits")
        String egn,

        @Email
        @NotBlank
        String email
) {
}

