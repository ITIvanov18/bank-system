package com.nbu.bank_system.dto.loan;

import jakarta.validation.constraints.Size;

public record LoanReviewRequest(
        @Size(max = 500, message = "Decision note cannot exceed 500 characters.")
        String decisionNote
) {
}
