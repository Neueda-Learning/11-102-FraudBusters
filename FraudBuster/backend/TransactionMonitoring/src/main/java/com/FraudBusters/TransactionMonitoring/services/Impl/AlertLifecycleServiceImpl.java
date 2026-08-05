package com.FraudBusters.TransactionMonitoring.services.Impl;

import com.FraudBusters.TransactionMonitoring.exceptions.ResourceNotFoundException;
import com.FraudBusters.TransactionMonitoring.models.AlertEntity;
import com.FraudBusters.TransactionMonitoring.models.AlertStatusHistoryEntity;
import com.FraudBusters.TransactionMonitoring.models.AlertTransactionEntity;
import com.FraudBusters.TransactionMonitoring.models.TransactionDecisionEntity;
import com.FraudBusters.TransactionMonitoring.models.TransactionEntity;
import com.FraudBusters.TransactionMonitoring.models.enums.AlertStatus;
import com.FraudBusters.TransactionMonitoring.models.enums.DecisionType;
import com.FraudBusters.TransactionMonitoring.models.enums.FinalDecision;
import com.FraudBusters.TransactionMonitoring.models.enums.MonitorState;
import com.FraudBusters.TransactionMonitoring.repository.AlertEntityRepository;
import com.FraudBusters.TransactionMonitoring.repository.AlertStatusHistoryEntityRepo;
import com.FraudBusters.TransactionMonitoring.repository.AlertTransactionEntityRepo;
import com.FraudBusters.TransactionMonitoring.repository.TransactionDecisionEntityRepo;
import com.FraudBusters.TransactionMonitoring.repository.TransactionEntityRepository;
import com.FraudBusters.TransactionMonitoring.services.AlertLifecycleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AlertLifecycleServiceImpl implements AlertLifecycleService {

    @Autowired
    private AlertEntityRepository alertEntityRepository;

    @Autowired
    private AlertStatusHistoryEntityRepo alertStatusHistoryRepository;

    @Autowired
    private AlertTransactionEntityRepo alertTransactionRepository;

    @Autowired
    private TransactionDecisionEntityRepo transactionDecisionRepository;

    @Autowired
    private TransactionEntityRepository transactionRepository;

    // ---------------------------------------------------------------
    //  ACKNOWLEDGE  (OPEN -> ACKNOWLEDGED)
    // ---------------------------------------------------------------

    /**
     * Operator acknowledges the alert — marks that they have seen it.
     * Only valid when alert is currently OPEN.
     *
     * Writes:
     *   - alerts.status = ACKNOWLEDGED
     *   - alert_status_history: OPEN -> ACKNOWLEDGED
     */
    @Override
    @Transactional
    public void acknowledgeAlert(String alertCode) {
        // Step 1: fetch alert by business code
        AlertEntity alert = findAlertOrThrow(alertCode);

        // Step 2: validate that current status is OPEN (only OPEN can be acknowledged)
        validateStrictTransition(alert, AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED);

        // Step 3: capture old status before changing
        AlertStatus oldStatus = alert.getStatus();

        // Step 4: update alert status
        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alertEntityRepository.save(alert);

        // Step 5: record status change in history
        insertStatusHistory(
                alert,
                oldStatus,
                AlertStatus.ACKNOWLEDGED,
                "operator-1",
                "Operator acknowledged the alert."
        );
    }

    // ---------------------------------------------------------------
    //  INVESTIGATE  (ACKNOWLEDGED -> INVESTIGATING)
    // ---------------------------------------------------------------

    /**
     * Operator starts active investigation on the alert.
     * Only valid when alert is currently ACKNOWLEDGED.
     *
     * Writes:
     *   - alerts.status = INVESTIGATING
     *   - alert_status_history: ACKNOWLEDGED -> INVESTIGATING
     */
    @Override
    @Transactional
    public void investigateAlert(String alertCode) {
        // Step 1: fetch alert by business code
        AlertEntity alert = findAlertOrThrow(alertCode);

        // Step 2: validate that current status is ACKNOWLEDGED
        validateStrictTransition(alert, AlertStatus.ACKNOWLEDGED, AlertStatus.INVESTIGATING);

        // Step 3: capture old status before changing
        AlertStatus oldStatus = alert.getStatus();

        // Step 4: update alert status
        alert.setStatus(AlertStatus.INVESTIGATING);
        alertEntityRepository.save(alert);

        // Step 5: record status change in history
        insertStatusHistory(
                alert,
                oldStatus,
                AlertStatus.INVESTIGATING,
                "operator-1",
                "Operator started investigation."
        );
    }

    // ---------------------------------------------------------------
    //  CLOSE  (INVESTIGATING -> CLOSED)
    //  Terminal action: fraud confirmed -> DECLINE the transaction
    // ---------------------------------------------------------------

    /**
     * Operator closes the alert after confirming fraud.
     * Only valid when alert is currently INVESTIGATING.
     *
     * Writes:
     *   - alerts.status = CLOSED, alerts.closed_at, alerts.closure_reason
     *   - alert_status_history: INVESTIGATING -> CLOSED
     *   - transaction_decisions: DECLINE row for each linked transaction
     *   - transactions: monitor_state = DECLINED, final_decision = DECLINE (mirror)
     *
     * @param reason    mandatory — reason shown in UI and audit logs
     * @param decidedBy who made the decision (e.g. "operator-1")
     */
    @Override
    @Transactional
    public void closeAlert(String alertCode, String reason, String decidedBy) {
        // Step 1: fetch alert
        AlertEntity alert = findAlertOrThrow(alertCode);

        // Step 2: validate only INVESTIGATING can be closed
        validateStrictTransition(alert, AlertStatus.INVESTIGATING, AlertStatus.CLOSED);

        // Step 3: capture old status
        AlertStatus oldStatus = alert.getStatus();

        // Step 4: update alert — set CLOSED state and save closure metadata
        alert.setStatus(AlertStatus.CLOSED);
        alert.setClosedAt(LocalDateTime.now());
        alert.setClosureReason(reason);
        alertEntityRepository.save(alert);

        // Step 5: record status transition in history
        insertStatusHistory(alert, oldStatus, AlertStatus.CLOSED, decidedBy, reason);

        // Step 6: write DECLINE decision for each transaction linked to this alert
        //         and mirror the final result onto the transactions table
        recordDecisionForLinkedTransactions(alert, DecisionType.DECLINE, decidedBy, reason);
    }

    // ---------------------------------------------------------------
    //  DISMISS  (OPEN / ACKNOWLEDGED / INVESTIGATING -> DISMISSED)
    //  Terminal action: false positive -> ALLOW the transaction
    // ---------------------------------------------------------------

    /**
     * Operator dismisses the alert as a false positive.
     * Valid from any active status: OPEN, ACKNOWLEDGED, or INVESTIGATING.
     * Not allowed if alert is already CLOSED or DISMISSED.
     *
     * Writes:
     *   - alerts.status = DISMISSED, alerts.closed_at, alerts.closure_reason
     *   - alert_status_history: (current) -> DISMISSED
     *   - transaction_decisions: ALLOW row for each linked transaction
     *   - transactions: monitor_state = RELEASED, final_decision = ALLOW (mirror)
     *
     * @param reason    mandatory — reason shown in UI and audit logs
     * @param decidedBy who dismissed it (e.g. "operator-1")
     */
    @Override
    @Transactional
    public void dismissAlert(String alertCode, String reason, String decidedBy) {
        // Step 1: fetch alert
        AlertEntity alert = findAlertOrThrow(alertCode);

        // Step 2: validate dismiss is allowed from current status
        validateDismissTransition(alert);

        // Step 3: capture old status
        AlertStatus oldStatus = alert.getStatus();

        // Step 4: update alert — set DISMISSED state and save closure metadata
        alert.setStatus(AlertStatus.DISMISSED);
        alert.setClosedAt(LocalDateTime.now());
        alert.setClosureReason(reason);
        alertEntityRepository.save(alert);

        // Step 5: record status transition in history
        insertStatusHistory(alert, oldStatus, AlertStatus.DISMISSED, decidedBy, reason);

        // Step 6: write ALLOW decision for each transaction linked to this alert
        //         and mirror the final result onto the transactions table
        recordDecisionForLinkedTransactions(alert, DecisionType.ALLOW, decidedBy, reason);
    }

    // ---------------------------------------------------------------
    //  PRIVATE HELPERS
    // ---------------------------------------------------------------

    /**
     * Fetches an alert by alertCode. Throws 404 if not found.
     */
    private AlertEntity findAlertOrThrow(String alertCode) {
        return alertEntityRepository.findByAlertCode(alertCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Alert not found with alertCode: " + alertCode));
    }

    /**
     * Validates a strict one-step transition.
     * If alert is not in expectedCurrent status, throws IllegalStateException.
     *
     * Example: acknowledge requires OPEN. If alert is INVESTIGATING, this throws.
     */
    private void validateStrictTransition(AlertEntity alert,
                                          AlertStatus expectedCurrent,
                                          AlertStatus target) {
        if (alert.getStatus() != expectedCurrent) {
            throw new IllegalStateException(
                    "Cannot move alert to " + target
                    + ". Current status is " + alert.getStatus()
                    + ", but expected " + expectedCurrent + ".");
        }
    }

    /**
     * Validates that dismiss is allowed.
     * Dismiss is valid from OPEN, ACKNOWLEDGED, or INVESTIGATING.
     * Dismiss is NOT valid if alert is already CLOSED or DISMISSED.
     */
    private void validateDismissTransition(AlertEntity alert) {
        AlertStatus current = alert.getStatus();
        if (current == AlertStatus.CLOSED || current == AlertStatus.DISMISSED) {
            throw new IllegalStateException(
                    "Cannot dismiss alert. It is already in terminal status: " + current);
        }
    }

    /**
     * Inserts one row into alert_status_history.
     * Called on every status transition.
     *
     * @param alert      the alert that changed status
     * @param oldStatus  previous status (NULL only on initial OPEN creation)
     * @param newStatus  new status after transition
     * @param changedBy  "operator-1" or "SYSTEM"
     * @param reason     human-readable reason for the change
     */
    private void insertStatusHistory(AlertEntity alert,
                                     AlertStatus oldStatus,
                                     AlertStatus newStatus,
                                     String changedBy,
                                     String reason) {
        AlertStatusHistoryEntity history = new AlertStatusHistoryEntity();
        history.setAlert(alert);
        history.setOldStatus(oldStatus);    // NULL for initial OPEN — but that's handled in rule engine
        history.setNewStatus(newStatus);
        history.setChangedBy(changedBy);
        history.setChangeReason(reason);
        // changedAt is DB-managed (DEFAULT CURRENT_TIMESTAMP), no manual set needed
        alertStatusHistoryRepository.save(history);
    }

    /**
     * For close() and dismiss() only:
     * Finds all transactions linked to this alert via alert_transactions,
     * inserts a transaction_decisions row for each one,
     * and mirrors the final decision onto the transactions table.
     *
     * DECLINE -> monitor_state = DECLINED, final_decision = DECLINE
     * ALLOW   -> monitor_state = RELEASED, final_decision = ALLOW
     */
    private void recordDecisionForLinkedTransactions(AlertEntity alert,
                                                     DecisionType decisionType,
                                                     String decidedBy,
                                                     String reason) {
        // Get all transactions linked to this alert
        List<AlertTransactionEntity> links = alertTransactionRepository.findByAlert(alert);

        for (AlertTransactionEntity link : links) {
            TransactionEntity txn = link.getTransaction();

            // --- Insert transaction_decisions audit row ---
            TransactionDecisionEntity decisionRecord = new TransactionDecisionEntity();
            decisionRecord.setTransaction(txn);
            decisionRecord.setAlert(alert);
            decisionRecord.setDecision(decisionType);
            decisionRecord.setDecidedBy(decidedBy);
            decisionRecord.setDecisionReason(reason);
            // decided_at is DB-managed (DEFAULT CURRENT_TIMESTAMP)
            transactionDecisionRepository.save(decisionRecord);

            // --- Mirror final decision onto transactions ---
            if (decisionType == DecisionType.DECLINE) {
                // Fraud confirmed: hold payment, mark as declined
                txn.setMonitorState(MonitorState.DECLINED);
                txn.setFinalDecision(FinalDecision.DECLINE);
            } else {
                // False positive: release payment, mark as allowed
                txn.setMonitorState(MonitorState.RELEASED);
                txn.setFinalDecision(FinalDecision.ALLOW);
            }
            txn.setDecisionReason(reason);
            txn.setDecidedAt(LocalDateTime.now());
            transactionRepository.save(txn);
        }
    }
}

