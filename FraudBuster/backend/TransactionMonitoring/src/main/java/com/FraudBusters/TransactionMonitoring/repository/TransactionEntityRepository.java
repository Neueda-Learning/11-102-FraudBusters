package com.FraudBusters.TransactionMonitoring.repository;

import com.FraudBusters.TransactionMonitoring.models.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TransactionEntityRepository extends JpaRepository<TransactionEntity, Long> {

    /** Look up a transaction by its unique business ID */
    Optional<TransactionEntity> findByTxnId(String txnId);

    /** Dashboard summary count for today's transactions using [start, end) on createdAt. */
    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime start, LocalDateTime end);
}
