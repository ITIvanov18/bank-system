package com.nbu.bank_system.dto.loan;

import com.nbu.bank_system.domain.enums.LoanType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record SubmitLoanApplicationRequest(
        @NotNull(message = "Loan type is required.")
        LoanType loanType,

        @NotNull(message = "Principal amount is required.")
        @Digits(integer = 19, fraction = 2, message = "Principal amount must have up to 2 decimal places.")
        @DecimalMin(value = "0.01", inclusive = true, message = "Principal amount must be at least 0.01.")
        BigDecimal principalAmount,

        @NotNull(message = "Repayment term is required.")
        @Min(value = 1, message = "Repayment term must be at least 1 month.")
        @Max(value = 360, message = "Repayment term cannot exceed 360 months.")
        Integer repaymentTermMonths
) {
}
