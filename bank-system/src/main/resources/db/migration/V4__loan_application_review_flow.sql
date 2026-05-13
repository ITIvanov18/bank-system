ALTER TABLE loans
    MODIFY status ENUM('PENDING', 'ACTIVE', 'REJECTED', 'CLOSED') NOT NULL,
    MODIFY start_date DATE NULL,
    ADD COLUMN reviewed_at DATETIME(6) NULL;

CREATE TABLE IF NOT EXISTS loan_review_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    loan_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    decision ENUM('APPROVED', 'REJECTED') NOT NULL,
    loan_type ENUM('CONSUMER', 'MORTGAGE') NOT NULL,
    principal_amount DECIMAL(19,2) NOT NULL,
    annual_interest_rate DECIMAL(9,4) NOT NULL,
    repayment_term_months INT NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    employee_email VARCHAR(255) NOT NULL,
    decision_note VARCHAR(500) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_loan_review_log_loan
        FOREIGN KEY (loan_id) REFERENCES loans (id),
    CONSTRAINT fk_loan_review_log_customer
        FOREIGN KEY (customer_id) REFERENCES customers (id)
);
