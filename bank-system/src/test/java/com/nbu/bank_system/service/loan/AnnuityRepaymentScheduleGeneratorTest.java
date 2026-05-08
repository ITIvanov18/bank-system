package com.nbu.bank_system.service.loan;

import static org.assertj.core.api.Assertions.assertThat;

import com.nbu.bank_system.domain.enums.InstallmentStatus;
import com.nbu.bank_system.domain.model.loan.Installment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests за генератора на погасителни планове
 * Тези тестове не използват Spring context или database, защото проверяват чиста
 * financial calculation логика с цел прихващане на грешки във формулата.
 */

class AnnuityRepaymentScheduleGeneratorTest {

    private final AnnuityRepaymentScheduleGenerator generator = new AnnuityRepaymentScheduleGenerator();

    @Test
    void generateCreatesAnnuityScheduleWithDecreasingInterestAndIncreasingPrincipal() {
        // Генерира едногодишен план с фиксирана главница и лихва
        List<Installment> installments = generator.generate(
                BigDecimal.valueOf(10_000),
                BigDecimal.valueOf(7.25),
                12,
                LocalDate.of(2026, 1, 15)
        );

        assertThat(installments).hasSize(12);
        assertThat(installments.getFirst().getDueDate()).isEqualTo(LocalDate.of(2026, 2, 15));
        assertThat(installments.getFirst().getStatus()).isEqualTo(InstallmentStatus.PENDING);

        BigDecimal regularPayment = installments.getFirst().getMonthlyInstallmentAmount();

        // При annuity repayment всички вноски са еднакви, с изключение на възможна rounding корекция в края
        assertThat(installments.subList(0, installments.size() - 1))
                .allSatisfy(installment -> assertThat(installment.getMonthlyInstallmentAmount())
                        .isEqualByComparingTo(regularPayment));

        // В началото лихвата е по-висока, а principal частта расте с намаляване на остатъка
        assertThat(installments.get(1).getInterestPart())
                .isLessThan(installments.getFirst().getInterestPart());
        assertThat(installments.get(1).getPrincipalPart())
                .isGreaterThan(installments.getFirst().getPrincipalPart());
        assertThat(installments.getLast().getRemainingBalance()).isEqualByComparingTo("0.00");

        BigDecimal totalPrincipal = installments.stream()
                .map(Installment::getPrincipalPart)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Сумата на principal частите трябва да върне точно отпуснатата главница
        assertThat(totalPrincipal).isEqualByComparingTo("10000.00");
    }
}
