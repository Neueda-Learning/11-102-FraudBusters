package com.FraudBusters.TransactionMonitoring.repository;


import com.FraudBusters.TransactionMonitoring.models.AlertEntity;
import com.FraudBusters.TransactionMonitoring.models.enums.AlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlertEntityRepository extends JpaRepository<AlertEntity, Long> {

    /** Find alert by its business-facing alertCode (used by all operator lifecycle endpoints) */
    Optional<AlertEntity> findByAlertCode(String alertCode);

    //implement findAlertsWhoseAlertStatusIsNotDismissedAndClosed using cutom query
    @Query("SELECT a FROM AlertEntity a WHERE a.status NOT IN ('DISMISSED', 'CLOSED')")
    List<AlertEntity> findAlertsWhoseAlertStatusIsNotDismissedAndClosed();
    /** Returns CLOSED and DISMISSED alerts sorted by most recently closed first. */
    List<AlertEntity> findByStatusInOrderByClosedAtDesc(List<AlertStatus> statuses);

    /** Dashboard summary count by a single status (e.g. OPEN, ACKNOWLEDGED). */
    long countByStatus(AlertStatus status);

    /**
     * Dashboard resolved-today count using a [start, end) window on closedAt.
     * Intended statuses: CLOSED + DISMISSED.
     */
    long countByStatusInAndClosedAtGreaterThanEqualAndClosedAtLessThan(
            List<AlertStatus> statuses,
            LocalDateTime start,
            LocalDateTime end);

    /** Dashboard recent alerts card: newest 5 OPEN alerts. */
    List<AlertEntity> findTop5ByStatusOrderByCreatedAtDesc(AlertStatus status);
}
