package com.nbu.bank_system.domain.enums;

/**
 * Фиксира възможните employee решения върху кредитно заявление.
 * Използва се в audit логовете, за да остане историята еднозначна и лесна за филтриране.
 */

public enum LoanReviewDecision {
    APPROVED,
    REJECTED
}
