#!/usr/bin/env python3
"""
CardDemo Data Import Script
Parses COBOL fixed-width ASCII files and imports into PostgreSQL
Source: AWS CardDemo mainframe application
"""

import psycopg2
from psycopg2.extras import execute_values
from decimal import Decimal
from datetime import datetime, date
import os
import re

# Database connection
DB_CONFIG = {
    'host': 'localhost',
    'port': 5432,
    'database': 'carddemo',
    'user': 'carddemo',
    'password': 'carddemo123'
}

DATA_DIR = os.path.dirname(os.path.abspath(__file__))

def parse_cobol_signed_number(value, decimals=2):
    """Parse COBOL signed numeric field (trailing sign overpunch)"""
    if not value or value.strip() == '':
        return Decimal('0')

    value = value.strip()
    if not value:
        return Decimal('0')

    # COBOL sign overpunch mapping
    positive_signs = {'{': '0', 'A': '1', 'B': '2', 'C': '3', 'D': '4',
                      'E': '5', 'F': '6', 'G': '7', 'H': '8', 'I': '9'}
    negative_signs = {'}': '0', 'J': '1', 'K': '2', 'L': '3', 'M': '4',
                      'N': '5', 'O': '6', 'P': '7', 'Q': '8', 'R': '9'}

    last_char = value[-1]
    is_negative = False

    if last_char in positive_signs:
        value = value[:-1] + positive_signs[last_char]
    elif last_char in negative_signs:
        value = value[:-1] + negative_signs[last_char]
        is_negative = True

    try:
        num = Decimal(value) / Decimal(10 ** decimals)
        return -num if is_negative else num
    except:
        return Decimal('0')

def parse_date(date_str):
    """Parse date string YYYY-MM-DD"""
    if not date_str or date_str.strip() == '' or date_str.strip() == '0000-00-00':
        return None
    try:
        return datetime.strptime(date_str.strip(), '%Y-%m-%d').date()
    except:
        return None

def parse_datetime(dt_str):
    """Parse datetime string YYYY-MM-DD HH:MM:SS.ffffff"""
    if not dt_str or dt_str.strip() == '':
        return None
    try:
        # Handle format: 2022-06-10 19:27:53.000000
        return datetime.strptime(dt_str.strip(), '%Y-%m-%d %H:%M:%S.%f')
    except:
        try:
            return datetime.strptime(dt_str.strip()[:19], '%Y-%m-%d %H:%M:%S')
        except:
            return None

def load_customers(conn):
    """Load customers from custdata.txt"""
    print("Loading customers...")
    customers = []

    with open(os.path.join(DATA_DIR, 'custdata.txt'), 'r') as f:
        for line in f:
            if len(line.strip()) < 50:
                continue

            # Fixed positions based on COBOL copybook CVCUS01Y
            cust_id = int(line[0:9].strip())
            first_name = line[9:34].strip()
            middle_name = line[34:59].strip()
            last_name = line[59:84].strip()
            addr_line1 = line[84:134].strip()
            addr_line2 = line[134:184].strip()
            city = line[184:234].strip()
            state = line[234:236].strip()
            country = line[236:239].strip()
            zip_code = line[239:249].strip()
            phone1 = line[249:264].strip()
            phone2 = line[264:279].strip()
            ssn = line[279:288].strip()
            # Skip govt ID fields
            dob_str = line[297:307].strip()
            fico_str = line[307:310].strip()

            dob = parse_date(dob_str)
            # FICO score must be between 300-850
            fico = None
            if fico_str.isdigit():
                fico_val = int(fico_str)
                if 300 <= fico_val <= 850:
                    fico = fico_val
                elif fico_val > 850:
                    fico = 850  # Cap at max
                elif fico_val > 0:
                    fico = 300  # Floor at min

            customers.append((
                cust_id, first_name, last_name, dob, fico,
                addr_line1, addr_line2, city, state, zip_code,
                phone1, f"{first_name.lower()}.{last_name.lower()}@email.com"
            ))

    cursor = conn.cursor()
    # Clear dependent tables first (respecting FK constraints)
    cursor.execute("DELETE FROM bill_payments")
    cursor.execute("DELETE FROM bill_payees")
    cursor.execute("DELETE FROM pending_authorizations")
    cursor.execute("DELETE FROM transactions")
    cursor.execute("DELETE FROM credit_cards")
    cursor.execute("DELETE FROM accounts")
    cursor.execute("DELETE FROM customers WHERE customer_id NOT IN (SELECT customer_id FROM users WHERE customer_id IS NOT NULL)")

    insert_sql = """
        INSERT INTO customers (customer_id, first_name, last_name, date_of_birth, fico_credit_score,
                               address_line1, address_line2, city, state, zip_code, phone_number, email)
        VALUES %s
        ON CONFLICT (customer_id) DO UPDATE SET
            first_name = EXCLUDED.first_name,
            last_name = EXCLUDED.last_name,
            date_of_birth = EXCLUDED.date_of_birth,
            fico_credit_score = EXCLUDED.fico_credit_score,
            address_line1 = EXCLUDED.address_line1,
            address_line2 = EXCLUDED.address_line2,
            city = EXCLUDED.city,
            state = EXCLUDED.state,
            zip_code = EXCLUDED.zip_code,
            phone_number = EXCLUDED.phone_number,
            email = EXCLUDED.email,
            updated_at = CURRENT_TIMESTAMP
    """
    execute_values(cursor, insert_sql, customers)
    conn.commit()
    print(f"  Loaded {len(customers)} customers")
    return customers

