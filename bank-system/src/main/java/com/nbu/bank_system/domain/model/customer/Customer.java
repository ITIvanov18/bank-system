package com.nbu.bank_system.domain.model.customer;

import com.nbu.bank_system.domain.model.BaseEntity;
import com.nbu.bank_system.domain.model.account.BankAccount;
import com.nbu.bank_system.domain.model.loan.Loan;
import com.nbu.bank_system.domain.enums.CustomerType;
import com.nbu.bank_system.domain.enums.UserRole;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;

/**
 * Абстрактно root customer entity, което дефинира общите полета и поведение за всички типове клиенти.
 * Играе ролята на owner-side reference към свързаните bank accounts и loans и държи credentials, first-login флага и security ролята.
 */

@Entity
@Table(name = "customers")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "customer_discriminator", discriminatorType = DiscriminatorType.STRING)
public abstract class Customer extends BaseEntity {

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", nullable = false, length = 20)
    private CustomerType customerType;

    @Email
    @Size(max = 255)
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @NotNull
    @Size(max = 255)
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @NotNull
    @Column(name = "is_first_login", nullable = false)
    private Boolean firstLogin = true;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false, length = 20)
    private UserRole userRole = UserRole.CUSTOMER;

    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private Set<BankAccount> bankAccounts = new HashSet<>();

    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private Set<Loan> loans = new HashSet<>();

    protected Customer() {
    }

    protected Customer(CustomerType customerType) {
        this.customerType = customerType;
    }

    public CustomerType getCustomerType() {
        return customerType;
    }

    protected void setCustomerType(CustomerType customerType) {
        this.customerType = customerType;
    }

    public Set<BankAccount> getBankAccounts() {
        return bankAccounts;
    }

    public Set<Loan> getLoans() {
        return loans;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Boolean isFirstLogin() {
        return firstLogin;
    }

    public UserRole getUserRole() {
        return userRole;
    }

    public void assignOnlineBankingCredentials(String email, String passwordHash, boolean firstLogin, UserRole userRole) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstLogin = firstLogin;
        this.userRole = userRole;
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.firstLogin = false;
    }
}
