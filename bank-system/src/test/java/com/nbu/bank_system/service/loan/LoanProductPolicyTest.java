package com.nbu.bank_system.service.loan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nbu.bank_system.domain.enums.LoanType;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Unit tests за правилата на кредитните продукти
 * Те проверяват дали правилата за лихвени проценти, max суми и срокове са имплементирани
 * според договорените бизнес изисквания. Ако тези правила се променят, тестовете ще прихванат
 * несъответствията и ще ни помогнат да ги коригираме без да нарушим очакваното поведение на кредитните продукти
 */

class LoanProductPolicyTest {

    private final LoanProductPolicy loanProductPolicy = new LoanProductPolicy();

    @Test
    void calculateAnnualInterestRateReturnsHigherRateForHigherAmountAndLongerTerm() {
        // По-голяма сума и по-дълъг срок трябва да доведат до по-висока adaptive rate
        BigDecimal lowerRiskRate = loanProductPolicy.calculateAnnualInterestRate(
                LoanType.CONSUMER,
                BigDecimal.valueOf(5_000),
                12
        );
        BigDecimal higherRiskRate = loanProductPolicy.calculateAnnualInterestRate(
                LoanType.CONSUMER,
                BigDecimal.valueOf(45_000),
                72
        );

        assertThat(higherRiskRate).isGreaterThan(lowerRiskRate);
        assertThat(lowerRiskRate).isGreaterThan(BigDecimal.valueOf(6.25));
    }

    @Test
    void termsForReturnsConfiguredMortgageReferenceValues() {
        // Проверяваме reference условията, които са договорени като business requirement
        LoanProductTerms terms = loanProductPolicy.termsFor(LoanType.MORTGAGE);

        assertThat(terms.baseAnnualInterestRate()).isEqualByComparingTo("3.00");
        assertThat(terms.maximumPrincipalAmount()).isEqualByComparingTo("500000");
        assertThat(terms.maximumRepaymentTermMonths()).isEqualTo(360);
    }

    @Test
    void validateLoanRequestRejectsPrincipalAboveProductMaximum() {
        // Consumer loan не трябва да позволява сума над зададения maximum principal
        assertThatThrownBy(() -> loanProductPolicy.validateLoanRequest(
                LoanType.CONSUMER,
                BigDecimal.valueOf(50_000.01),
                84
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maximum allowed amount");
    }

    @Test
    void validateLoanRequestRejectsTermAboveProductMaximum() {
        // Mortgage loan не трябва да позволява срок над product maximum term
        assertThatThrownBy(() -> loanProductPolicy.validateLoanRequest(
                LoanType.MORTGAGE,
                BigDecimal.valueOf(100_000),
                361
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maximum allowed term");
    }
}