def load_accounts(conn):
    """Load accounts from acctdata.txt"""
    print("Loading accounts...")
    accounts = []

    with open(os.path.join(DATA_DIR, 'acctdata.txt'), 'r') as f:
        for line in f:
            if len(line.strip()) < 90:
                continue

            # Fixed positions based on COBOL copybook CVACT01Y
            # Fields are: ACCT-ID(11), STATUS(1), BALANCE(12), CREDIT-LIM(12), CASH-LIM(12),
            #             OPEN-DATE(10), EXPIRY-DATE(10), REISSUE-DATE(10), CYC-CREDIT(12), CYC-DEBIT(12), GROUP(10)
            acct_id = int(line[0:11].strip())
            active_status = line[11:12].strip() or 'Y'
            # Amounts stored in cents (divide by 100)
            current_balance = parse_cobol_signed_number(line[12:24], decimals=2)
            credit_limit = parse_cobol_signed_number(line[24:36], decimals=2)
            cash_credit_limit = parse_cobol_signed_number(line[36:48], decimals=2)
            open_date = parse_date(line[48:58])
            expiry_date = parse_date(line[58:68])
            reissue_date = parse_date(line[68:78])
            curr_cycle_credit = parse_cobol_signed_number(line[78:90], decimals=2)
            curr_cycle_debit = parse_cobol_signed_number(line[90:102], decimals=2)
            group_id = line[102:112].strip() or None

            # Use customer_id = account_id for now (will be linked via cardxref)
            accounts.append((
                acct_id, acct_id % 100 + 1,  # Temporary customer mapping
                active_status, current_balance, credit_limit, cash_credit_limit,
                open_date or date(2020, 1, 1), expiry_date or date(2025, 12, 31), reissue_date,
                curr_cycle_credit, curr_cycle_debit, group_id
            ))

    return accounts

def load_cardxref(conn):
    """Load card cross-reference from cardxref.txt to map accounts to customers"""
    print("Loading card cross-reference...")
    xref = {}

    with open(os.path.join(DATA_DIR, 'cardxref.txt'), 'r') as f:
        for line in f:
            if len(line.strip()) < 30:
                continue

            # Fixed positions based on COBOL copybook
            card_num = line[0:16].strip()
            cust_id = int(line[16:25].strip())
            acct_id = int(line[25:36].strip())

            xref[acct_id] = cust_id

    print(f"  Loaded {len(xref)} cross-references")
    return xref

def insert_accounts(conn, accounts, xref):
    """Insert accounts with proper customer mapping"""
    print("Inserting accounts...")
    cursor = conn.cursor()

    # Get valid customer IDs
    cursor.execute("SELECT customer_id FROM customers")
    valid_customers = {row[0] for row in cursor.fetchall()}

    mapped_accounts = []
    for acct in accounts:
        acct_id = acct[0]
        # Use xref mapping, fallback to first valid customer
        cust_id = xref.get(acct_id)
        if cust_id not in valid_customers:
            cust_id = min(valid_customers) if valid_customers else 1

        mapped_accounts.append((
            acct_id, cust_id, acct[2], acct[3], acct[4], acct[5],
            acct[6], acct[7], acct[8], acct[9], acct[10], acct[11]
        ))

    insert_sql = """
        INSERT INTO accounts (account_id, customer_id, active_status, current_balance, credit_limit,
                              cash_credit_limit, open_date, expiry_date, reissue_date,
                              curr_cycle_credit, curr_cycle_debit, group_id)
        VALUES %s
        ON CONFLICT (account_id) DO UPDATE SET
            customer_id = EXCLUDED.customer_id,
            active_status = EXCLUDED.active_status,
            current_balance = EXCLUDED.current_balance,
            credit_limit = EXCLUDED.credit_limit,
            cash_credit_limit = EXCLUDED.cash_credit_limit,
            open_date = EXCLUDED.open_date,
            expiry_date = EXCLUDED.expiry_date,
            reissue_date = EXCLUDED.reissue_date,
            curr_cycle_credit = EXCLUDED.curr_cycle_credit,
            curr_cycle_debit = EXCLUDED.curr_cycle_debit,
            group_id = EXCLUDED.group_id,
            updated_at = CURRENT_TIMESTAMP
    """
    execute_values(cursor, insert_sql, mapped_accounts)
    conn.commit()
    print(f"  Inserted {len(mapped_accounts)} accounts")

