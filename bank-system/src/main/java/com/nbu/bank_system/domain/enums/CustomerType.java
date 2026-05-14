package com.nbu.bank_system.domain.enums;

/**
 * Разграничава подтиповете клиенти в домейн модела.
 * Стойността се пази отделно от JPA discriminator-а, за да е ясна и в API responses.
 */

public enum CustomerType {
    INDIVIDUAL,
    CORPORATE
}
