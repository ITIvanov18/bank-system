package com.nbu.bank_system.repository;

import com.nbu.bank_system.domain.model.loan.Installment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository за Installment entity-то.
 * Използва се за подредено извличане на погасителен план по loan id.
 */

public interface InstallmentRepository extends JpaRepository<Installment, Long> {

    List<Installment> findByLoanIdOrderByInstallmentNumberAsc(Long loanId);
}
