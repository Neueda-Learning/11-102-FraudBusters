package com.FraudBusters.TransactionMonitoring.repository;


import com.FraudBusters.TransactionMonitoring.models.AlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertEntityRepository extends JpaRepository<AlertEntity, Long> {

    /** Find alert by its business-facing alertCode (used by all operator lifecycle endpoints) */
    Optional<AlertEntity> findByAlertCode(String alertCode);

    //implement findAlertsWhoseAlertStatusIsNotDismissedAndClosed using cutom query
    @Query("SELECT a FROM AlertEntity a WHERE a.status NOT IN ('DISMISSED', 'CLOSED')")
    List<AlertEntity> findAlertsWhoseAlertStatusIsNotDismissedAndClosed();
}
