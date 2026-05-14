package com.nbu.bank_system.repository;

import com.nbu.bank_system.domain.enums.AccountStatus;
import com.nbu.bank_system.domain.model.account.BankAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository за BankAccount entity-то.
 * Съдържа query methods за намиране на customer сметка, active account checks и uniqueness проверка на IBAN.
 */

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    Optional<BankAccount> findFirstByOwnerIdOrderByIdAsc(Long ownerId);

    Optional<BankAccount> findFirstByOwnerIdAndStatusOrderByIdAsc(Long ownerId, AccountStatus status);

    boolean existsByOwnerIdAndStatus(Long ownerId, AccountStatus status);

    boolean existsByIban(String iban);
}

