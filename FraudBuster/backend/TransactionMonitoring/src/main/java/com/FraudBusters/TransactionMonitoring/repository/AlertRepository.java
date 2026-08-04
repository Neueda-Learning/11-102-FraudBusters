package com.FraudBusters.TransactionMonitoring.repository;

import com.FraudBusters.TransactionMonitoring.models.AlertEntity;
import com.FraudBusters.TransactionMonitoring.models.enums.AlertStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AlertRepository extends JpaRepository<AlertEntity, Long> {

    long countByStatus(AlertStatus status);

    long countByStatusAndClosedAtBetween(AlertStatus status, LocalDateTime startInclusive, LocalDateTime endExclusive);

    List<AlertEntity> findTop10ByStatusInOrderByCreatedAtDesc(Collection<AlertStatus> statuses);

    @Query("select a from AlertEntity a join fetch a.rule order by a.createdAt desc")
    List<AlertEntity> findAllWithRuleOrderByCreatedAtDesc();

    @Query("select a from AlertEntity a join fetch a.rule where a.status in ('CLOSED','DISMISSED') order by a.closedAt desc, a.createdAt desc")
    List<AlertEntity> findHistoryAlertsWithRuleOrderByClosedAtDesc();
}




