package com.nbu.bank_system.repository;

import com.nbu.bank_system.domain.enums.LoanStatus;
import com.nbu.bank_system.domain.model.loan.Loan;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Loan> findByStatusOrderByCreatedAtAsc(LoanStatus status);

    Optional<Loan> findFirstByCustomerIdOrderByCreatedAtDesc(Long customerId);

    boolean existsByCustomerIdAndStatus(Long customerId, LoanStatus status);

    @Query("""
            select coalesce(sum(installment.principalPart), 0)
            from Installment installment
            join installment.loan loan
            where loan.customer.id = :customerId
              and loan.status = com.nbu.bank_system.domain.enums.LoanStatus.ACTIVE
              and installment.status = com.nbu.bank_system.domain.enums.InstallmentStatus.PENDING
            """)
    BigDecimal calculateOutstandingPrincipalDebt(@Param("customerId") Long customerId);
}
