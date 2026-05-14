package com.nbu.bank_system.dto.auth;

/**
 * Малък response DTO за password-related операции.
 * Унифицира успешните съобщения при change password и reset password flows.
 */

public record ChangePasswordResponse(String message) {
}

