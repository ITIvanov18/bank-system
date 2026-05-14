package com.nbu.bank_system.domain.model.account;

import com.nbu.bank_system.domain.model.BaseEntity;
import com.nbu.bank_system.domain.model.customer.Customer;
import com.nbu.bank_system.domain.enums.AccountStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Представя банкова сметка, притежавана от customer.
 * Съдържа уникален IBAN, текущ баланс и operational account status, а domain методите пазят валидността на credit/debit операциите.
 */

@Getter
@Entity
@Table(
        name = "bank_accounts",
        uniqueConstraints = {
                @UniqueConstraint(name = "bg_bank_account_iban", columnNames = "iban")
        }
)
public class BankAccount extends BaseEntity {

    @NotBlank
    @Pattern(
            regexp = "^BG\\d{2}[A-Z]{4}\\d{4}\\d{2}\\d{8}$",
            message = "Invalid Bulgarian IBAN format. Expected: BG + 2 digits + 4 letters + 4 digits + 2 digits + 8 digits."
    )
    @Column(name = "iban", nullable = false, length = 34)
    private String iban;

    @Setter
    @NotNull
    @Digits(integer = 19, fraction = 2)
    @DecimalMin(value = "0.00")
    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Setter
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AccountStatus status;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "owner_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_bank_account_owner")
    )
    private Customer owner;

    protected BankAccount() {
    }

    public BankAccount(String iban, BigDecimal balance, AccountStatus status, Customer owner) {
        this.iban = iban;
        this.balance = balance;
        this.status = status;
        this.owner = owner;
    }

    public void credit(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive.");
        }
        this.balance = this.balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive.");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance.");
        }
        this.balance = this.balance.subtract(amount);
    }

}
