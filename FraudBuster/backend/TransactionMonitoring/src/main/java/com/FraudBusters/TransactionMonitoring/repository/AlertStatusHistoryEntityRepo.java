package com.FraudBusters.TransactionMonitoring.repository;

import com.FraudBusters.TransactionMonitoring.models.AlertStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertStatusHistoryEntityRepo extends JpaRepository<AlertStatusHistoryEntity, Long> {

    /** Returns status transitions for one alert code in oldest-first order. */
    List<AlertStatusHistoryEntity> findByAlertAlertCodeOrderByChangedAtAsc(String alertCode);
}
