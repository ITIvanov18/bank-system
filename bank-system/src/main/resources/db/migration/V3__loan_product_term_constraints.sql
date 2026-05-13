ALTER TABLE loans
    ADD CONSTRAINT chk_loans_product_terms CHECK (
        (
            loan_type = 'CONSUMER'
            AND principal_amount BETWEEN 1000.00 AND 40000.00
            AND repayment_term_months BETWEEN 12 AND 120
            AND MOD(principal_amount - 1000.00, 5.00) = 0
        )
        OR
        (
            loan_type = 'MORTGAGE'
            AND principal_amount BETWEEN 3000.00 AND 500000.00
            AND repayment_term_months BETWEEN 1 AND 360
            AND MOD(principal_amount - 3000.00, 500.00) = 0
        )
    );
