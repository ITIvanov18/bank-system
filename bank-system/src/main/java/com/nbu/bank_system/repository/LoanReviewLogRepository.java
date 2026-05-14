package com.nbu.bank_system.repository;

import com.nbu.bank_system.domain.model.loan.LoanReviewLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository за employee loan review audit logs.
 * Връща историята на решенията в обратен хронологичен ред за employee overview.
 */

public interface LoanReviewLogRepository extends JpaRepository<LoanReviewLog, Long> {

    List<LoanReviewLog> findAllByOrderByCreatedAtDesc();
}
