-- ============================================================
--  Seed Data: Rules
--  Uses INSERT IGNORE so rows are inserted ONLY ONCE.
--  If rule_code already exists (UNIQUE KEY), the row is skipped.
-- ============================================================

INSERT IGNORE INTO rules (rule_code, name, description, rule_type, severity_default, inline_mode, config_json, is_active, is_deleted)
VALUES
    (
        'AMOUNT_THRESHOLD',
        'Amount Threshold Rule',
        'Trigger alert when a single transaction exceeds a threshold amount. Example: Alert if any transaction > $10,000.',
        'AMOUNT',
        'HIGH',
        'INLINE',
        '{"thresholdAmount": 10000, "currency": "USD"}',
        true,
        false
    ),
    (
        'VELOCITY_CHECK',
        'Velocity Rule',
        'Trigger alert when N transactions occur within T time period from the same account. Example: Alert if more than 5 transactions occur within 6 hours.',
        'VELOCITY',
        'HIGH',
        'INLINE',
        '{"maxTransactions": 5, "windowHours": 6}',
        true,
        false
    ),
    (
        'NEW_PAYEE',
        'New Payee Rule',
        'Trigger alert when a transaction is made to a previously unseen payee from an account. Example: Alert on first transaction to any new payee.',
        'PATTERN',
        'MEDIUM',
        'POST_AUTH',
        '{"lookbackDays": 90}',
        true,
        false
    ),
    (
        'DAILY_LIMIT',
        'Daily Limit Rule',
        'Trigger alert when cumulative transaction amount from an account exceeds the daily limit. Example: Alert if total exceeds $50,000 in one day.',
        'AMOUNT',
        'CRITICAL',
        'INLINE',
        '{"dailyLimitAmount": 50000, "currency": "USD"}',
        true,
        false
    );

-- ============================================================
--  Seed Data: Dummy Transactions for Rule Testing
--  These rows are designed to trigger the seeded rules in a
--  deterministic way for local MVP testing.
-- ============================================================

