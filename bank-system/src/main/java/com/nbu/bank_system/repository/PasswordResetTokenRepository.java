package com.nbu.bank_system.repository;

import com.nbu.bank_system.domain.model.PasswordResetToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

/**
 * Spring Data repository за password reset token persistence.
 * Поддържа lookup по token hash и изчистване на неизползвани tokens преди издаване на нов reset линк.
 */

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHashAndUsedAtIsNull(String tokenHash);

    @Modifying
    void deleteByCustomer_IdAndUsedAtIsNull(Long customerId);
}
