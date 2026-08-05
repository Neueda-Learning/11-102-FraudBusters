package com.FraudBusters.TransactionMonitoring.repository;

import com.FraudBusters.TransactionMonitoring.models.AlertEntity;
import com.FraudBusters.TransactionMonitoring.models.AlertTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertTransactionEntityRepo extends JpaRepository<AlertTransactionEntity, Long> {

    /**
     * Returns all alert-transaction links for a given alert.
     * Used by close/dismiss to find which transactions need a final decision recorded.
     */
    List<AlertTransactionEntity> findByAlert(AlertEntity alert);
}