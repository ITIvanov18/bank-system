package com.nbu.bank_system.repository;

import com.nbu.bank_system.domain.enums.UserRole;
import com.nbu.bank_system.domain.model.customer.Customer;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByEmailIgnoreCase(String email);

    Optional<Customer> findFirstByUserRole(UserRole userRole);

    @Modifying
    @Query(
            value = """
                    UPDATE customers
                    SET password_hash = :passwordHash,
                        is_first_login = b'0',
                        user_role = 'EMPLOYEE',
                        updated_at = CURRENT_TIMESTAMP(6)
                    WHERE LOWER(email) = LOWER(:email)
                    """,
            nativeQuery = true
    )
    int updateBootstrapEmployeeCredentials(@Param("email") String email, @Param("passwordHash") String passwordHash);

    @Modifying
    @Query(
            value = """
                    UPDATE customers
                    SET email = :email,
                        password_hash = :passwordHash,
                        is_first_login = b'0',
                        user_role = 'EMPLOYEE',
                        updated_at = CURRENT_TIMESTAMP(6)
                    WHERE user_role = 'EMPLOYEE'
                    ORDER BY id
                    LIMIT 1
                    """,
            nativeQuery = true
    )
    int updateAnyEmployeeCredentials(@Param("email") String email, @Param("passwordHash") String passwordHash);

    boolean existsByEmailIgnoreCase(String email);
}

