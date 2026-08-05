package com.FraudBusters.TransactionMonitoring.repository;


import com.FraudBusters.TransactionMonitoring.models.AlertEntity;
import com.FraudBusters.TransactionMonitoring.models.enums.AlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertEntityRepository extends JpaRepository<AlertEntity, Long> {

    /** Find alert by its business-facing alertCode (used by all operator lifecycle endpoints) */
    Optional<AlertEntity> findByAlertCode(String alertCode);

    /** Returns CLOSED and DISMISSED alerts sorted by most recently closed first. */
    List<AlertEntity> findByStatusInOrderByClosedAtDesc(List<AlertStatus> statuses);
}
