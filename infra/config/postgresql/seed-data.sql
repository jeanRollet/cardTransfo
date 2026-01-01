-- ============================================================================
-- CardDemo Test Data Seeding
-- ============================================================================
-- Purpose: Load test data matching z/OS CICS CardDemo application
-- Source:  Based on USER0001/ADMIN001 from USRSEC VSAM
-- Date:    2025-12-30
-- ============================================================================

-- ============================================================================
-- USERS (from USRSEC VSAM)
-- ============================================================================
-- Password: "PASSWORD" hashed with BCrypt (rounds=10)
-- BCrypt hash: $2b$10$OHvubn0WB3jzCdC0AlFubOIzH/v/l01gGk31V8WorCjjLudnPltXe

INSERT INTO users (user_id, first_name, last_name, password_hash, user_type, customer_id, is_active) VALUES
('USER0001', 'John', 'Doe', '$2b$10$OHvubn0WB3jzCdC0AlFubOIzH/v/l01gGk31V8WorCjjLudnPltXe', 'U', 123456789, TRUE),
('USER0002', 'Jane', 'Smith', '$2b$10$OHvubn0WB3jzCdC0AlFubOIzH/v/l01gGk31V8WorCjjLudnPltXe', 'U', 123456790, TRUE),
('USER0003', 'Bob', 'Johnson', '$2b$10$OHvubn0WB3jzCdC0AlFubOIzH/v/l01gGk31V8WorCjjLudnPltXe', 'U', 123456791, TRUE),
('USER0004', 'Alice', 'Williams', '$2b$10$OHvubn0WB3jzCdC0AlFubOIzH/v/l01gGk31V8WorCjjLudnPltXe', 'U', 123456792, TRUE),
('USER0005', 'Charlie', 'Brown', '$2b$10$OHvubn0WB3jzCdC0AlFubOIzH/v/l01gGk31V8WorCjjLudnPltXe', 'U', 123456793, TRUE),
('ADMIN001', 'Admin', 'User', '$2b$10$OHvubn0WB3jzCdC0AlFubOIzH/v/l01gGk31V8WorCjjLudnPltXe', 'A', NULL, TRUE),
('ADMIN002', 'Super', 'Admin', '$2b$10$OHvubn0WB3jzCdC0AlFubOIzH/v/l01gGk31V8WorCjjLudnPltXe', 'A', NULL, TRUE)
ON CONFLICT (user_id) DO NOTHING;

-- ============================================================================
-- CUSTOMERS (from CUSTDAT VSAM)
-- ============================================================================

INSERT INTO customers (customer_id, first_name, last_name, date_of_birth, fico_credit_score,
                       address_line1, city, state, zip_code, phone_number, email) VALUES
(123456789, 'John', 'Doe', '1985-03-15', 720,
 '123 Main Street', 'New York', 'NY', '10001', '212-555-0101', 'john.doe@email.com'),
(123456790, 'Jane', 'Smith', '1990-07-22', 780,
 '456 Oak Avenue', 'Los Angeles', 'CA', '90001', '310-555-0102', 'jane.smith@email.com'),
(123456791, 'Bob', 'Johnson', '1978-11-30', 650,
 '789 Pine Road', 'Chicago', 'IL', '60601', '312-555-0103', 'bob.johnson@email.com'),
(123456792, 'Alice', 'Williams', '1995-01-10', 800,
 '321 Elm Street', 'Houston', 'TX', '77001', '713-555-0104', 'alice.williams@email.com'),
(123456793, 'Charlie', 'Brown', '1982-05-25', 690,
 '654 Maple Drive', 'Phoenix', 'AZ', '85001', '602-555-0105', 'charlie.brown@email.com'),
(123456794, 'Diana', 'Prince', '1988-08-14', 750,
 '987 Cedar Lane', 'Philadelphia', 'PA', '19019', '215-555-0106', 'diana.prince@email.com'),
(123456795, 'Eve', 'Anderson', '1992-12-05', 710,
 '147 Birch Court', 'San Antonio', 'TX', '78201', '210-555-0107', 'eve.anderson@email.com'),
(123456796, 'Frank', 'Miller', '1975-09-18', 640,
 '258 Willow Way', 'San Diego', 'CA', '92101', '619-555-0108', 'frank.miller@email.com'),
(123456797, 'Grace', 'Lee', '1998-04-27', 670,
 '369 Spruce Path', 'Dallas', 'TX', '75201', '214-555-0109', 'grace.lee@email.com'),
(123456798, 'Henry', 'Taylor', '1980-06-12', 730,
 '741 Ash Boulevard', 'San Jose', 'CA', '95101', '408-555-0110', 'henry.taylor@email.com')
