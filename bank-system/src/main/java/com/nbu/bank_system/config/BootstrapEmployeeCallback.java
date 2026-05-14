package com.nbu.bank_system.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import org.flywaydb.core.api.callback.BaseCallback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Flyway callback, който гарантира наличието на начален служител след миграциите.
 * Използва JDBC директно, защото callback-ът работи по време на Flyway lifecycle, преди repository/service слоят да е подходящ за seed логика.
 */

@Component
public class BootstrapEmployeeCallback extends BaseCallback {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final String bootstrapEmail;
    private final String bootstrapPassword;

    public BootstrapEmployeeCallback(
            @Value("${app.bootstrap.employee.email:employee@bank.ai}") String bootstrapEmail,
            @Value("${app.bootstrap.employee.password:Employee@123}") String bootstrapPassword
    ) {
        this.bootstrapEmail = bootstrapEmail.trim().toLowerCase();
        this.bootstrapPassword = bootstrapPassword.trim();
    }

    @Override
    public boolean supports(Event event, Context context) {
        // AFTER_MIGRATE означава, че schema-та вече е създадена и можe да се seed-ват данни
        return event == Event.AFTER_MIGRATE;
    }

    @Override
    public void handle(Event event, Context context) {
        try {
            Connection connection = context.getConnection();
            ensureBootstrapEmployeeExists(connection);
        } catch (Exception e) {
            throw new RuntimeException("Failed to bootstrap employee account", e);
        }
    }

    private void ensureBootstrapEmployeeExists(Connection connection) throws Exception {
        String checkSql = "SELECT id FROM customers WHERE user_role = 'EMPLOYEE' ORDER BY id LIMIT 1";
        try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    // Ако служителят вече съществува, обновяваме credentials за repeatable local setup
                    updateBootstrapEmployee(connection, rs.getLong("id"));
                    return;
                }
            }
        }

        createBootstrapEmployee(connection);
    }

    private void createBootstrapEmployee(Connection connection) throws Exception {
        String passwordHash = passwordEncoder.encode(bootstrapPassword);
        LocalDateTime now = LocalDateTime.now();

        // Employee е моделиран като Customer със специална UserRole, за да използва общия auth механизъм
        String insertCustomerSql = """
                INSERT INTO customers (
                    created_at, updated_at, customer_discriminator, customer_type,
                    email, password_hash, is_first_login, user_role
                ) VALUES (?, ?, 'INDIVIDUAL', 'INDIVIDUAL', ?, ?, false, 'EMPLOYEE')
                """;

        try (PreparedStatement stmt = connection.prepareStatement(insertCustomerSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setObject(1, now);
            stmt.setObject(2, now);
            stmt.setString(3, bootstrapEmail);
            stmt.setString(4, passwordHash);
            stmt.executeUpdate();

            long customerId;
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    customerId = generatedKeys.getLong(1);
                } else {
                    throw new RuntimeException("Failed to retrieve generated customer ID");
                }
            }

            insertIndividualCustomer(connection, customerId);
        }
    }

    private void insertIndividualCustomer(Connection connection, long customerId) throws Exception {
        String insertIndividualSql = """
                INSERT INTO individual_customers (id, first_name, last_name, egn)
                VALUES (?, 'System', 'Employee', '0000000000')
                """;

        try (PreparedStatement stmt = connection.prepareStatement(insertIndividualSql)) {
            stmt.setLong(1, customerId);
            stmt.executeUpdate();
        }
    }

    private void updateBootstrapEmployee(Connection connection, long customerId) throws Exception {
        String passwordHash = passwordEncoder.encode(bootstrapPassword);

        String updateSql = """
                UPDATE customers
                SET email = ?, password_hash = ?, is_first_login = false, updated_at = ?, user_role = 'EMPLOYEE'
                WHERE id = ?
                """;

        try (PreparedStatement stmt = connection.prepareStatement(updateSql)) {
            stmt.setString(1, bootstrapEmail);
            stmt.setString(2, passwordHash);
            stmt.setObject(3, LocalDateTime.now());
            stmt.setLong(4, customerId);
            stmt.executeUpdate();
        }
    }
}



