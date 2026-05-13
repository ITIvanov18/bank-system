package com.nbu.bank_system.service.loan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nbu.bank_system.domain.enums.LoanType;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;


class LoanProductPolicyTest {

    private final LoanProductPolicy loanProductPolicy = new LoanProductPolicy();

    @Test
    void calculateAnnualInterestRateGivesSmallDiscountForHigherAmount() {
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
        BigDecimal shorterTermRate = loanProductPolicy.calculateAnnualInterestRate(
                LoanType.CONSUMER,
                BigDecimal.valueOf(20_000),
                12
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
                12
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
    void termsForReturnsConfiguredConsumerReferenceValues() {
        LoanProductTerms terms = loanProductPolicy.termsFor(LoanType.CONSUMER);

        assertThat(terms.minimumPrincipalAmount()).isEqualByComparingTo("1000");
        assertThat(terms.maximumPrincipalAmount()).isEqualByComparingTo("40000");
        assertThat(terms.minimumRepaymentTermMonths()).isEqualTo(12);
        assertThat(terms.maximumRepaymentTermMonths()).isEqualTo(120);
    }

    @Test
    void termsForReturnsConfiguredMortgageReferenceValues() {
        LoanProductTerms terms = loanProductPolicy.termsFor(LoanType.MORTGAGE);

        assertThat(terms.minimumAnnualInterestRate()).isEqualByComparingTo("2.65");
        assertThat(terms.maximumAnnualInterestRate()).isEqualByComparingTo("3.45");
        assertThat(terms.minimumPrincipalAmount()).isEqualByComparingTo("3000");
        assertThat(terms.maximumPrincipalAmount()).isEqualByComparingTo("500000");
        assertThat(terms.principalStepAmount()).isEqualByComparingTo("500");
        assertThat(terms.minimumRepaymentTermMonths()).isNull();
        assertThat(terms.maximumRepaymentTermMonths()).isEqualTo(360);
    }

    @Test
    void calculateMortgageAnnualInterestRateStaysWithinConfiguredRange() {
        BigDecimal shortTermRate = loanProductPolicy.calculateAnnualInterestRate(
                LoanType.MORTGAGE,
                BigDecimal.valueOf(500_000),
                1
        );
        BigDecimal longTermRate = loanProductPolicy.calculateAnnualInterestRate(
                LoanType.MORTGAGE,
                BigDecimal.valueOf(3_000),
                360
        );

        assertThat(shortTermRate).isBetween(BigDecimal.valueOf(2.65), BigDecimal.valueOf(3.45));
        assertThat(longTermRate).isBetween(BigDecimal.valueOf(2.65), BigDecimal.valueOf(3.45));
    }

    @Test
    void calculateMortgageAnnualInterestRateAddsPremiumForVeryShortTerms() {
        BigDecimal veryShortSmallMortgageRate = loanProductPolicy.calculateAnnualInterestRate(
                LoanType.MORTGAGE,
                BigDecimal.valueOf(3_000),
                5
        );
        BigDecimal veryShortLargeMortgageRate = loanProductPolicy.calculateAnnualInterestRate(
                LoanType.MORTGAGE,
                BigDecimal.valueOf(300_000),
                1
        );
        BigDecimal standardMortgageRate = loanProductPolicy.calculateAnnualInterestRate(
                LoanType.MORTGAGE,
                BigDecimal.valueOf(300_000),
                180
        );

        assertThat(veryShortSmallMortgageRate).isGreaterThan(BigDecimal.valueOf(3.20));
        assertThat(veryShortLargeMortgageRate).isGreaterThan(BigDecimal.valueOf(3.15));
        assertThat(standardMortgageRate).isLessThan(veryShortLargeMortgageRate);
    }

    @Test
    void validateLoanRequestRejectsPrincipalBelowProductMinimum() {
        assertThatThrownBy(() -> loanProductPolicy.validateLoanRequest(
                LoanType.CONSUMER,
                BigDecimal.valueOf(999.99),
                12
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minimum allowed amount");
    }

    @Test
    void validateLoanRequestRejectsTermBelowProductMinimumWhenProductHasMinimumTerm() {
        assertThatThrownBy(() -> loanProductPolicy.validateLoanRequest(
                LoanType.CONSUMER,
                BigDecimal.valueOf(10_000),
                11
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
        assertThatThrownBy(() -> loanProductPolicy.validateLoanRequest(
                LoanType.MORTGAGE,
                BigDecimal.valueOf(100_000),
                361
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maximum allowed term");
    }
}