INSERT IGNORE INTO transactions (
    txn_id,
    account_id,
    customer_full_name,
    customer_email,
    customer_phone,
    payee_id,
    amount,
    currency,
    txn_type,
    txn_timestamp,
    monitor_state,
    hold_started_at,
    hold_expires_at,
    final_decision,
    decision_reason,
    decided_at
)
VALUES
    -- AMOUNT_THRESHOLD: single transaction above 10,000
    (
        'TXN-AMT-001',
        'acct_amt_001',
        'Aman Verma',
        'aman.verma@example.com',
        '+919900000101',
        'payee_high_001',
        12500.00,
        'USD',
        'DEBIT',
        '2025-01-15 09:00:00.000',
        'RECEIVED',
        NULL,
        NULL,
        'PENDING',
        NULL,
        NULL
    ),

    -- VELOCITY_CHECK: 6 transactions within 10 minutes for same account
    (
        'TXN-VEL-001',
        'acct_vel_001',
        'Riya Shah',
        'riya.shah@example.com',
        '+919900000201',
        'payee_vel_001',
        150.00,
        'USD',
        'DEBIT',
        '2025-01-15 10:00:00.000',
        'RECEIVED',
        NULL,
        NULL,
        'PENDING',
        NULL,
        NULL
    ),
    (
        'TXN-VEL-002',
        'acct_vel_001',
        'Riya Shah',
        'riya.shah@example.com',
        '+919900000201',
        'payee_vel_001',
        175.00,
        'USD',
        'DEBIT',
        '2025-01-15 10:02:00.000',
        'RECEIVED',
        NULL,
        NULL,
        'PENDING',
        NULL,
        NULL
    ),
    (
        'TXN-VEL-003',
        'acct_vel_001',
        'Riya Shah',
        'riya.shah@example.com',
        '+919900000201',
        'payee_vel_001',
        200.00,
        'USD',
        'DEBIT',
        '2025-01-15 10:04:00.000',
        'RECEIVED',
        NULL,
        NULL,
        'PENDING',
        NULL,
        NULL
    ),
    (
        'TXN-VEL-004',
        'acct_vel_001',
        'Riya Shah',
        'riya.shah@example.com',
        '+919900000201',
        'payee_vel_001',
        225.00,
        'USD',
        'DEBIT',
        '2025-01-15 10:06:00.000',
        'RECEIVED',
        NULL,
        NULL,
        'PENDING',
        NULL,
        NULL
    ),
    (
        'TXN-VEL-005',
        'acct_vel_001',
        'Riya Shah',
        'riya.shah@example.com',
        '+919900000201',
        'payee_vel_001',
        250.00,
        'USD',
        'DEBIT',
        '2025-01-15 10:08:00.000',
        'RECEIVED',
        NULL,
        NULL,
        'PENDING',
        NULL,
        NULL
    ),
    (
        'TXN-VEL-006',
        'acct_vel_001',
        'Riya Shah',
        'riya.shah@example.com',
        '+919900000201',
        'payee_vel_001',
        275.00,
        'USD',
        'DEBIT',
        '2025-01-15 10:09:00.000',
        'RECEIVED',
        NULL,
        NULL,
        'PENDING',
        NULL,
        NULL
    ),

    -- NEW_PAYEE: first transaction establishes known payee, second introduces a new one
    (
        'TXN-NP-001',
        'acct_np_001',
        'Neha Kapoor',
        'neha.kapoor@example.com',
        '+919900000301',
        'payee_known_001',
        500.00,
        'USD',
        'DEBIT',
        '2025-01-15 11:00:00.000',
        'RECEIVED',
        NULL,
        NULL,
        'PENDING',
        NULL,
        NULL
    ),
    (
        'TXN-NP-002',
        'acct_np_001',
        'Neha Kapoor',
        'neha.kapoor@example.com',
        '+919900000301',
        'payee_new_001',
        750.00,
        'USD',
        'DEBIT',
        '2025-01-15 12:00:00.000',
        'RECEIVED',
        NULL,
        NULL,
        'PENDING',
        NULL,
        NULL
    ),

    -- DAILY_LIMIT: total crosses 50,000 across the same day, but each txn is spaced out
    (
        'TXN-DL-001',
        'acct_dl_001',
        'Kabir Mehta',
        'kabir.mehta@example.com',
        '+919900000401',
        'payee_dl_001',
        9000.00,
        'USD',
        'DEBIT',
        '2025-01-15 09:00:00.000',
        'RECEIVED',
        NULL,
        NULL,
        'PENDING',
        NULL,
        NULL
    ),
    (
        'TXN-DL-002',
        'acct_dl_001',
        'Kabir Mehta',
        'kabir.mehta@example.com',
        '+919900000401',
        'payee_dl_001',
        9000.00,
        'USD',
        'DEBIT',
        '2025-01-15 12:00:00.000',
        'RECEIVED',
        NULL,
        NULL,
        'PENDING',
        NULL,
        NULL
    ),
    (
        'TXN-DL-003',
        'acct_dl_001',
        'Kabir Mehta',
        'kabir.mehta@example.com',
        '+919900000401',
        'payee_dl_001',
        9000.00,
        'USD',
        'DEBIT',
        '2025-01-15 15:00:00.000',
        'RECEIVED',
        NULL,
        NULL,
        'PENDING',
        NULL,
        NULL
    ),
    (
        'TXN-DL-004',
        'acct_dl_001',
        'Kabir Mehta',
        'kabir.mehta@example.com',
        '+919900000401',
        'payee_dl_001',
        9000.00,
        'USD',
        'DEBIT',
        '2025-01-15 18:00:00.000',
        'RECEIVED',
        NULL,
        NULL,
        'PENDING',
        NULL,
        NULL
    ),
    (
        'TXN-DL-005',
        'acct_dl_001',
        'Kabir Mehta',
        'kabir.mehta@example.com',
        '+919900000401',
        'payee_dl_001',
        9000.00,
        'USD',
        'DEBIT',
        '2025-01-15 20:00:00.000',
        'RECEIVED',
        NULL,
        NULL,
        'PENDING',
        NULL,
        NULL
    ),
    (
        'TXN-DL-006',
        'acct_dl_001',
        'Kabir Mehta',
        'kabir.mehta@example.com',
        '+919900000401',
        'payee_dl_001',
        9000.00,
        'USD',
        'DEBIT',
        '2025-01-15 22:00:00.000',
        'RECEIVED',
        NULL,
        NULL,
        'PENDING',
        NULL,
        NULL
    );

