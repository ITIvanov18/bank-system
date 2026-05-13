package com.nbu.bank_system.repository;

import com.nbu.bank_system.domain.model.loan.LoanReviewLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanReviewLogRepository extends JpaRepository<LoanReviewLog, Long> {

    List<LoanReviewLog> findAllByOrderByCreatedAtDesc();
}
