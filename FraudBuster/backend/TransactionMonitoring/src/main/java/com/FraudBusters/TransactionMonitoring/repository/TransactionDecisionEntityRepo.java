package com.FraudBusters.TransactionMonitoring.repository;

import com.FraudBusters.TransactionMonitoring.models.TransactionDecisionEntity;
import com.FraudBusters.TransactionMonitoring.models.enums.DecisionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface TransactionDecisionEntityRepo extends JpaRepository<TransactionDecisionEntity, Long> {

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
}
