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
        'Trigger alert when N transactions occur within T time period from the same account. Example: Alert if more than 5 transactions occur within 10 minutes.',
        'VELOCITY',
        'HIGH',
        'INLINE',
        '{"maxTransactions": 5, "windowMinutes": 10}',
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

