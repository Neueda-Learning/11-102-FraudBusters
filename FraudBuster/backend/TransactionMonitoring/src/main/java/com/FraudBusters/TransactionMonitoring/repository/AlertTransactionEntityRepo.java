package com.FraudBusters.TransactionMonitoring.repository;

import com.FraudBusters.TransactionMonitoring.models.AlertEntity;
import com.FraudBusters.TransactionMonitoring.models.AlertTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import com.FraudBusters.TransactionMonitoring.models.enums.AlertRelationType;
import java.util.List;
import java.util.Optional;

public interface AlertTransactionEntityRepo extends JpaRepository<AlertTransactionEntity, Long> {

    /**
     * Returns all alert-transaction links for a given alert.
     * Used by close/dismiss to find which transactions need a final decision recorded.
     */
    List<AlertTransactionEntity> findByAlert(AlertEntity alert);

    /** Returns the oldest triggering link for an alert (primary source for dashboard accountId). */
    Optional<AlertTransactionEntity> findFirstByAlertAndRelationTypeOrderByCreatedAtAsc(
            AlertEntity alert,
            AlertRelationType relationType);

    /** Fallback when no triggering link exists for an alert. */
    Optional<AlertTransactionEntity> findFirstByAlertOrderByCreatedAtAsc(AlertEntity alert);
}