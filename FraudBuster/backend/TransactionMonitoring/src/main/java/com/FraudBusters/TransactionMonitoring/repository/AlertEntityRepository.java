package com.FraudBusters.TransactionMonitoring.repository;


import com.FraudBusters.TransactionMonitoring.models.AlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AlertEntityRepository extends JpaRepository<AlertEntity, Long> {

    /** Find alert by its business-facing alertCode (used by all operator lifecycle endpoints) */
    Optional<AlertEntity> findByAlertCode(String alertCode);
}
