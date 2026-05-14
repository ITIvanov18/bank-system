package com.nbu.bank_system.dto.loan;

import jakarta.validation.constraints.Size;

/**
 * Request DTO за optional бележка при approve/reject на кредитно заявление.
 * Ограничава дължината на decision note-а, за да остане audit логът четим и предвидим.
 */

public record LoanReviewRequest(
        @Size(max = 500, message = "Decision note cannot exceed 500 characters.")
        String decisionNote
) {
}
