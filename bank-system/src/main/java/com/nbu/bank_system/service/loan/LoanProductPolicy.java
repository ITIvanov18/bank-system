package com.nbu.bank_system.service.loan;

import com.nbu.bank_system.domain.enums.LoanType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Централизира бизнес правилата за кредитните продукти
 * Тук държим reference/base условията за consumer и mortgage кредити,
 * както и адаптивната лихва според сумата и срока
 * Така контролерът и service слоят не съдържат hard-coded финансови правила
 */

@Component
public class LoanProductPolicy {

    private static final int RATE_SCALE = 4;
    private static final int SHORT_MORTGAGE_TERM_THRESHOLD_MONTHS = 36;
    private static final BigDecimal MORTGAGE_TERM_RISK_WEIGHT = BigDecimal.valueOf(0.65);
    private static final BigDecimal MORTGAGE_AMOUNT_RISK_WEIGHT = BigDecimal.valueOf(0.35);
    private static final BigDecimal CONSUMER_AMOUNT_WEIGHT = BigDecimal.valueOf(0.65);
    private static final BigDecimal CONSUMER_TERM_WEIGHT = BigDecimal.valueOf(0.35);

    private final Map<LoanType, LoanProductTerms> termsByLoanType;

    public LoanProductPolicy() {
        this.termsByLoanType = new EnumMap<>(LoanType.class);
        // Потребителски кредит: по-висока базова лихва и по-кратък максимален срок
        termsByLoanType.put(
                LoanType.CONSUMER,
                new LoanProductTerms(
                        LoanType.CONSUMER,
                        BigDecimal.valueOf(6.25),
                        BigDecimal.valueOf(5.20),
                        BigDecimal.valueOf(6.70),
                        BigDecimal.valueOf(1_000),
                        BigDecimal.valueOf(40_000),
                        BigDecimal.valueOf(5),
                        12,
                        120
                )
        );

        // Ипотечен кредит: променлива индикативна лихва между 2.25% и 3.45%.
        termsByLoanType.put(
                LoanType.MORTGAGE,
                new LoanProductTerms(
                        LoanType.MORTGAGE,
                        BigDecimal.valueOf(4.75),
                        BigDecimal.valueOf(3.10),
                        BigDecimal.valueOf(6.85),
                        BigDecimal.valueOf(3_000),
                        BigDecimal.valueOf(500_000),
                        BigDecimal.valueOf(500),
                        null,
                        360
                )
        );
    }

    public LoanProductTerms termsFor(LoanType loanType) {
        LoanProductTerms terms = termsByLoanType.get(loanType);
        if (terms == null) {
            throw new IllegalArgumentException("Unsupported loan type.");
        }
        return terms;
    }

    public void validateLoanRequest(LoanType loanType, BigDecimal principalAmount, int repaymentTermMonths) {
        LoanProductTerms terms = termsFor(loanType);

        if (principalAmount.compareTo(terms.minimumPrincipalAmount()) < 0) {
            throw new IllegalArgumentException(
                    "Principal amount is below the minimum allowed amount for " + loanType + " loans."
            );
        }

        if (principalAmount.compareTo(terms.maximumPrincipalAmount()) > 0) {
            throw new IllegalArgumentException(
                    "Principal amount exceeds the maximum allowed amount for " + loanType + " loans."
            );
        }

        if (!isValidPrincipalStep(terms, principalAmount)) {
            throw new IllegalArgumentException(
                    "Principal amount must use a valid increment for " + loanType + " loans."
            );
        }

        if (terms.minimumRepaymentTermMonths() != null
                && repaymentTermMonths < terms.minimumRepaymentTermMonths()) {
            throw new IllegalArgumentException(
                    "Repayment term is below the minimum allowed term for " + loanType + " loans."
            );
        }

        if (repaymentTermMonths > terms.maximumRepaymentTermMonths()) {
            throw new IllegalArgumentException(
                    "Repayment term exceeds the maximum allowed term for " + loanType + " loans."
            );
        }
    }

