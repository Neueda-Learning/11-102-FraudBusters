package com.FraudBusters.TransactionMonitoring.repository;

import com.FraudBusters.TransactionMonitoring.models.AlertTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertTransactionEntityRepo extends JpaRepository<AlertTransactionEntity, Long> {
}