package com.FraudBusters.TransactionMonitoring.services;

/**
 * AlertLifecycleService defines the operator workflow for managing alert states.
 *
 * Allowed transitions:
 *   OPEN -> ACKNOWLEDGED         (operator saw the alert)
 *   ACKNOWLEDGED -> INVESTIGATING (operator started reviewing)
 *   INVESTIGATING -> CLOSED       (fraud confirmed -> transaction DECLINED)
 *   OPEN / ACKNOWLEDGED / INVESTIGATING -> DISMISSED  (false positive -> transaction ALLOWED)
 *
 * close() and dismiss() are terminal actions — they also write
 * to transaction_decisions and mirror the final result onto transactions.
 */
public interface AlertLifecycleService {

    /**
     * Operator acknowledges the alert.
     * Transition: OPEN -> ACKNOWLEDGED
     *
     * @param alertCode business-facing alert identifier (e.g. "AMT-1234567890")
     */
    void acknowledgeAlert(String alertCode);

    /**
     * Operator starts active investigation on the alert.
     * Transition: ACKNOWLEDGED -> INVESTIGATING
     *
     * @param alertCode business-facing alert identifier
     */
    void investigateAlert(String alertCode);

    /**
     * Operator closes the alert after confirming fraud.
     * Transition: INVESTIGATING -> CLOSED
     *
     * Also writes:
     *   - alert_status_history row (INVESTIGATING -> CLOSED)
     *   - transaction_decisions row (DECLINE)
     *   - mirrors DECLINED + DECLINE onto transactions
     *
     * @param alertCode   business-facing alert identifier
     * @param reason      mandatory reason for closure (e.g. "Fraud confirmed after review")
     * @param decidedBy   who closed it (e.g. "operator-1" or "SYSTEM")
     */
    void closeAlert(String alertCode, String reason, String decidedBy);

    /**
     * Operator dismisses the alert as a false positive.
     * Transition: OPEN / ACKNOWLEDGED / INVESTIGATING -> DISMISSED
     *
     * Also writes:
     *   - alert_status_history row (current -> DISMISSED)
     *   - transaction_decisions row (ALLOW)
     *   - mirrors RELEASED + ALLOW onto transactions
     *
     * @param alertCode   business-facing alert identifier
     * @param reason      mandatory reason for dismissal (e.g. "Customer confirmed transaction")
     * @param decidedBy   who dismissed it (e.g. "operator-1")
     */
    void dismissAlert(String alertCode, String reason, String decidedBy);
}