def load_cards(conn):
    """Load credit cards from carddata.txt"""
    print("Loading credit cards...")
    cards = []

    # Get valid account IDs
    cursor = conn.cursor()
    cursor.execute("SELECT account_id FROM accounts")
    valid_accounts = {row[0] for row in cursor.fetchall()}

    with open(os.path.join(DATA_DIR, 'carddata.txt'), 'r') as f:
        for line in f:
            if len(line.strip()) < 50:
                continue

            # Fixed positions based on COBOL copybook CVACT02Y
            # CARD-NUM(16), CARD-ACCT-ID(11), CARD-CVV-CD(3), CARD-EMBOSSED-NAME(50), CARD-EXPIRATION-DATE(10), CARD-ACTIVE-STATUS(1)
            card_num = line[0:16].strip()
            acct_id = int(line[16:27].strip())
            # cvv = line[27:30].strip()  # 3-digit CVV (not stored)
            embossed_name = line[30:80].strip()
            expiry_date = parse_date(line[80:90])
            active_status = line[90:91].strip() if len(line) > 90 else 'Y'
            if active_status not in ('Y', 'N', 'S'):
                active_status = 'Y'

            # Skip if account doesn't exist
            if acct_id not in valid_accounts:
                continue

            # Determine card type from number prefix
            if card_num.startswith('4'):
                card_type = 'VC'  # Visa
            elif card_num.startswith('5'):
                card_type = 'MC'  # Mastercard
            elif card_num.startswith('34') or card_num.startswith('37'):
                card_type = 'AX'  # Amex
            elif card_num.startswith('6'):
                card_type = 'DC'  # Discover
            else:
                card_type = 'VC'  # Default to Visa

            cards.append((
                card_num, acct_id, card_type, embossed_name,
                expiry_date or date(2025, 12, 31), active_status,
                expiry_date or date(2020, 1, 1)  # issued_date approximation
            ))

    insert_sql = """
        INSERT INTO credit_cards (card_number, account_id, card_type, embossed_name,
                                  expiry_date, active_status, issued_date)
        VALUES %s
        ON CONFLICT (card_number) DO UPDATE SET
            account_id = EXCLUDED.account_id,
            card_type = EXCLUDED.card_type,
            embossed_name = EXCLUDED.embossed_name,
            expiry_date = EXCLUDED.expiry_date,
            active_status = EXCLUDED.active_status,
            updated_at = CURRENT_TIMESTAMP
    """
    execute_values(cursor, insert_sql, cards)
    conn.commit()
    print(f"  Loaded {len(cards)} credit cards")

