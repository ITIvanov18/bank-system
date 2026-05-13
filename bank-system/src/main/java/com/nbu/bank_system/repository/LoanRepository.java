package com.nbu.bank_system.repository;

import com.nbu.bank_system.domain.enums.LoanStatus;
import com.nbu.bank_system.domain.model.loan.Loan;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Loan> findByStatusOrderByCreatedAtAsc(LoanStatus status);
}
