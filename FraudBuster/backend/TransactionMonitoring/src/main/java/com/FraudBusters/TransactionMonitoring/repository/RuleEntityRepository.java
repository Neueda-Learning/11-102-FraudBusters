package com.FraudBusters.TransactionMonitoring.repository;

import com.FraudBusters.TransactionMonitoring.models.RuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RuleEntityRepository extends JpaRepository<RuleEntity, Long> {

    /** All rules that are active and not soft-deleted (used for the Rules list screen) */
    List<RuleEntity> findByIsActiveTrueAndIsDeletedFalse();

    /** Find a specific rule by its stable rule_code (used for rule detail page) */
    Optional<RuleEntity> findByRuleCodeAndIsDeletedFalse(String ruleCode);
}

