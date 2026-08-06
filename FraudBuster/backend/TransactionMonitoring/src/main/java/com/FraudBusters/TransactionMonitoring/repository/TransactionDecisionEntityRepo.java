package com.FraudBusters.TransactionMonitoring.repository;

import com.FraudBusters.TransactionMonitoring.models.TransactionEntity;
import com.FraudBusters.TransactionMonitoring.models.TransactionDecisionEntity;
import com.FraudBusters.TransactionMonitoring.models.enums.DecisionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionDecisionEntityRepo extends JpaRepository<TransactionDecisionEntity, Long> {

	boolean existsByTransactionTxnId(String txnId);

	@Query("SELECT COALESCE(SUM(td.transaction.amount), 0) FROM TransactionDecisionEntity td " +
		   "WHERE td.decision = :decision " +
		   "AND td.transaction.accountId = :accountId " +
		   "AND td.transaction.txnType = com.FraudBusters.TransactionMonitoring.models.enums.TransactionType.DEBIT " +
		   "AND td.transaction.txnTimestamp >= :startOfDay " +
		   "AND td.transaction.txnTimestamp <= :endOfDay " +
		   "AND (:txnId IS NULL OR td.transaction.txnId <> :txnId)")
	BigDecimal sumAllowedDebitTransactionsForAccountByDay(
			@Param("accountId") String accountId,
			@Param("startOfDay") LocalDateTime startOfDay,
			@Param("endOfDay") LocalDateTime endOfDay,
			@Param("decision") DecisionType decision,
			@Param("txnId") String txnId);

	@Query("SELECT DISTINCT td.transaction FROM TransactionDecisionEntity td " +
			"WHERE td.decision = :decision " +
			"AND td.transaction.accountId = :accountId " +
			"AND td.transaction.txnType = com.FraudBusters.TransactionMonitoring.models.enums.TransactionType.DEBIT " +
			"AND td.transaction.txnTimestamp >= :windowStart " +
			"AND td.transaction.txnTimestamp <= :windowEnd " +
			"AND (:txnId IS NULL OR td.transaction.txnId <> :txnId) " +
			"ORDER BY td.transaction.txnTimestamp DESC")
	List<TransactionEntity> findAllowedDebitTransactionsForAccountInWindow(
			@Param("accountId") String accountId,
			@Param("windowStart") LocalDateTime windowStart,
			@Param("windowEnd") LocalDateTime windowEnd,
			@Param("decision") DecisionType decision,
			@Param("txnId") String txnId);
}
