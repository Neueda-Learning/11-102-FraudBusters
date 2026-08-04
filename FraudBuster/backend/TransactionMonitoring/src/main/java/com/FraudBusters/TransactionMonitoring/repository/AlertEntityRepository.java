package com.FraudBusters.TransactionMonitoring.repository;


import com.FraudBusters.TransactionMonitoring.models.AlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertEntityRepository extends JpaRepository<AlertEntity, Long> {
}
