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
 * както и адаптивното оскъпяване според сумата и срока
 * Така контролерът и service слоят не съдържат hard-coded финансови правила
 */

@Component
public class LoanProductPolicy {

    private static final int RATE_SCALE = 4;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    // Premium values са в процентни пунктове
    // Пример: 0.75 означава +0.75% към годишната лихва
    private static final BigDecimal MAX_AMOUNT_PREMIUM_PERCENTAGE_POINTS = BigDecimal.valueOf(0.75);
    private static final BigDecimal MAX_TERM_PREMIUM_PERCENTAGE_POINTS = BigDecimal.valueOf(0.50);

    private final Map<LoanType, LoanProductTerms> termsByLoanType;

    public LoanProductPolicy() {
        this.termsByLoanType = new EnumMap<>(LoanType.class);
        // Потребителски кредит: по-висока базова лихва и по-кратък максимален срок
        termsByLoanType.put(
                LoanType.CONSUMER,
                new LoanProductTerms(
                        LoanType.CONSUMER,
                        BigDecimal.valueOf(6.25),
                        BigDecimal.valueOf(50_000),
                        84
                )
        );

        // Ипотечен кредит: по-нисък reference rate, но по-висок лимит и по-дълъг срок
        termsByLoanType.put(
                LoanType.MORTGAGE,
                new LoanProductTerms(
                        LoanType.MORTGAGE,
                        BigDecimal.valueOf(3.00),
                        BigDecimal.valueOf(500_000),
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

        if (principalAmount.compareTo(terms.maximumPrincipalAmount()) > 0) {
            throw new IllegalArgumentException(
                    "Principal amount exceeds the maximum allowed amount for " + loanType + " loans."
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

        // Utilization показва каква част от максимално разрешената сума/срок използва клиентът
        // Колкото по-висок utilization, толкова по-висок adaptive annual interest rate
        BigDecimal amountUtilization = principalAmount
                .multiply(ONE_HUNDRED)
                .divide(terms.maximumPrincipalAmount(), RATE_SCALE, RoundingMode.HALF_UP)
                .divide(ONE_HUNDRED, RATE_SCALE, RoundingMode.HALF_UP);
        BigDecimal termUtilization = BigDecimal.valueOf(repaymentTermMonths)
                .multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(terms.maximumRepaymentTermMonths()), RATE_SCALE, RoundingMode.HALF_UP)
                .divide(ONE_HUNDRED, RATE_SCALE, RoundingMode.HALF_UP);

        BigDecimal amountPremium = amountUtilization.multiply(MAX_AMOUNT_PREMIUM_PERCENTAGE_POINTS);
        BigDecimal termPremium = termUtilization.multiply(MAX_TERM_PREMIUM_PERCENTAGE_POINTS);

        return terms.baseAnnualInterestRate()
                .add(amountPremium)
                .add(termPremium)
                .setScale(RATE_SCALE, RoundingMode.HALF_UP);
    }
}
