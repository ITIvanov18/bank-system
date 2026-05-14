package com.nbu.bank_system.domain.enums;

/**
 * Ролите, с които Spring Security защитава backend endpoints.
 * CUSTOMER и EMPLOYEE позволяват различни dashboard-и и различни API права върху един общ Customer модел.
 */

public enum UserRole {
    CUSTOMER,
    EMPLOYEE
}