ON CONFLICT (customer_id) DO NOTHING;

-- ============================================================================
-- ACCOUNTS (from ACCTDAT VSAM)
-- ============================================================================

INSERT INTO accounts (account_id, customer_id, active_status, current_balance, credit_limit,
                      cash_credit_limit, open_date, expiry_date, reissue_date,
                      curr_cycle_credit, curr_cycle_debit, group_id) VALUES
-- John Doe's accounts
(1000000001, 123456789, 'Y', 1250.50, 5000.00, 1000.00,
 '2020-01-15', '2026-01-31', '2025-12-01', 2500.00, 1249.50, 'GRP001'),
(1000000002, 123456789, 'Y', -350.25, 3000.00, 500.00,
 '2021-06-20', '2027-06-30', NULL, 1200.00, 1550.25, 'GRP001'),

-- Jane Smith's account
(1000000003, 123456790, 'Y', 5240.75, 10000.00, 2000.00,
 '2019-03-10', '2025-03-31', '2024-12-15', 8000.00, 2759.25, 'GRP002'),

-- Bob Johnson's account
(1000000004, 123456791, 'Y', 890.00, 2500.00, 250.00,
 '2022-11-05', '2028-11-30', NULL, 500.00, -610.00, 'GRP003'),

-- Alice Williams's accounts (Premium customer)
(1000000005, 123456792, 'Y', 12500.00, 25000.00, 5000.00,
 '2018-05-01', '2025-05-31', '2024-10-01', 15000.00, 2500.00, 'GRP004'),
(1000000006, 123456792, 'Y', 3200.00, 15000.00, 3000.00,
 '2020-08-15', '2026-08-31', NULL, 5000.00, 1800.00, 'GRP004'),

-- Charlie Brown's account
(1000000007, 123456793, 'Y', 1567.30, 4000.00, 800.00,
 '2021-02-28', '2027-02-28', NULL, 2000.00, 432.70, 'GRP005'),

-- Diana Prince's account
(1000000008, 123456794, 'Y', 2890.50, 7500.00, 1500.00,
 '2019-09-12', '2025-09-30', '2024-11-01', 4500.00, 1609.50, 'GRP006'),

-- Eve Anderson's account
(1000000009, 123456795, 'Y', 450.00, 3500.00, 700.00,
 '2022-04-01', '2028-04-30', NULL, 1000.00, 550.00, 'GRP007'),

-- Frank Miller's account (Inactive)
(1000000010, 123456796, 'N', 0.00, 2000.00, 400.00,
 '2018-12-01', '2024-12-31', NULL, 0.00, 0.00, 'GRP008'),

-- Grace Lee's account
(1000000011, 123456797, 'Y', 678.90, 3000.00, 600.00,
 '2023-01-10', '2029-01-31', NULL, 800.00, 121.10, 'GRP009'),

-- Henry Taylor's account
(1000000012, 123456798, 'Y', 3456.78, 8000.00, 1600.00,
 '2020-07-22', '2026-07-31', '2025-06-01', 5000.00, 1543.22, 'GRP010')
ON CONFLICT (account_id) DO NOTHING;

-- ============================================================================
-- CREDIT CARDS (from CARDDAT VSAM)
-- ============================================================================

INSERT INTO credit_cards (card_number, account_id, card_type, embossed_name,
                          expiry_date, active_status, issued_date) VALUES
-- Visa cards
('4532123456789012', 1000000001, 'VC', 'JOHN DOE', '2026-01-31', 'Y', '2020-01-15'),
('4532123456789023', 1000000002, 'VC', 'JOHN DOE', '2027-06-30', 'Y', '2021-06-20'),
('4532123456789034', 1000000003, 'VC', 'JANE SMITH', '2025-03-31', 'Y', '2019-03-10'),
('4532123456789045', 1000000007, 'VC', 'CHARLIE BROWN', '2027-02-28', 'Y', '2021-02-28'),

-- Mastercard cards
('5412345678901234', 1000000004, 'MC', 'BOB JOHNSON', '2028-11-30', 'Y', '2022-11-05'),
('5412345678901245', 1000000008, 'MC', 'DIANA PRINCE', '2025-09-30', 'Y', '2019-09-12'),
('5412345678901256', 1000000009, 'MC', 'EVE ANDERSON', '2028-04-30', 'Y', '2022-04-01'),

-- American Express cards (Premium)
('378282246310005', 1000000005, 'AX', 'ALICE WILLIAMS', '2025-05-31', 'Y', '2018-05-01'),
('378282246310016', 1000000006, 'AX', 'ALICE WILLIAMS', '2026-08-31', 'Y', '2020-08-15'),

