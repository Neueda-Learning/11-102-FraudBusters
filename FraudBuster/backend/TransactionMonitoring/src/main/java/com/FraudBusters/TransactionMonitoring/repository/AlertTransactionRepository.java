package com.FraudBusters.TransactionMonitoring.repository;

import com.FraudBusters.TransactionMonitoring.models.AlertTransactionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlertTransactionRepository extends JpaRepository<AlertTransactionEntity, Long> {

    @Query("select at from AlertTransactionEntity at join fetch at.transaction where at.alert.id in :alertIds order by at.id asc")
    List<AlertTransactionEntity> findByAlertIdsWithTransaction(@Param("alertIds") List<Long> alertIds);

    @Query("select at from AlertTransactionEntity at join fetch at.alert a join fetch a.rule where at.transaction.id in :transactionIds order by at.id asc")
    List<AlertTransactionEntity> findByTransactionIdsWithAlertAndRule(@Param("transactionIds") List<Long> transactionIds);

    @Query("select count(distinct at.transaction.id) from AlertTransactionEntity at")
    long countDistinctTransactionsWithAlerts();
}

