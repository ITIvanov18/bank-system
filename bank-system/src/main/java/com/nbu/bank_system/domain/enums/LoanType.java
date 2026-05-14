package com.nbu.bank_system.domain.enums;

/**
 * Каталог на поддържаните кредитни продукти.
 * LoanProductPolicy използва тази стойност, за да приложи правилните лимити, срокове и лихвени правила.
 */

public enum LoanType {
    CONSUMER,
    MORTGAGE
}
