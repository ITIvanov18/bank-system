package com.nbu.bank_system.domain.enums;

/**
 * Показва текущото състояние на отделна погасителна вноска.
 * Служи за намиране на следващата pending вноска и за визуализиране на repayment schedule-а.
 */

public enum InstallmentStatus {
    PENDING,
    PAID,
    OVERDUE
}
