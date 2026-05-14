package com.nbu.bank_system.domain.enums;

/**
 * Описва lifecycle състоянието на банкова сметка.
 * Използва се както в persistence слоя, така и в DTO отговорите към клиента.
 */

public enum AccountStatus {
    ACTIVE,
    CLOSED
}
