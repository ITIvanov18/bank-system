package com.nbu.bank_system.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO за смяна на парола от authenticated user.
 * Bean validation правилата пазят минималната password policy още на controller boundary-то.
 */

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
                message = "New password must contain uppercase, lowercase and number, min 8 chars"
        )
        String newPassword
) {
}

