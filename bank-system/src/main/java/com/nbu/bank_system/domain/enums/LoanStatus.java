package com.nbu.bank_system.domain.enums;

/**
 * Описва lifecycle статуса на кредита или кредитното заявление.
 * Разделя pending review, active repayment, rejected application и fully closed loan сценарии.
 */

public enum LoanStatus {
    PENDING,
    ACTIVE,
    REJECTED,
    CLOSED
}
