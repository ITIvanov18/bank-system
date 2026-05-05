package com.nbu.bank_system.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import org.flywaydb.core.api.callback.BaseCallback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BootstrapEmployeeCallback extends BaseCallback {

    private static final String BOOTSTRAP_EMAIL = "employee@bank.local";
    private static final String BOOTSTRAP_PASSWORD = "Employee@123";
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public boolean supports(Event event, Context context) {
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
        String checkSql = "SELECT id FROM customers WHERE LOWER(email) = LOWER(?) AND user_role = 'EMPLOYEE'";
        try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
            checkStmt.setString(1, BOOTSTRAP_EMAIL);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    updateBootstrapEmployee(connection);
                    return;
                }
            }
        }

        createBootstrapEmployee(connection);
    }

    private void createBootstrapEmployee(Connection connection) throws Exception {
        String passwordHash = passwordEncoder.encode(BOOTSTRAP_PASSWORD);
        LocalDateTime now = LocalDateTime.now();

        String insertCustomerSql = """
                INSERT INTO customers (
                    created_at, updated_at, customer_discriminator, customer_type,
                    email, password_hash, is_first_login, user_role
                ) VALUES (?, ?, 'INDIVIDUAL', 'INDIVIDUAL', ?, ?, false, 'EMPLOYEE')
                """;

        try (PreparedStatement stmt = connection.prepareStatement(insertCustomerSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setObject(1, now);
            stmt.setObject(2, now);
            stmt.setString(3, BOOTSTRAP_EMAIL);
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

            insertIndividualCustomer(connection, customerId, now);
        }
    }

    private void insertIndividualCustomer(Connection connection, long customerId, LocalDateTime now) throws Exception {
        String insertIndividualSql = """
                INSERT INTO individual_customers (id, first_name, last_name, egn)
                VALUES (?, 'System', 'Employee', '0000000000')
                """;

        try (PreparedStatement stmt = connection.prepareStatement(insertIndividualSql)) {
            stmt.setLong(1, customerId);
            stmt.executeUpdate();
        }
    }

    private void updateBootstrapEmployee(Connection connection) throws Exception {
        String passwordHash = passwordEncoder.encode(BOOTSTRAP_PASSWORD);

        String updateSql = """
                UPDATE customers
                SET email = ?, password_hash = ?, is_first_login = false, updated_at = ?
                WHERE LOWER(email) = LOWER(?) AND user_role = 'EMPLOYEE'
                """;

        try (PreparedStatement stmt = connection.prepareStatement(updateSql)) {
            stmt.setString(1, BOOTSTRAP_EMAIL);
            stmt.setString(2, passwordHash);
            stmt.setObject(3, LocalDateTime.now());
            stmt.setString(4, BOOTSTRAP_EMAIL);
            stmt.executeUpdate();
        }
    }
}



