-- ============================================================================
-- CardDemo PostgreSQL Database Initialization
-- ============================================================================
-- Purpose: Create schema for CardDemo cloud-native transformation
-- Source:  VSAM files (USRSEC, ACCTDAT, CUSTDAT) from z/OS CICS
-- Date:    2025-12-30
-- ============================================================================

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- USERS TABLE (from USRSEC VSAM)
-- ============================================================================

CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(8) PRIMARY KEY,
    first_name VARCHAR(20) NOT NULL,
    last_name VARCHAR(20) NOT NULL,
    password_hash VARCHAR(60) NOT NULL,  -- BCrypt hash
    user_type CHAR(1) NOT NULL CHECK (user_type IN ('A', 'U')),
    customer_id INTEGER,  -- Link to customer for regular users (NULL for admins)
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP,
    CONSTRAINT chk_user_type CHECK (user_type IN ('A', 'U'))
);

COMMENT ON TABLE users IS 'User authentication and authorization (from USRSEC VSAM)';
COMMENT ON COLUMN users.user_id IS 'User ID (8 chars, from SEC-USR-ID)';
COMMENT ON COLUMN users.password_hash IS 'BCrypt password hash (replaces SEC-USR-PWD)';
COMMENT ON COLUMN users.user_type IS 'User type: A=Admin, U=User (from SEC-USR-TYPE)';
COMMENT ON COLUMN users.login_attempts IS 'Failed login counter for security';

CREATE INDEX idx_users_type ON users(user_type);
CREATE INDEX idx_users_active ON users(is_active);
CREATE INDEX idx_users_last_login ON users(last_login DESC);

-- ============================================================================
-- CUSTOMERS TABLE (from CUSTDAT VSAM)
-- ============================================================================

CREATE TABLE IF NOT EXISTS customers (
    customer_id INTEGER PRIMARY KEY,
    first_name VARCHAR(25) NOT NULL,
    last_name VARCHAR(25) NOT NULL,
    date_of_birth DATE,
    fico_credit_score INTEGER CHECK (fico_credit_score BETWEEN 300 AND 850),
    address_line1 VARCHAR(50),
    address_line2 VARCHAR(50),
    city VARCHAR(30),
    state CHAR(2),
    zip_code VARCHAR(10),
    phone_number VARCHAR(15),
    email VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_fico_score CHECK (fico_credit_score IS NULL OR
                                     (fico_credit_score >= 300 AND fico_credit_score <= 850))
);

COMMENT ON TABLE customers IS 'Customer master data (from CUSTDAT VSAM)';
COMMENT ON COLUMN customers.customer_id IS 'Unique customer ID (from CUST-ID)';
COMMENT ON COLUMN customers.fico_credit_score IS 'FICO credit score (300-850)';

CREATE INDEX idx_customers_name ON customers(last_name, first_name);
CREATE INDEX idx_customers_dob ON customers(date_of_birth);
CREATE INDEX idx_customers_state ON customers(state);

-- ============================================================================
-- ACCOUNTS TABLE (from ACCTDAT VSAM)
-- ============================================================================

