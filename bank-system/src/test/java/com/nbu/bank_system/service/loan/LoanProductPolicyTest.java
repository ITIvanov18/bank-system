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
    void calculateAnnualInterestRateGivesSmallDiscountForHigherAmount() {
        // По-голямата сума получава по-добра лихва, ако срокът е еднакъв.
        BigDecimal smallerAmountRate = loanProductPolicy.calculateAnnualInterestRate(
                LoanType.CONSUMER,
                BigDecimal.valueOf(5_000),
                36
        );
        BigDecimal largerAmountRate = loanProductPolicy.calculateAnnualInterestRate(
                LoanType.CONSUMER,
                BigDecimal.valueOf(35_000),
                36
        );

        assertThat(largerAmountRate).isLessThan(smallerAmountRate);
    }

    @Test
    void calculateAnnualInterestRateAddsPremiumForLongerTerm() {
        // При потребителския продукт кратките и малки кредити са по-скъпи като индикативна лихва.
        BigDecimal shorterTermRate = loanProductPolicy.calculateAnnualInterestRate(
                LoanType.CONSUMER,
                BigDecimal.valueOf(20_000),
                18
        );
        BigDecimal longerTermRate = loanProductPolicy.calculateAnnualInterestRate(
                LoanType.CONSUMER,
                BigDecimal.valueOf(20_000),
                72
        );

        assertThat(longerTermRate).isLessThan(shorterTermRate);
    }

    @Test
    void calculateConsumerAnnualInterestRateStaysWithinConfiguredRange() {
        BigDecimal highestRiskRate = loanProductPolicy.calculateAnnualInterestRate(
                LoanType.CONSUMER,
                BigDecimal.valueOf(1_000),
                18
        );
        BigDecimal lowestRiskRate = loanProductPolicy.calculateAnnualInterestRate(
                LoanType.CONSUMER,
                BigDecimal.valueOf(40_000),
                120
        );

        assertThat(highestRiskRate).isEqualByComparingTo("6.7000");
        assertThat(lowestRiskRate).isEqualByComparingTo("5.2000");
    }

    @Test
    void termsForReturnsConfiguredMortgageReferenceValues() {
        // Проверяваме reference условията, които са договорени като business requirement
        LoanProductTerms terms = loanProductPolicy.termsFor(LoanType.MORTGAGE);

        assertThat(terms.minimumAnnualInterestRate()).isEqualByComparingTo("2.25");
        assertThat(terms.maximumAnnualInterestRate()).isEqualByComparingTo("3.45");
        assertThat(terms.minimumPrincipalAmount()).isEqualByComparingTo("3000");
        assertThat(terms.maximumPrincipalAmount()).isEqualByComparingTo("500000");
        assertThat(terms.principalStepAmount()).isEqualByComparingTo("500");
        assertThat(terms.minimumRepaymentTermMonths()).isNull();
        assertThat(terms.maximumRepaymentTermMonths()).isEqualTo(360);
    }

    @Test
    void calculateMortgageAnnualInterestRateStaysWithinConfiguredRange() {
        BigDecimal lowestRiskRate = loanProductPolicy.calculateAnnualInterestRate(
                LoanType.MORTGAGE,
                BigDecimal.valueOf(500_000),
                1
        );
        BigDecimal highestRiskRate = loanProductPolicy.calculateAnnualInterestRate(
                LoanType.MORTGAGE,
                BigDecimal.valueOf(3_000),
                360
        );

        assertThat(lowestRiskRate).isEqualByComparingTo("2.2500");
        assertThat(highestRiskRate).isEqualByComparingTo("3.4500");
    }

    @Test
    void validateLoanRequestRejectsPrincipalBelowProductMinimum() {
        assertThatThrownBy(() -> loanProductPolicy.validateLoanRequest(
                LoanType.CONSUMER,
                BigDecimal.valueOf(999.99),
                18
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minimum allowed amount");
    }

    @Test
    void validateLoanRequestRejectsTermBelowProductMinimumWhenProductHasMinimumTerm() {
        assertThatThrownBy(() -> loanProductPolicy.validateLoanRequest(
                LoanType.CONSUMER,
                BigDecimal.valueOf(10_000),
                17
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minimum allowed term");
    }

    @Test
    void validateLoanRequestRejectsPrincipalThatDoesNotMatchProductStep() {
        assertThatThrownBy(() -> loanProductPolicy.validateLoanRequest(
                LoanType.CONSUMER,
                BigDecimal.valueOf(3999.91),
                24
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valid increment");
    }

    @Test
    void validateLoanRequestRejectsPrincipalAboveProductMaximum() {
        // Consumer loan не трябва да позволява сума над зададения maximum principal
        assertThatThrownBy(() -> loanProductPolicy.validateLoanRequest(
                LoanType.CONSUMER,
                BigDecimal.valueOf(40_000.01),
                120
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
