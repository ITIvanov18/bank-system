package com.nbu.bank_system.repository;

import com.nbu.bank_system.domain.model.loan.InstallmentPaymentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data repository за repayment payment logs.
 * Custom query-то връща payment history за конкретен customer в обратен хронологичен ред.
 */

public interface InstallmentPaymentLogRepository extends JpaRepository<InstallmentPaymentLog, Long> {

    @Query("SELECT l FROM InstallmentPaymentLog l JOIN l.loan ln WHERE ln.customer.id = :customerId ORDER BY l.paidAt DESC")
    List<InstallmentPaymentLog> findAllByCustomerIdOrderByPaidAtDesc(@Param("customerId") Long customerId);
}