    public BigDecimal calculateAnnualInterestRate(
            LoanType loanType,
            BigDecimal principalAmount,
            int repaymentTermMonths
    ) {
        LoanProductTerms terms = termsFor(loanType);

        // Utilization показва каква част от допустимия диапазон за сума/срок използва клиентът.
        // Потребителският кредит е по-скъп при малка сума и кратък срок.
        // Ипотеката няма минимален срок, но нетипично кратки срокове се ценообразуват като по-рискови.
        BigDecimal amountUtilization = calculateAmountUtilization(terms, principalAmount);
        BigDecimal termUtilization = calculateTermUtilization(terms, repaymentTermMonths);

        if (loanType == LoanType.MORTGAGE) {
            BigDecimal shortTermRisk = calculateShortMortgageTermRisk(repaymentTermMonths);
            BigDecimal termRisk = termUtilization.max(shortTermRisk);
            BigDecimal amountRisk = BigDecimal.ONE.subtract(amountUtilization);
            BigDecimal mortgageRiskScore = termRisk.multiply(MORTGAGE_TERM_RISK_WEIGHT)
                    .add(amountRisk.multiply(MORTGAGE_AMOUNT_RISK_WEIGHT))
                    .min(BigDecimal.ONE)
                    .max(BigDecimal.ZERO);

            return terms.minimumAnnualInterestRate()
                    .add(terms.maximumAnnualInterestRate()
                            .subtract(terms.minimumAnnualInterestRate())
                            .multiply(mortgageRiskScore))
                    .setScale(RATE_SCALE, RoundingMode.HALF_UP);
        }

        BigDecimal consumerRiskScore = BigDecimal.ONE.subtract(amountUtilization).multiply(CONSUMER_AMOUNT_WEIGHT)
                .add(BigDecimal.ONE.subtract(termUtilization).multiply(CONSUMER_TERM_WEIGHT))
                .min(BigDecimal.ONE)
                .max(BigDecimal.ZERO);

        return terms.minimumAnnualInterestRate()
                .add(terms.maximumAnnualInterestRate()
                        .subtract(terms.minimumAnnualInterestRate())
                        .multiply(consumerRiskScore))
                .setScale(RATE_SCALE, RoundingMode.HALF_UP);
    }

    private boolean isValidPrincipalStep(LoanProductTerms terms, BigDecimal principalAmount) {
        BigDecimal remainder = principalAmount
                .subtract(terms.minimumPrincipalAmount())
                .remainder(terms.principalStepAmount());
        return remainder.compareTo(BigDecimal.ZERO) == 0;
    }

    private BigDecimal calculateAmountUtilization(LoanProductTerms terms, BigDecimal principalAmount) {
        BigDecimal principalRange = terms.maximumPrincipalAmount().subtract(terms.minimumPrincipalAmount());
        if (principalRange.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return principalAmount
                .subtract(terms.minimumPrincipalAmount())
                .max(BigDecimal.ZERO)
                .divide(principalRange, RATE_SCALE, RoundingMode.HALF_UP)
                .min(BigDecimal.ONE);
    }

    private BigDecimal calculateTermUtilization(LoanProductTerms terms, int repaymentTermMonths) {
        int minimumTerm = terms.minimumRepaymentTermMonths() != null ? terms.minimumRepaymentTermMonths() : 1;
        int termRange = terms.maximumRepaymentTermMonths() - minimumTerm;
        if (termRange == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(repaymentTermMonths - minimumTerm)
                .max(BigDecimal.ZERO)
                .divide(BigDecimal.valueOf(termRange), RATE_SCALE, RoundingMode.HALF_UP)
                .min(BigDecimal.ONE);
    }

    private BigDecimal calculateShortMortgageTermRisk(int repaymentTermMonths) {
        if (repaymentTermMonths >= SHORT_MORTGAGE_TERM_THRESHOLD_MONTHS) {
            return BigDecimal.ZERO;
        }

        int termRange = SHORT_MORTGAGE_TERM_THRESHOLD_MONTHS - 1;
        return BigDecimal.valueOf(SHORT_MORTGAGE_TERM_THRESHOLD_MONTHS - repaymentTermMonths)
                .divide(BigDecimal.valueOf(termRange), RATE_SCALE, RoundingMode.HALF_UP)
                .min(BigDecimal.ONE)
                .max(BigDecimal.ZERO);
    }
}
