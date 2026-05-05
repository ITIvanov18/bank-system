package com.nbu.bank_system.config;

import com.nbu.bank_system.domain.enums.UserRole;
import com.nbu.bank_system.domain.model.customer.IndividualCustomer;
import com.nbu.bank_system.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

public class EmployeeBootstrap implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.employee.email:employee@bank.local}")
    private String employeeEmail;

    @Value("${app.bootstrap.employee.password:Employee@123}")
    private String employeePassword;

    public EmployeeBootstrap(CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        String normalizedEmail = employeeEmail.trim().toLowerCase();
        String configuredPassword = employeePassword.trim();

        if (normalizedEmail.isBlank() || configuredPassword.isBlank()) {
            throw new IllegalStateException("Bootstrap employee credentials must not be blank");
        }

        String encodedPassword = passwordEncoder.encode(configuredPassword);
        int updatedRows = customerRepository.updateBootstrapEmployeeCredentials(normalizedEmail, encodedPassword);
        if (updatedRows > 0) {
            return;
        }

        int updatedExistingEmployee = customerRepository.updateAnyEmployeeCredentials(normalizedEmail, encodedPassword);
        if (updatedExistingEmployee > 0) {
            return;
        }

        IndividualCustomer employee = new IndividualCustomer("System", "Employee", "0000000000");

        employee.assignOnlineBankingCredentials(
                normalizedEmail,
                encodedPassword,
                false,
                UserRole.EMPLOYEE
        );

        customerRepository.save(employee);
    }
}

