package com.nbu.bank_system.service.loan;

import com.nbu.bank_system.domain.model.loan.Installment;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Генерира анюитетен погасителен план за вече одобрен кредит.
 * Annuity означава, че месечната вноска е еднаква през целия срок,
 * но съотношението между лихва и главница се променя:
 * в началото лихвената част е по-голяма, а към края по-голяма става главницата
 */

@Component
public class AnnuityRepaymentScheduleGenerator {

    private static final MathContext MONEY_CALCULATION_CONTEXT = MathContext.DECIMAL128;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal MONTHS_IN_YEAR = BigDecimal.valueOf(12);
    private static final int MONEY_SCALE = 2;

    public List<Installment> generate(
            BigDecimal principalAmount,
            BigDecimal annualInterestRate,
            int repaymentTermMonths,
            LocalDate startDate
    ) {
        // Годишната лихва е процент, затова първо я превръщаме в месечен коефициент
        // Пример: 7.20% annual rate -> 0.006 monthly rate
        BigDecimal monthlyInterestRate = annualInterestRate
                .divide(ONE_HUNDRED, MONEY_CALCULATION_CONTEXT)
                .divide(MONTHS_IN_YEAR, MONEY_CALCULATION_CONTEXT);
        BigDecimal regularMonthlyPayment = calculateMonthlyPayment(
                principalAmount,
                monthlyInterestRate,
                repaymentTermMonths
        );

        List<Installment> installments = new ArrayList<>(repaymentTermMonths);
        BigDecimal remainingBalance = principalAmount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        for (int installmentNumber = 1; installmentNumber <= repaymentTermMonths; installmentNumber++) {
            // Лихвата винаги се начислява върху текущия остатък, а не върху първоначалната главница
            BigDecimal interestPart = remainingBalance
                    .multiply(monthlyInterestRate, MONEY_CALCULATION_CONTEXT)
                    .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            boolean isFinalInstallment = installmentNumber == repaymentTermMonths;

            // Последната вноска взима целия оставащ principal, за да изчистим rounding разликите
            BigDecimal principalPart = isFinalInstallment
                    ? remainingBalance
                    : regularMonthlyPayment.subtract(interestPart).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            BigDecimal monthlyInstallmentAmount = isFinalInstallment
                    ? principalPart.add(interestPart).setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                    : regularMonthlyPayment;

            remainingBalance = remainingBalance.subtract(principalPart).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            if (isFinalInstallment) {
                remainingBalance = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            }

            installments.add(new Installment(
                    installmentNumber,
                    startDate.plusMonths(installmentNumber),
                    monthlyInstallmentAmount,
                    principalPart,
                    interestPart,
                    remainingBalance
            ));
        }

        return installments;
    }

    private BigDecimal calculateMonthlyPayment(
            BigDecimal principalAmount,
            BigDecimal monthlyInterestRate,
            int repaymentTermMonths
    ) {
        // Стандартна annuity формула: P * r * (1+r)^n / ((1+r)^n - 1)
        // Междинните сметки са с DECIMAL, а закръглянето до 2 знака чак накрая,
        // за да се избегнат натрупващи се rounding грешки.
        BigDecimal compoundFactor = BigDecimal.ONE
                .add(monthlyInterestRate, MONEY_CALCULATION_CONTEXT)
                .pow(repaymentTermMonths, MONEY_CALCULATION_CONTEXT);
        BigDecimal numerator = principalAmount
                .multiply(monthlyInterestRate, MONEY_CALCULATION_CONTEXT)
                .multiply(compoundFactor, MONEY_CALCULATION_CONTEXT);
        BigDecimal denominator = compoundFactor.subtract(BigDecimal.ONE, MONEY_CALCULATION_CONTEXT);

        return numerator
                .divide(denominator, MONEY_CALCULATION_CONTEXT)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
