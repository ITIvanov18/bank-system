CREATE TABLE IF NOT EXISTS installment_payments_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    loan_id BIGINT NOT NULL,
    installment_id BIGINT NOT NULL,
    amount_paid DECIMAL(19,2) NOT NULL,
    paid_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_installment_payments_log_loan
        FOREIGN KEY (loan_id) REFERENCES loans (id),
    CONSTRAINT fk_installment_payments_log_installment
        FOREIGN KEY (installment_id) REFERENCES installments (id)
);

