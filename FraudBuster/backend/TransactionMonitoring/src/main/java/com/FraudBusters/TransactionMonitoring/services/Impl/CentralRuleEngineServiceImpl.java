package com.FraudBusters.TransactionMonitoring.services.Impl;

import com.FraudBusters.TransactionMonitoring.models.RuleEntity;
import com.FraudBusters.TransactionMonitoring.models.TransactionDecisionEntity;
import com.FraudBusters.TransactionMonitoring.models.TransactionEntity;
import com.FraudBusters.TransactionMonitoring.models.dto.CentralRuleEvaluationResponseDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.TransactionRequestDTO;
import com.FraudBusters.TransactionMonitoring.models.enums.DecisionType;
import com.FraudBusters.TransactionMonitoring.models.enums.FinalDecision;
import com.FraudBusters.TransactionMonitoring.models.enums.MonitorState;
import com.FraudBusters.TransactionMonitoring.repository.RuleEntityRepository;
import com.FraudBusters.TransactionMonitoring.repository.TransactionDecisionEntityRepo;
import com.FraudBusters.TransactionMonitoring.repository.TransactionEntityRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CentralRuleEngineServiceImpl {

    private final RuleEntityRepository ruleEntityRepository;
    private final TransactionEntityRepository transactionEntityRepository;
    private final TransactionDecisionEntityRepo transactionDecisionEntityRepo;
    private final ModelMapper modelMapper;
    private final AmountThresholdRuleEngineServiceImpl amountThresholdRuleEngineService;
    private final DailyLimitRuleEngineServiceImpl dailyLimitRuleEngineService;
    private final NewPayeeRuleEngineImpl newPayeeRuleEngine;
    private final VelocityCheckRuleEngineImpl velocityCheckRuleEngine;

    public CentralRuleEngineServiceImpl(
            RuleEntityRepository ruleEntityRepository,
            TransactionEntityRepository transactionEntityRepository,
            TransactionDecisionEntityRepo transactionDecisionEntityRepo,
            ModelMapper modelMapper,
            AmountThresholdRuleEngineServiceImpl amountThresholdRuleEngineService,
            DailyLimitRuleEngineServiceImpl dailyLimitRuleEngineService,
            NewPayeeRuleEngineImpl newPayeeRuleEngine,
            VelocityCheckRuleEngineImpl velocityCheckRuleEngine) {
        this.ruleEntityRepository = ruleEntityRepository;
        this.transactionEntityRepository = transactionEntityRepository;
        this.transactionDecisionEntityRepo = transactionDecisionEntityRepo;
        this.modelMapper = modelMapper;
        this.amountThresholdRuleEngineService = amountThresholdRuleEngineService;
        this.dailyLimitRuleEngineService = dailyLimitRuleEngineService;
        this.newPayeeRuleEngine = newPayeeRuleEngine;
        this.velocityCheckRuleEngine = velocityCheckRuleEngine;
    }

    public CentralRuleEvaluationResponseDTO evaluateAgainstActiveRules(TransactionRequestDTO transactionRequestDTO) {
        TransactionEntity transactionEntity = modelMapper.map(transactionRequestDTO, TransactionEntity.class);
        return evaluateAgainstActiveRules(transactionEntity);
    }

    public CentralRuleEvaluationResponseDTO evaluateAgainstActiveRules(TransactionEntity transaction) {
        TransactionEntity resolvedTransaction = transactionEntityRepository
                .findByTxnId(transaction.getTxnId())
                .orElseGet(() -> {
                    transaction.setMonitorState(MonitorState.RECEIVED);
                    transaction.setFinalDecision(FinalDecision.PENDING);
                    transaction.setUpdatedAt(LocalDateTime.now());
                    return transactionEntityRepository.save(transaction);
                });

        boolean wasHeldBeforeEvaluation = MonitorState.HELD.equals(resolvedTransaction.getMonitorState());

        List<RuleEntity> activeRules = ruleEntityRepository
                .findByIsActiveTrueAndIsDeletedFalse()
                .stream()
                .sorted(Comparator.comparing(RuleEntity::getId))
                .toList();

        Map<String, Boolean> ruleResults = new LinkedHashMap<>();
        List<String> skippedRuleCodes = new ArrayList<>();
        boolean anyRuleTriggered = false;

        for (RuleEntity ruleEntity : activeRules) {
            Boolean ruleTriggered = evaluateByRuleCode(ruleEntity.getRuleCode(), resolvedTransaction);
            if (ruleTriggered == null) {
                skippedRuleCodes.add(ruleEntity.getRuleCode());
                continue;
            }
            ruleResults.put(ruleEntity.getRuleCode(), ruleTriggered);
            if (ruleTriggered) {
                anyRuleTriggered = true;
            }
        }

        TransactionEntity finalTransaction = transactionEntityRepository
                .findByTxnId(resolvedTransaction.getTxnId())
                .orElse(resolvedTransaction);

        if (anyRuleTriggered) {
            finalTransaction.setMonitorState(MonitorState.HELD);
            finalTransaction.setFinalDecision(FinalDecision.PENDING);
            finalTransaction.setDecisionReason("Held by central rule engine: one or more rules triggered");
            finalTransaction.setUpdatedAt(LocalDateTime.now());
            finalTransaction.setDecidedAt(LocalDateTime.now());
            finalTransaction = transactionEntityRepository.save(finalTransaction);
        } else if (!wasHeldBeforeEvaluation && !MonitorState.HELD.equals(finalTransaction.getMonitorState())) {
            finalTransaction.setMonitorState(MonitorState.RELEASED);
            finalTransaction.setFinalDecision(FinalDecision.ALLOW);
            finalTransaction.setDecisionReason("Passed all active rules in central rule engine");
            finalTransaction.setUpdatedAt(LocalDateTime.now());
            finalTransaction.setDecidedAt(LocalDateTime.now());
            finalTransaction = transactionEntityRepository.save(finalTransaction);

            if (!transactionDecisionEntityRepo.existsByTransactionTxnId(finalTransaction.getTxnId())) {
                TransactionDecisionEntity decisionEntity = TransactionDecisionEntity.builder()
                        .transaction(finalTransaction)
                        .alert(null)
                        .decision(DecisionType.ALLOW)
                        .decidedBy("SYSTEM")
                        .decisionReason("No active rule triggered in central rule engine")
                        .build();
                transactionDecisionEntityRepo.save(decisionEntity);
            }
        }

        return CentralRuleEvaluationResponseDTO.builder()
                .txnId(finalTransaction.getTxnId())
                .anyRuleTriggered(anyRuleTriggered)
                .evaluatedRuleCount(ruleResults.size())
                .ruleResults(ruleResults)
                .skippedRuleCodes(skippedRuleCodes)
                .monitorState(finalTransaction.getMonitorState())
                .finalDecision(finalTransaction.getFinalDecision())
                .decisionReason(finalTransaction.getDecisionReason())
                .build();
    }

    public List<CentralRuleEvaluationResponseDTO> evaluatePendingTransactionsBatch() {
        List<TransactionEntity> pendingTransactions = transactionEntityRepository
                .findTop100ByMonitorStateAndFinalDecisionOrderByTxnTimestampAsc(
                        MonitorState.RECEIVED,
                        FinalDecision.PENDING);

        List<CentralRuleEvaluationResponseDTO> responses = new ArrayList<>();
        for (TransactionEntity pendingTransaction : pendingTransactions) {
            responses.add(evaluateAgainstActiveRules(pendingTransaction));
        }
        return responses;
    }

    private Boolean evaluateByRuleCode(String ruleCode, TransactionEntity transaction) {
        return switch (ruleCode) {
            case "AMOUNT_THRESHOLD" -> amountThresholdRuleEngineService.evaluateTransaction(transaction);
            case "DAILY_LIMIT" -> dailyLimitRuleEngineService.evaluateTransaction(transaction);
            case "NEW_PAYEE" -> newPayeeRuleEngine.evaluateTransaction(transaction);
            case "VELOCITY_CHECK" -> velocityCheckRuleEngine.evaluateTransaction(transaction);
            default -> null;
        };
    }
}