-- ============================================================
--  Seed Data: Transaction Decisions
--  Stores final outcomes used by daily-limit aggregation logic
--  (only ALLOW decisions should contribute to spend totals).
-- ============================================================

INSERT IGNORE INTO transaction_decisions (
    transaction_id,
    alert_id,
    decision,
    decided_by,
    decision_reason
)
VALUES
    (
        (SELECT id FROM transactions WHERE txn_id = 'TXN-DL-001'),
        NULL,
        'ALLOW',
        'SYSTEM',
        'Seeded successful decision for daily limit test'
    ),
    (
        (SELECT id FROM transactions WHERE txn_id = 'TXN-DL-002'),
        NULL,
        'ALLOW',
        'SYSTEM',
        'Seeded successful decision for daily limit test'
    ),
    (
        (SELECT id FROM transactions WHERE txn_id = 'TXN-DL-003'),
        NULL,
        'ALLOW',
        'SYSTEM',
        'Seeded successful decision for daily limit test'
    ),
    (
        (SELECT id FROM transactions WHERE txn_id = 'TXN-DL-004'),
        NULL,
        'ALLOW',
        'SYSTEM',
        'Seeded successful decision for daily limit test'
    ),
    (
        (SELECT id FROM transactions WHERE txn_id = 'TXN-DL-005'),
        NULL,
        'ALLOW',
        'SYSTEM',
        'Seeded successful decision for daily limit test'
    ),
    (
        (SELECT id FROM transactions WHERE txn_id = 'TXN-DL-006'),
        NULL,
        'DECLINE',
        'SYSTEM',
        'Seeded declined decision for daily limit test'
    );

-- ============================================================
--  Seed Data: Today Test Records for Daily Limit API
--  These rows are dated for the current test day so the API
--  can be verified immediately with today-based aggregation.
--  Prior ALLOW total = 49,000 for acct_dl_today_001.
-- ============================================================

INSERT IGNORE INTO transactions (
    txn_id,
    account_id,
    customer_full_name,
    customer_email,
    customer_phone,
    payee_id,
    amount,
    currency,
    txn_type,
    txn_timestamp,
    monitor_state,
    hold_started_at,
    hold_expires_at,
    final_decision,
    decision_reason,
    decided_at
)
VALUES
    (
        'TXN-DL-TODAY-001',
        'acct_dl_today_001',
        'Kabir Mehta',
        'kabir.mehta@example.com',
        '+919900000401',
        'payee_dl_001',
        24000.00,
        'USD',
        'DEBIT',
        '2026-08-05 09:00:00.000',
        'RELEASED',
        NULL,
        NULL,
        'ALLOW',
        'Seeded allowed transaction for today test',
        '2026-08-05 09:00:00.000'
    ),
    (
        'TXN-DL-TODAY-002',
        'acct_dl_today_001',
        'Kabir Mehta',
        'kabir.mehta@example.com',
        '+919900000401',
        'payee_dl_001',
        25000.00,
        'USD',
        'DEBIT',
        '2026-08-05 10:00:00.000',
        'RELEASED',
        NULL,
        NULL,
        'ALLOW',
        'Seeded allowed transaction for today test',
        '2026-08-05 10:00:00.000'
    );

INSERT IGNORE INTO transaction_decisions (
    transaction_id,
    alert_id,
    decision,
    decided_by,
    decision_reason
)
VALUES
    (
        (SELECT id FROM transactions WHERE txn_id = 'TXN-DL-TODAY-001'),
        NULL,
        'ALLOW',
        'SYSTEM',
        'Seeded successful decision for today test'
    ),
    (
        (SELECT id FROM transactions WHERE txn_id = 'TXN-DL-TODAY-002'),
        NULL,
        'ALLOW',
        'SYSTEM',
        'Seeded successful decision for today test'
    );

