package com.FraudBusters.TransactionMonitoring.repository;

import com.FraudBusters.TransactionMonitoring.models.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionEntityRepository extends JpaRepository<TransactionEntity, Long> {

    /** Look up a transaction by its unique business ID */
    Optional<TransactionEntity> findByTxnId(String txnId);

    /**
     * Fetch all DEBIT transactions for an account within a time window.
     * Used by the DAILY_LIMIT rule to sum up the day's spend.
     */
    @Query("SELECT t FROM TransactionEntity t " +
           "WHERE t.accountId = :accountId " +
           "AND t.txnType = TransactionType.DEBIT " +
           "AND t.txnTimestamp >= :startOfDay " +
           "AND t.txnTimestamp <= :endOfDay")
    List<TransactionEntity> findDebitTransactionsByAccountAndDay(
            @Param("accountId") String accountId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay);
}
