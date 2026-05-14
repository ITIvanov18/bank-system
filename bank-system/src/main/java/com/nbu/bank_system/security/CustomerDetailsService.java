package com.nbu.bank_system.security;

import com.nbu.bank_system.domain.model.customer.Customer;
import com.nbu.bank_system.repository.CustomerRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * UserDetailsService implementation за email-based authentication.
 * Зарежда Customer от repository слоя и го адаптира до BankUserPrincipal за Spring Security.
 */

@Service
public class CustomerDetailsService implements UserDetailsService {

    private final CustomerRepository customerRepository;

    public CustomerDetailsService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Customer customer = customerRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("No user found with email: " + username));
        return BankUserPrincipal.from(customer);
    }
}

