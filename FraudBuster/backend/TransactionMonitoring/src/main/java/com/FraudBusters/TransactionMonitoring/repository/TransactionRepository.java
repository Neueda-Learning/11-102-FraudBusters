package com.FraudBusters.TransactionMonitoring.repository;

import com.FraudBusters.TransactionMonitoring.models.TransactionEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    long countByTxnTimestampBetween(LocalDateTime startInclusive, LocalDateTime endExclusive);

    List<TransactionEntity> findTop200ByOrderByTxnTimestampDesc();

    @Query("select count(t) from TransactionEntity t")
    long countAllTransactions();

    @Query("select coalesce(sum(t.amount), 0) from TransactionEntity t")
    BigDecimal sumAllAmounts();
}