def load_transactions(conn):
    """Load transactions from dailytran.txt"""
    print("Loading transactions...")
    transactions = []

    # Get valid account IDs and card numbers
    cursor = conn.cursor()
    cursor.execute("SELECT account_id FROM accounts")
    valid_accounts = {row[0] for row in cursor.fetchall()}
    cursor.execute("SELECT card_number, account_id FROM credit_cards")
    card_to_account = {row[0]: row[1] for row in cursor.fetchall()}

    # Transaction type mapping
    tran_types = {
        '01': 'SALE', '02': 'PYMT', '03': 'CRDT',
        '04': 'AUTH', '05': 'RFND', '06': 'RVRSL', '07': 'ADJ'
    }

    with open(os.path.join(DATA_DIR, 'dailytran.txt'), 'r') as f:
        for line in f:
            if len(line.strip()) < 100:
                continue

            # Fixed positions based on COBOL copybook CVTRA05Y
            tran_id = line[0:16].strip()
            tran_type_code = line[16:18].strip()
            tran_cat_code = line[18:22].strip()
            tran_source = line[22:32].strip()
            tran_desc = line[32:132].strip()
            tran_amt = parse_cobol_signed_number(line[132:143])
            merchant_id = line[143:152].strip()
            merchant_name = line[152:202].strip()
            merchant_city = line[202:252].strip()
            merchant_zip = line[252:262].strip()
            card_num = line[262:278].strip()
            tran_datetime = parse_datetime(line[278:304])

            # Get account from card
            acct_id = card_to_account.get(card_num)
            if acct_id not in valid_accounts:
                # Try to find any valid account
                if valid_accounts:
                    acct_id = min(valid_accounts)
                else:
                    continue

            tran_type = tran_types.get(tran_type_code, 'SALE')
            tran_date = tran_datetime.date() if tran_datetime else date.today()
            tran_time = tran_datetime.time() if tran_datetime else datetime.now().time()

            transactions.append((
                acct_id, tran_type, tran_cat_code, tran_source, tran_desc,
                tran_amt, merchant_id or None, merchant_name or None,
                merchant_city or None, merchant_zip or None, card_num or None,
                tran_date, tran_time
            ))

    if transactions:
        insert_sql = """
            INSERT INTO transactions (account_id, transaction_type, transaction_category, transaction_source,
                                      transaction_desc, transaction_amount, merchant_id, merchant_name,
                                      merchant_city, merchant_zip, card_number, transaction_date, transaction_time)
            VALUES %s
        """
        execute_values(cursor, insert_sql, transactions)
        conn.commit()

    print(f"  Loaded {len(transactions)} transactions")

def update_users_customer_mapping(conn):
    """Update user-customer mapping based on imported data"""
    print("Updating user-customer mappings...")
    cursor = conn.cursor()

    # Get first few customers for user mapping
    cursor.execute("SELECT customer_id FROM customers ORDER BY customer_id LIMIT 10")
    customer_ids = [row[0] for row in cursor.fetchall()]

    if customer_ids:
        # Update existing test users with real customer IDs
        cursor.execute("""
            UPDATE users SET customer_id = %s WHERE user_id = 'USER0001' AND user_type = 'U'
        """, (customer_ids[0],))

        if len(customer_ids) > 1:
            cursor.execute("""
                UPDATE users SET customer_id = %s WHERE user_id = 'USER0002' AND user_type = 'U'
            """, (customer_ids[1],))

        if len(customer_ids) > 2:
            cursor.execute("""
                UPDATE users SET customer_id = %s WHERE user_id = 'USER0003' AND user_type = 'U'
            """, (customer_ids[2],))

        if len(customer_ids) > 3:
            cursor.execute("""
                UPDATE users SET customer_id = %s WHERE user_id = 'USER0004' AND user_type = 'U'
            """, (customer_ids[3],))

        if len(customer_ids) > 4:
            cursor.execute("""
                UPDATE users SET customer_id = %s WHERE user_id = 'USER0005' AND user_type = 'U'
            """, (customer_ids[4],))

    conn.commit()
    print("  User mappings updated")

def main():
    print("=" * 60)
    print("CardDemo Data Import")
    print("=" * 60)
    print(f"Data directory: {DATA_DIR}")
    print()

    # Connect to database
    print("Connecting to PostgreSQL...")
    conn = psycopg2.connect(**DB_CONFIG)

    try:
        # Load data in order (respecting foreign keys)
        customers = load_customers(conn)
        xref = load_cardxref(conn)
        accounts = load_accounts(conn)
        insert_accounts(conn, accounts, xref)
        load_cards(conn)
        load_transactions(conn)
        update_users_customer_mapping(conn)

        # Print summary
        print()
        print("=" * 60)
        print("Import Summary")
        print("=" * 60)
        cursor = conn.cursor()
        cursor.execute("SELECT COUNT(*) FROM customers")
        print(f"Customers: {cursor.fetchone()[0]}")
        cursor.execute("SELECT COUNT(*) FROM accounts")
        print(f"Accounts: {cursor.fetchone()[0]}")
        cursor.execute("SELECT COUNT(*) FROM credit_cards")
        print(f"Credit Cards: {cursor.fetchone()[0]}")
        cursor.execute("SELECT COUNT(*) FROM transactions")
        print(f"Transactions: {cursor.fetchone()[0]}")
        print()
        print("Data import completed successfully!")

    except Exception as e:
        print(f"Error: {e}")
        conn.rollback()
        raise
    finally:
        conn.close()

if __name__ == '__main__':
    main()