CREATE TABLE IF NOT EXISTS accounts (
    account_id BIGINT PRIMARY KEY,
    customer_id INTEGER NOT NULL,
    active_status CHAR(1) NOT NULL DEFAULT 'Y' CHECK (active_status IN ('Y', 'N')),
    current_balance DECIMAL(11,2) NOT NULL DEFAULT 0.00,
    credit_limit DECIMAL(11,2) NOT NULL,
    cash_credit_limit DECIMAL(11,2) NOT NULL,
    open_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    reissue_date DATE,
    curr_cycle_credit DECIMAL(11,2) DEFAULT 0.00,
    curr_cycle_debit DECIMAL(11,2) DEFAULT 0.00,
    group_id VARCHAR(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_accounts_customer FOREIGN KEY (customer_id)
        REFERENCES customers(customer_id) ON DELETE RESTRICT,
    CONSTRAINT chk_active_status CHECK (active_status IN ('Y', 'N')),
    CONSTRAINT chk_balance CHECK (current_balance >= -credit_limit),
    CONSTRAINT chk_credit_limit CHECK (credit_limit >= 0),
    CONSTRAINT chk_dates CHECK (expiry_date > open_date)
);

COMMENT ON TABLE accounts IS 'Account master data (from ACCTDAT VSAM)';
COMMENT ON COLUMN accounts.account_id IS 'Account number (11 digits, from ACCT-ID)';
COMMENT ON COLUMN accounts.active_status IS 'Y=Active, N=Closed (from ACCT-ACTIVE-STATUS)';
COMMENT ON COLUMN accounts.current_balance IS 'Current balance (from ACCT-CURR-BAL)';
COMMENT ON COLUMN accounts.credit_limit IS 'Credit limit (from ACCT-CREDIT-LIMIT)';
COMMENT ON COLUMN accounts.curr_cycle_credit IS 'Current cycle credits (from ACCT-CURR-CYC-CREDIT)';
COMMENT ON COLUMN accounts.curr_cycle_debit IS 'Current cycle debits (from ACCT-CURR-CYC-DEBIT)';

CREATE INDEX idx_accounts_customer ON accounts(customer_id);
CREATE INDEX idx_accounts_status ON accounts(active_status);
CREATE INDEX idx_accounts_group ON accounts(group_id);
CREATE INDEX idx_accounts_expiry ON accounts(expiry_date);
CREATE INDEX idx_accounts_balance ON accounts(current_balance);

-- ============================================================================
-- TRANSACTIONS TABLE (future, for CT00/CT01/CT02)
-- ============================================================================

CREATE TABLE IF NOT EXISTS transactions (
    transaction_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    account_id BIGINT NOT NULL,
    transaction_type VARCHAR(4) NOT NULL,
    transaction_category VARCHAR(10),
    transaction_source VARCHAR(10),
    transaction_desc VARCHAR(100),
    transaction_amount DECIMAL(11,2) NOT NULL,
    merchant_id VARCHAR(16),
    merchant_name VARCHAR(50),
    merchant_city VARCHAR(30),
    merchant_zip VARCHAR(10),
    card_number VARCHAR(16),
    original_tranid VARCHAR(16),
    transaction_date DATE NOT NULL,
    transaction_time TIME NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transactions_account FOREIGN KEY (account_id)
        REFERENCES accounts(account_id) ON DELETE RESTRICT
);

COMMENT ON TABLE transactions IS 'Transaction history (from TRANDAT VSAM)';
COMMENT ON COLUMN transactions.transaction_type IS 'Transaction type code (e.g., SALE, REFUND, PAYMENT)';
COMMENT ON COLUMN transactions.transaction_amount IS 'Transaction amount (positive or negative)';

CREATE INDEX idx_transactions_account ON transactions(account_id);
CREATE INDEX idx_transactions_date ON transactions(transaction_date DESC);
CREATE INDEX idx_transactions_type ON transactions(transaction_type);
CREATE INDEX idx_transactions_merchant ON transactions(merchant_id);

-- ============================================================================
-- CREDIT CARDS TABLE (future, for CC00/CCLI/CCVW)
-- ============================================================================

CREATE TABLE IF NOT EXISTS credit_cards (
    card_number VARCHAR(16) PRIMARY KEY,
    account_id BIGINT NOT NULL,
    card_type CHAR(2) NOT NULL CHECK (card_type IN ('VC', 'MC', 'AX', 'DC')),
    embossed_name VARCHAR(50) NOT NULL,
    expiry_date DATE NOT NULL,
    cvv_hash VARCHAR(60),  -- Hashed CVV for security
    active_status CHAR(1) NOT NULL DEFAULT 'Y' CHECK (active_status IN ('Y', 'N', 'S')),
    issued_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cards_account FOREIGN KEY (account_id)
        REFERENCES accounts(account_id) ON DELETE RESTRICT,
    CONSTRAINT chk_card_type CHECK (card_type IN ('VC', 'MC', 'AX', 'DC')),
    CONSTRAINT chk_card_status CHECK (active_status IN ('Y', 'N', 'S'))
);

COMMENT ON TABLE credit_cards IS 'Credit card data (from CARDDAT VSAM)';
COMMENT ON COLUMN credit_cards.card_type IS 'VC=Visa, MC=Mastercard, AX=Amex, DC=Discover';
COMMENT ON COLUMN credit_cards.active_status IS 'Y=Active, N=Closed, S=Stolen';

CREATE INDEX idx_cards_account ON credit_cards(account_id);
CREATE INDEX idx_cards_status ON credit_cards(active_status);
CREATE INDEX idx_cards_expiry ON credit_cards(expiry_date);

-- ============================================================================
-- AUDIT TRAIL TABLE
-- ============================================================================

CREATE TABLE IF NOT EXISTS audit_log (
    audit_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    user_id VARCHAR(8),
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(50),
    old_value TEXT,
    new_value TEXT,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE audit_log IS 'Audit trail for all data modifications';

CREATE INDEX idx_audit_user ON audit_log(user_id);
CREATE INDEX idx_audit_date ON audit_log(created_at DESC);
CREATE INDEX idx_audit_entity ON audit_log(entity_type, entity_id);

-- ============================================================================
-- USER SESSIONS TABLE (for distributed session management)
-- ============================================================================

CREATE TABLE IF NOT EXISTS user_sessions (
    session_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id VARCHAR(8) NOT NULL,
    jwt_token TEXT NOT NULL,
    refresh_token TEXT,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    CONSTRAINT fk_sessions_user FOREIGN KEY (user_id)
        REFERENCES users(user_id) ON DELETE CASCADE
);

COMMENT ON TABLE user_sessions IS 'Active user sessions for JWT token management';

CREATE INDEX idx_sessions_user ON user_sessions(user_id);
CREATE INDEX idx_sessions_expires ON user_sessions(expires_at);
CREATE INDEX idx_sessions_active ON user_sessions(is_active, expires_at);

-- ============================================================================
-- PARTNERS TABLE (for external API integrations)
-- Replaces: CICS Web Services / MQ Series partner configurations
-- ============================================================================

CREATE TABLE IF NOT EXISTS partners (
    partner_id SERIAL PRIMARY KEY,
    partner_name VARCHAR(100) NOT NULL UNIQUE,
    partner_type VARCHAR(20) NOT NULL CHECK (partner_type IN ('FINTECH', 'MERCHANT', 'PROCESSOR', 'BANK')),
    contact_email VARCHAR(100) NOT NULL UNIQUE,
    contact_phone VARCHAR(20),
    webhook_url VARCHAR(255),
    allowed_scopes TEXT[],
    rate_limit_per_minute INTEGER DEFAULT 60,
    daily_quota INTEGER DEFAULT 10000,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE partners IS 'External API partner registry (replaces CICS Web Services partner config)';
COMMENT ON COLUMN partners.partner_type IS 'FINTECH, MERCHANT, PROCESSOR, or BANK';
COMMENT ON COLUMN partners.allowed_scopes IS 'Array of allowed API scopes (e.g., accounts:read, transactions:read)';
COMMENT ON COLUMN partners.rate_limit_per_minute IS 'Max requests per minute (replaces CICS transaction limits)';

CREATE INDEX idx_partners_type ON partners(partner_type);
CREATE INDEX idx_partners_active ON partners(is_active);

-- ============================================================================
-- PARTNER API KEYS TABLE
-- ============================================================================

CREATE TABLE IF NOT EXISTS partner_api_keys (
    key_id SERIAL PRIMARY KEY,
    partner_id INTEGER NOT NULL REFERENCES partners(partner_id) ON DELETE CASCADE,
    api_key_hash VARCHAR(256) NOT NULL,
    key_prefix VARCHAR(12) NOT NULL,
    key_suffix VARCHAR(8),
    description VARCHAR(100),
    scopes TEXT[],
    expires_at TIMESTAMP,
    last_used_at TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE partner_api_keys IS 'Partner API keys (hashed, not stored in plain text)';
COMMENT ON COLUMN partner_api_keys.api_key_hash IS 'SHA-256 hash of the API key';
COMMENT ON COLUMN partner_api_keys.key_prefix IS 'Key prefix for identification (e.g., pk_live_, pk_test_)';

CREATE INDEX idx_partner_keys_hash ON partner_api_keys(api_key_hash);
CREATE INDEX idx_partner_keys_partner ON partner_api_keys(partner_id);
CREATE INDEX idx_partner_keys_active ON partner_api_keys(is_active);

-- ============================================================================
-- PARTNER API LOGS TABLE (Audit trail for partner API requests)
-- Replaces: SMF Records from mainframe
-- ============================================================================

CREATE TABLE IF NOT EXISTS partner_api_logs (
    log_id BIGSERIAL PRIMARY KEY,
    partner_id INTEGER REFERENCES partners(partner_id),
    api_key_prefix VARCHAR(12),
    endpoint VARCHAR(255),
    method VARCHAR(10),
    status_code INTEGER,
    response_time_ms INTEGER,
    ip_address INET,
    user_agent VARCHAR(255),
    request_body_size INTEGER,
    response_body_size INTEGER,
    error_code VARCHAR(50),
    error_message VARCHAR(500),
    request_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE partner_api_logs IS 'Partner API request audit trail (replaces SMF records)';

CREATE INDEX idx_partner_logs_partner ON partner_api_logs(partner_id);
CREATE INDEX idx_partner_logs_timestamp ON partner_api_logs(request_timestamp DESC);
CREATE INDEX idx_partner_logs_endpoint ON partner_api_logs(endpoint);
CREATE INDEX idx_partner_logs_status ON partner_api_logs(status_code);

-- ============================================================================
-- PARTNER DAILY USAGE TABLE (for quota enforcement)
-- ============================================================================

CREATE TABLE IF NOT EXISTS partner_daily_usage (
    usage_id SERIAL PRIMARY KEY,
    partner_id INTEGER NOT NULL REFERENCES partners(partner_id),
    usage_date DATE NOT NULL,
    request_count INTEGER DEFAULT 0,
    successful_count INTEGER DEFAULT 0,
    failed_count INTEGER DEFAULT 0,
    UNIQUE(partner_id, usage_date)
);

COMMENT ON TABLE partner_daily_usage IS 'Daily API usage tracking for quota enforcement';

CREATE INDEX idx_partner_usage_partner_date ON partner_daily_usage(partner_id, usage_date DESC);

-- ============================================================================
-- TRIGGERS FOR updated_at COLUMNS
-- ============================================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_customers_updated_at
    BEFORE UPDATE ON customers
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_accounts_updated_at
    BEFORE UPDATE ON accounts
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_cards_updated_at
    BEFORE UPDATE ON credit_cards
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_partners_updated_at
    BEFORE UPDATE ON partners
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- VIEWS FOR COMMON QUERIES
-- ============================================================================

CREATE OR REPLACE VIEW v_account_summary AS
SELECT
    a.account_id,
    a.customer_id,
    c.first_name,
    c.last_name,
    c.first_name || ' ' || c.last_name AS full_name,
    a.active_status,
    a.current_balance,
    a.credit_limit,
    a.cash_credit_limit,
    (a.credit_limit - a.current_balance) AS available_credit,
    a.open_date,
    a.expiry_date,
    a.group_id,
    EXTRACT(YEAR FROM AGE(CURRENT_DATE, a.open_date)) AS account_age_years
FROM accounts a
INNER JOIN customers c ON a.customer_id = c.customer_id;

COMMENT ON VIEW v_account_summary IS 'Account summary with customer information (for Account View screen)';

-- ============================================================================
-- FUNCTIONS FOR BUSINESS LOGIC
-- ============================================================================

-- Function to validate user credentials (equivalent to COSGN00C login logic)
CREATE OR REPLACE FUNCTION validate_user_login(
    p_user_id VARCHAR(8),
    p_password VARCHAR(8)
)
RETURNS TABLE (
    success BOOLEAN,
    user_type CHAR(1),
    full_name VARCHAR(41),
    message TEXT
) AS $$
DECLARE
    v_stored_hash VARCHAR(60);
    v_is_active BOOLEAN;
    v_login_attempts INTEGER;
    v_locked_until TIMESTAMP;
BEGIN
    -- Get user data
    SELECT password_hash, is_active, login_attempts, locked_until, user_type,
           first_name || ' ' || last_name
    INTO v_stored_hash, v_is_active, v_login_attempts, v_locked_until, user_type, full_name
    FROM users
    WHERE user_id = p_user_id;

    -- User not found
    IF NOT FOUND THEN
        RETURN QUERY SELECT FALSE, NULL::CHAR(1), NULL::VARCHAR(41), 'Invalid user ID or password'::TEXT;
        RETURN;
    END IF;

    -- Check if account is locked
    IF v_locked_until IS NOT NULL AND v_locked_until > CURRENT_TIMESTAMP THEN
        RETURN QUERY SELECT FALSE, NULL::CHAR(1), NULL::VARCHAR(41),
                     'Account locked. Try again later.'::TEXT;
        RETURN;
    END IF;

    -- Check if account is active
    IF NOT v_is_active THEN
        RETURN QUERY SELECT FALSE, NULL::CHAR(1), NULL::VARCHAR(41),
                     'Account disabled. Contact administrator.'::TEXT;
        RETURN;
    END IF;

    -- Validate password (BCrypt comparison)
    IF crypt(p_password, v_stored_hash) = v_stored_hash THEN
        -- Password correct - update last_login and reset attempts
        UPDATE users
        SET last_login = CURRENT_TIMESTAMP,
            login_attempts = 0,
            locked_until = NULL
        WHERE user_id = p_user_id;

        success := TRUE;
        message := 'Login successful';
    ELSE
        -- Password incorrect - increment attempts
        UPDATE users
        SET login_attempts = login_attempts + 1,
            locked_until = CASE
                WHEN login_attempts + 1 >= 5
                THEN CURRENT_TIMESTAMP + INTERVAL '15 minutes'
                ELSE NULL
            END
        WHERE user_id = p_user_id;

        RETURN QUERY SELECT FALSE, NULL::CHAR(1), NULL::VARCHAR(41),
                     'Invalid user ID or password'::TEXT;
        RETURN;
    END IF;

    RETURN QUERY SELECT success, user_type, full_name, message;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION validate_user_login IS 'Validates user login credentials (replaces COSGN00C COBOL logic)';

-- ============================================================================
-- OUTBOX TABLES (Event Sourcing Pattern)
-- Replaces: CICS SYNCPOINT + MQ PUT atomicity (Unit of Work)
-- ============================================================================

CREATE TABLE IF NOT EXISTS transaction_outbox (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE DEFAULT uuid_generate_v4(),
    event_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(50) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP
);

COMMENT ON TABLE transaction_outbox IS 'Outbox table for transaction events (replaces MQ Series atomic messaging)';
COMMENT ON COLUMN transaction_outbox.aggregate_id IS 'Account ID for this transaction';
COMMENT ON COLUMN transaction_outbox.status IS 'PENDING=awaiting publish, PUBLISHED=sent to Kafka, FAILED=error';

CREATE INDEX idx_transaction_outbox_status ON transaction_outbox(status);
CREATE INDEX idx_transaction_outbox_created ON transaction_outbox(created_at);

CREATE TABLE IF NOT EXISTS card_outbox (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE DEFAULT uuid_generate_v4(),
    event_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(50) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP
);

COMMENT ON TABLE card_outbox IS 'Outbox table for card events';
CREATE INDEX idx_card_outbox_status ON card_outbox(status);
CREATE INDEX idx_card_outbox_created ON card_outbox(created_at);

CREATE TABLE IF NOT EXISTS account_outbox (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE DEFAULT uuid_generate_v4(),
    event_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(50) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP
);

COMMENT ON TABLE account_outbox IS 'Outbox table for account events';
CREATE INDEX idx_account_outbox_status ON account_outbox(status);
CREATE INDEX idx_account_outbox_created ON account_outbox(created_at);

-- ============================================================================
-- WEBHOOK TABLES (Partner Notification System)
-- Replaces: CICS Web Services callback patterns
-- ============================================================================

CREATE TABLE IF NOT EXISTS webhook_subscriptions (
    subscription_id SERIAL PRIMARY KEY,
    partner_id INTEGER NOT NULL REFERENCES partners(partner_id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL,
    scope_required VARCHAR(50) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(partner_id, event_type)
);

COMMENT ON TABLE webhook_subscriptions IS 'Partner webhook event subscriptions';
COMMENT ON COLUMN webhook_subscriptions.event_type IS 'Event type to subscribe to (e.g., TransactionCreated, CardStatusChanged)';
COMMENT ON COLUMN webhook_subscriptions.scope_required IS 'Required OAuth scope to receive this event';

CREATE INDEX idx_webhook_subs_partner ON webhook_subscriptions(partner_id);
CREATE INDEX idx_webhook_subs_event ON webhook_subscriptions(event_type);
CREATE INDEX idx_webhook_subs_active ON webhook_subscriptions(is_active);

CREATE TABLE IF NOT EXISTS webhook_deliveries (
    delivery_id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL,
    partner_id INTEGER NOT NULL REFERENCES partners(partner_id),
    webhook_url VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'DEAD_LETTER')),
    attempt_count INTEGER DEFAULT 0,
    next_attempt_at TIMESTAMP,
    last_error VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

COMMENT ON TABLE webhook_deliveries IS 'Webhook delivery tracking with retry logic';
COMMENT ON COLUMN webhook_deliveries.status IS 'PENDING, SUCCESS, FAILED (retrying), DEAD_LETTER (max retries exceeded)';
COMMENT ON COLUMN webhook_deliveries.attempt_count IS 'Number of delivery attempts (max 5)';
COMMENT ON COLUMN webhook_deliveries.next_attempt_at IS 'When to retry (exponential backoff: 1, 5, 15, 60, 240 min)';

CREATE INDEX idx_webhook_del_status ON webhook_deliveries(status);
CREATE INDEX idx_webhook_del_partner ON webhook_deliveries(partner_id);
CREATE INDEX idx_webhook_del_retry ON webhook_deliveries(status, next_attempt_at) WHERE status IN ('PENDING', 'FAILED');
CREATE INDEX idx_webhook_del_event ON webhook_deliveries(event_id);

-- ============================================================================
-- GRANT PERMISSIONS
-- ============================================================================

-- Grant permissions to carddemo user
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO carddemo;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO carddemo;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO carddemo;

-- ============================================================================
-- DATABASE READY
-- ============================================================================

SELECT 'CardDemo PostgreSQL database initialized successfully!' AS status;