-- Discover cards
('6011123456789012', 1000000011, 'DC', 'GRACE LEE', '2029-01-31', 'Y', '2023-01-10'),
('6011123456789023', 1000000012, 'DC', 'HENRY TAYLOR', '2026-07-31', 'Y', '2020-07-22'),

-- Inactive card
('4532123456789099', 1000000010, 'VC', 'FRANK MILLER', '2024-12-31', 'N', '2018-12-01')
ON CONFLICT (card_number) DO NOTHING;

-- ============================================================================
-- TRANSACTIONS (Sample data for testing)
-- ============================================================================

INSERT INTO transactions (account_id, transaction_type, transaction_category, transaction_source,
                          transaction_desc, transaction_amount, merchant_id, merchant_name,
                          merchant_city, merchant_zip, card_number, transaction_date, transaction_time) VALUES
-- Recent transactions for account 1000000001 (John Doe)
(1000000001, 'SALE', 'GROCERY', 'POS', 'Grocery Purchase', -125.50, 'MERCH001', 'Whole Foods',
 'New York', '10001', '4532123456789012', CURRENT_DATE - INTERVAL '1 day', '14:23:15'),
(1000000001, 'SALE', 'GAS', 'POS', 'Gas Station', -45.00, 'MERCH002', 'Shell Gas',
 'New York', '10002', '4532123456789012', CURRENT_DATE - INTERVAL '2 days', '08:15:30'),
(1000000001, 'PYMT', 'PAYMENT', 'ONLINE', 'Online Payment', 500.00, NULL, 'Bank Transfer',
 NULL, NULL, NULL, CURRENT_DATE - INTERVAL '5 days', '18:45:00'),

-- Transactions for account 1000000003 (Jane Smith)
(1000000003, 'SALE', 'RETAIL', 'ONLINE', 'Online Shopping', -259.25, 'MERCH003', 'Amazon',
 'Seattle', '98101', '4532123456789034', CURRENT_DATE - INTERVAL '1 day', '20:12:45'),
(1000000003, 'SALE', 'DINING', 'POS', 'Restaurant', -85.50, 'MERCH004', 'Italian Bistro',
 'Los Angeles', '90001', '4532123456789034', CURRENT_DATE - INTERVAL '3 days', '19:30:22'),

-- Transaction for account 1000000005 (Alice Williams - Premium)
(1000000005, 'SALE', 'TRAVEL', 'ONLINE', 'Airline Ticket', -1250.00, 'MERCH005', 'United Airlines',
 'Chicago', '60601', '378282246310005', CURRENT_DATE - INTERVAL '7 days', '10:00:00'),
(1000000005, 'PYMT', 'PAYMENT', 'AUTOPAY', 'Auto Payment', 5000.00, NULL, 'Bank Auto Payment',
 NULL, NULL, NULL, CURRENT_DATE - INTERVAL '10 days', '00:01:00')
ON CONFLICT DO NOTHING;

-- ============================================================================
-- PARTNERS (for API integration testing)
-- ============================================================================

INSERT INTO partners (partner_name, partner_type, contact_email, contact_phone, webhook_url,
                      allowed_scopes, rate_limit_per_minute, daily_quota, is_active) VALUES
('Fintech ABC', 'FINTECH', 'api@fintechabc.com', '+1-555-0001',
 'https://webhook.fintechabc.com/carddemo',
 ARRAY['accounts:read', 'transactions:read'], 60, 10000, TRUE),
('PayPro Merchants', 'MERCHANT', 'integration@paypro.com', '+1-555-0002',
 NULL,
 ARRAY['transactions:read'], 30, 5000, TRUE),
('Global Payments Inc', 'PROCESSOR', 'partners@globalpay.com', '+1-555-0003',
 'https://api.globalpay.com/webhooks/carddemo',
 ARRAY['accounts:read', 'transactions:read', 'cards:read'], 120, 50000, TRUE),
('Regional Bank Corp', 'BANK', 'api-team@regionalbank.com', '+1-555-0004',
 NULL,
 ARRAY['accounts:read', 'transactions:read'], 100, 25000, TRUE)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- PARTNER API KEYS (Test keys - DO NOT USE IN PRODUCTION)
-- ============================================================================
-- Test API Key for Fintech ABC: pk_test_abc123xyz456def789ghi012jkl345mno678
-- SHA-256 Hash: 8b2e97c2a0dcc9c5f4b8a7e6d5c4b3a2918f7e6d5c4b3a2918f7e6d5c4b3a291

INSERT INTO partner_api_keys (partner_id, api_key_hash, key_prefix, key_suffix,
                              description, scopes, expires_at, is_active) VALUES
