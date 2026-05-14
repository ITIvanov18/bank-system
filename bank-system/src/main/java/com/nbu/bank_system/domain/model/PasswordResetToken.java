package com.nbu.bank_system.domain.model;

import com.nbu.bank_system.domain.model.customer.Customer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Persistent model за еднократни password reset tokens.
 * В базата се пази само SHA-256 hash на token-а, срок на валидност и used marker, за да не се съхранява директният reset линк.
 */

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    protected PasswordResetToken() {
    }

    public PasswordResetToken(Customer customer, String tokenHash, LocalDateTime expiresAt) {
        this.customer = customer;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public Customer getCustomer() {
        return customer;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public void markUsed(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }
}
