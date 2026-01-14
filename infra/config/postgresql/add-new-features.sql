-- ============================================================================
-- CardDemo Additional Features - Bill Payment & Pending Authorization
-- ============================================================================
-- Purpose: Add tables for Bill Payment (COBIL00C) and Pending Auth (COAUTH0C)
-- Date:    2026-01-14
-- ============================================================================

-- ============================================================================
-- BILL PAYEES TABLE (for Bill Payment feature)
-- ============================================================================

CREATE TABLE IF NOT EXISTS bill_payees (
    payee_id SERIAL PRIMARY KEY,
    customer_id INTEGER NOT NULL REFERENCES customers(customer_id),
    payee_name VARCHAR(100) NOT NULL,
    payee_type VARCHAR(20) NOT NULL CHECK (payee_type IN ('UTILITY', 'CREDIT_CARD', 'LOAN', 'INSURANCE', 'TELECOM', 'OTHER')),
    payee_account_number VARCHAR(50),
    payee_routing_number VARCHAR(20),
    payee_address VARCHAR(200),
    nickname VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE bill_payees IS 'Bill payment payees/beneficiaries (for COBIL00C)';
CREATE INDEX idx_bill_payees_customer ON bill_payees(customer_id);
CREATE INDEX idx_bill_payees_type ON bill_payees(payee_type);

-- ============================================================================
-- BILL PAYMENTS TABLE
-- ============================================================================

CREATE TABLE IF NOT EXISTS bill_payments (
    payment_id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES accounts(account_id),
    payee_id INTEGER NOT NULL REFERENCES bill_payees(payee_id),
    amount DECIMAL(11,2) NOT NULL CHECK (amount > 0),
    payment_date DATE NOT NULL,
    scheduled_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SCHEDULED', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    confirmation_number VARCHAR(20),
    memo VARCHAR(100),
    is_recurring BOOLEAN DEFAULT FALSE,
    recurring_frequency VARCHAR(20) CHECK (recurring_frequency IN ('WEEKLY', 'BIWEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY')),
    next_payment_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    error_message VARCHAR(200)
);

COMMENT ON TABLE bill_payments IS 'Bill payment transactions (for COBIL00C)';
CREATE INDEX idx_bill_payments_account ON bill_payments(account_id);
CREATE INDEX idx_bill_payments_payee ON bill_payments(payee_id);
CREATE INDEX idx_bill_payments_status ON bill_payments(status);
CREATE INDEX idx_bill_payments_date ON bill_payments(payment_date DESC);
CREATE INDEX idx_bill_payments_scheduled ON bill_payments(scheduled_date) WHERE status = 'SCHEDULED';

-- ============================================================================
-- PENDING AUTHORIZATIONS TABLE (for COAUTH0C)
-- ============================================================================

CREATE TABLE IF NOT EXISTS pending_authorizations (
    auth_id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES accounts(account_id),
    card_number VARCHAR(16) NOT NULL,
    merchant_name VARCHAR(100) NOT NULL,
    merchant_category VARCHAR(50),
    merchant_city VARCHAR(50),
    merchant_country VARCHAR(3) DEFAULT 'USA',
    amount DECIMAL(11,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    auth_code VARCHAR(10),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'DECLINED', 'EXPIRED', 'SETTLED', 'REVERSED')),
    decline_reason VARCHAR(100),
    risk_score INTEGER CHECK (risk_score BETWEEN 0 AND 100),
    is_fraud_alert BOOLEAN DEFAULT FALSE,
    auth_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expiry_timestamp TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '7 days'),
    settled_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE pending_authorizations IS 'Pending transaction authorizations (for COAUTH0C)';
COMMENT ON COLUMN pending_authorizations.status IS 'PENDING=awaiting settlement, APPROVED=authorized, DECLINED=rejected, EXPIRED=auth expired, SETTLED=completed, REVERSED=voided';
COMMENT ON COLUMN pending_authorizations.risk_score IS 'Fraud risk score 0-100 (higher = more risky)';

CREATE INDEX idx_pending_auth_account ON pending_authorizations(account_id);
CREATE INDEX idx_pending_auth_card ON pending_authorizations(card_number);
CREATE INDEX idx_pending_auth_status ON pending_authorizations(status);
CREATE INDEX idx_pending_auth_pending ON pending_authorizations(status, auth_timestamp) WHERE status = 'PENDING';
CREATE INDEX idx_pending_auth_fraud ON pending_authorizations(is_fraud_alert) WHERE is_fraud_alert = TRUE;

-- ============================================================================
-- SAMPLE DATA FOR TESTING
-- ============================================================================

-- Sample Bill Payees
INSERT INTO bill_payees (customer_id, payee_name, payee_type, payee_account_number, nickname) VALUES
(123456789, 'Electric Company USA', 'UTILITY', 'ELEC-001-789', 'Electric Bill'),
(123456789, 'City Water Services', 'UTILITY', 'WATER-456-123', 'Water Bill'),
(123456789, 'ABC Insurance Co', 'INSURANCE', 'INS-2024-001', 'Car Insurance'),
(123456789, 'Verizon Wireless', 'TELECOM', '555-123-4567', 'Phone Bill'),
(123456790, 'Gas & Power Co', 'UTILITY', 'GAS-789-456', 'Gas Bill'),
(123456790, 'Home Mortgage Inc', 'LOAN', 'MTG-2020-5678', 'Mortgage');

-- Sample Pending Authorizations
INSERT INTO pending_authorizations (account_id, card_number, merchant_name, merchant_category, merchant_city, amount, status, risk_score, is_fraud_alert) VALUES
(1000000001, '4532015112830366', 'Amazon.com', 'ONLINE', 'Seattle', 156.99, 'PENDING', 15, FALSE),
(1000000001, '4532015112830366', 'Uber Eats', 'DINING', 'New York', 45.50, 'PENDING', 10, FALSE),
(1000000002, '5425233430109903', 'Best Buy', 'RETAIL', 'Chicago', 899.99, 'PENDING', 45, FALSE),
(1000000002, '5425233430109903', 'Unknown Merchant XYZ', 'OTHER', 'Lagos', 2500.00, 'PENDING', 95, TRUE),
(1000000003, '4916338506082832', 'Walmart', 'RETAIL', 'Dallas', 234.56, 'PENDING', 5, FALSE),
(1000000003, '4916338506082832', 'Gas Station #1234', 'GAS', 'Houston', 55.00, 'APPROVED', 8, FALSE),
(1000000004, '5425233430109904', 'Netflix', 'ONLINE', 'Los Gatos', 15.99, 'PENDING', 3, FALSE);

-- Sample Bill Payments
INSERT INTO bill_payments (account_id, payee_id, amount, payment_date, status, confirmation_number, is_recurring, recurring_frequency) VALUES
(1000000001, 1, 125.50, CURRENT_DATE, 'COMPLETED', 'CONF-001-2026', TRUE, 'MONTHLY'),
(1000000001, 2, 45.00, CURRENT_DATE + INTERVAL '5 days', 'SCHEDULED', NULL, TRUE, 'MONTHLY'),
(1000000001, 3, 189.00, CURRENT_DATE - INTERVAL '2 days', 'COMPLETED', 'CONF-002-2026', FALSE, NULL),
(1000000002, 5, 78.50, CURRENT_DATE, 'PENDING', NULL, FALSE, NULL),
(1000000002, 6, 1250.00, CURRENT_DATE + INTERVAL '10 days', 'SCHEDULED', NULL, TRUE, 'MONTHLY');

-- Grant permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO carddemo;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO carddemo;

SELECT 'New features tables created successfully!' AS status;
