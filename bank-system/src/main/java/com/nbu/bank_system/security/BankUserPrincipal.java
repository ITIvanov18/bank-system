package com.nbu.bank_system.security;

import com.nbu.bank_system.domain.enums.UserRole;
import com.nbu.bank_system.domain.model.customer.Customer;
import java.util.Collection;
import java.util.List;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Spring Security principal adapter върху домейн Customer.
 * Превежда customer id, email, password hash, role и first-login флага към UserDetails contract-а.
 */

public class BankUserPrincipal implements UserDetails {

    @Getter
    private final Long id;
    private final String email;
    private final String passwordHash;
    @Getter
    private final UserRole userRole;
    @Getter
    private final boolean firstLogin;

    public BankUserPrincipal(Long id, String email, String passwordHash, UserRole userRole, boolean firstLogin) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.userRole = userRole;
        this.firstLogin = firstLogin;
    }

    public static BankUserPrincipal from(Customer customer) {
        return new BankUserPrincipal(
                customer.getId(),
                customer.getEmail(),
                customer.getPasswordHash(),
                customer.getUserRole(),
                customer.isFirstLogin()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + userRole.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}

