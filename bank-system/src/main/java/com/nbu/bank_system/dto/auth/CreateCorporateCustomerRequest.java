package com.nbu.bank_system.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO за onboarding на корпоративен клиент.
 * Валидациите описват публичния API contract за company identity, representative данни и email.
 */

public record CreateCorporateCustomerRequest(
        @NotBlank
        @Size(max = 200)
        String companyName,

        @NotBlank
        @Pattern(regexp = "^[0-9]{9,13}$", message = "EIK must be 9 to 13 digits")
        String eik,

        @NotBlank
        @Size(max = 100)
        String representativeFirstName,

        @NotBlank
        @Size(max = 100)
        String representativeLastName,

        @Email
        @NotBlank
        String email
) {
}

