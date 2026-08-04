package com.FraudBusters.TransactionMonitoring.repository;

import com.FraudBusters.TransactionMonitoring.models.RuleEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleRepository extends JpaRepository<RuleEntity, Long> {

    List<RuleEntity> findByIsDeletedFalseOrderByIdAsc();

    long countByIsDeletedFalse();

    long countByIsDeletedFalseAndIsActiveTrue();
}