(1, '8b2e97c2a0dcc9c5f4b8a7e6d5c4b3a2918f7e6d5c4b3a2918f7e6d5c4b3a291',
 'pk_test_', 'no678', 'Test API Key for development', NULL, NULL, TRUE),
(2, 'a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2',
 'pk_live_', 'klm321', 'Production API Key', ARRAY['transactions:read'], NULL, TRUE),
(3, 'f1e2d3c4b5a6f7e8d9c0b1a2f3e4d5c6b7a8f9e0d1c2b3a4f5e6d7c8b9a0f1e2',
 'pk_live_', 'xyz999', 'Full access key', NULL, NULL, TRUE)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- AUDIT LOG (Initial system setup entries)
-- ============================================================================

INSERT INTO audit_log (user_id, action, entity_type, entity_id, new_value, ip_address) VALUES
('ADMIN001', 'SYSTEM_INIT', 'DATABASE', 'carddemo', 'Database initialized', '127.0.0.1'),
('ADMIN001', 'DATA_SEED', 'USERS', 'ALL', 'Test users created', '127.0.0.1'),
('ADMIN001', 'DATA_SEED', 'CUSTOMERS', 'ALL', 'Test customers created', '127.0.0.1'),
('ADMIN001', 'DATA_SEED', 'ACCOUNTS', 'ALL', 'Test accounts created', '127.0.0.1'),
('ADMIN001', 'DATA_SEED', 'CREDIT_CARDS', 'ALL', 'Test cards created', '127.0.0.1'),
('ADMIN001', 'DATA_SEED', 'TRANSACTIONS', 'ALL', 'Sample transactions created', '127.0.0.1');

-- ============================================================================
-- STATISTICS
-- ============================================================================

-- Update statistics for query optimization
ANALYZE users;
ANALYZE customers;
ANALYZE accounts;
ANALYZE credit_cards;
ANALYZE transactions;
ANALYZE audit_log;
ANALYZE partners;
ANALYZE partner_api_keys;

-- Display summary
SELECT 'Data seeding completed!' AS status;
SELECT 'Users: ' || COUNT(*) AS summary FROM users
UNION ALL
SELECT 'Customers: ' || COUNT(*) FROM customers
UNION ALL
SELECT 'Accounts: ' || COUNT(*) FROM accounts
UNION ALL
SELECT 'Credit Cards: ' || COUNT(*) FROM credit_cards
UNION ALL
SELECT 'Transactions: ' || COUNT(*) FROM transactions
UNION ALL
SELECT 'Partners: ' || COUNT(*) FROM partners
UNION ALL
SELECT 'Partner API Keys: ' || COUNT(*) FROM partner_api_keys;

-- ============================================================================
-- TEST USER CREDENTIALS
-- ============================================================================
/*
Test Users (all passwords: "PASSWORD"):

Standard Users:
- USER0001 / PASSWORD (John Doe)
- USER0002 / PASSWORD (Jane Smith)
- USER0003 / PASSWORD (Bob Johnson)
- USER0004 / PASSWORD (Alice Williams)
- USER0005 / PASSWORD (Charlie Brown)

Admin Users:
- ADMIN001 / PASSWORD (Admin User)
- ADMIN002 / PASSWORD (Super Admin)

Test Accounts:
- 1000000001 (John Doe, Balance: $1,250.50)
- 1000000003 (Jane Smith, Balance: $5,240.75)
- 1000000005 (Alice Williams, Balance: $12,500.00 - Premium)

============================================================================
PARTNER API TEST CREDENTIALS
============================================================================
Partner 1: Fintech ABC
- Partner ID: 1
- Type: FINTECH
- Rate Limit: 60 requests/minute
- Daily Quota: 10,000 requests
- Scopes: accounts:read, transactions:read
- Test API Key: (Generate via POST /api/v1/partners/1/keys)

Partner 2: PayPro Merchants
- Partner ID: 2
- Type: MERCHANT
- Rate Limit: 30 requests/minute
- Scopes: transactions:read

Partner 3: Global Payments Inc
- Partner ID: 3
- Type: PROCESSOR
- Rate Limit: 120 requests/minute
- Daily Quota: 50,000 requests
- Scopes: accounts:read, transactions:read, cards:read

API Endpoints:
- Partner Management: http://localhost:8085/api/v1/partners
- Partner API: http://localhost:8085/partner/v1/*
- Documentation: http://localhost:8085/swagger-ui.html

Example API Key Usage:
curl -H "X-API-Key: pk_live_your_api_key_here" http://localhost:8085/partner/v1/accounts/1000000001
*/
