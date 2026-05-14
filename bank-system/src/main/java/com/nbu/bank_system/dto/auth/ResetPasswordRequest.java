package com.nbu.bank_system.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO за потвърждаване на password reset.
 * Предава token-а от email линка и новата парола, като service слоят валидира hash-а, срока и употребата.
 */

public record ResetPasswordRequest(
        @NotBlank
        String token,

        @NotBlank
        @Size(min = 8, max = 72)
        String newPassword
) {
}
