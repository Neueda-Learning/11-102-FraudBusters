package com.FraudBusters.TransactionMonitoring.services.Impl;

import com.FraudBusters.TransactionMonitoring.models.AlertEntity;
import com.FraudBusters.TransactionMonitoring.models.AlertTransactionEntity;
import com.FraudBusters.TransactionMonitoring.models.RuleEntity;
import com.FraudBusters.TransactionMonitoring.models.TransactionEntity;
import com.FraudBusters.TransactionMonitoring.models.dto.TransactionRequestDTO;
import com.FraudBusters.TransactionMonitoring.models.enums.AlertRelationType;
import com.FraudBusters.TransactionMonitoring.models.enums.AlertStatus;
import com.FraudBusters.TransactionMonitoring.models.enums.FinalDecision;
import com.FraudBusters.TransactionMonitoring.models.enums.MonitorState;
import com.FraudBusters.TransactionMonitoring.repository.AlertEntityRepository;
import com.FraudBusters.TransactionMonitoring.repository.AlertTransactionEntityRepo;
import com.FraudBusters.TransactionMonitoring.repository.RuleEntityRepository;
import com.FraudBusters.TransactionMonitoring.repository.TransactionEntityRepository;
import com.FraudBusters.TransactionMonitoring.services.RuleEngineService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class NewPayeeRuleEngineImpl implements RuleEngineService {

    @Autowired
    private AlertEntityRepository alertEntityRepository;

    @Autowired
    private RuleEntityRepository ruleEntityRepository;

    @Autowired
    private TransactionEntityRepository transactionEntityRepository;

    @Autowired
    private AlertTransactionEntityRepo alertTransactionEntityRepository;

    @Autowired
    private ModelMapper modelMapper;

    public Optional<Boolean> evaluateTransaction(TransactionRequestDTO transactionRequestDTO) {
        TransactionEntity transactionEntity = modelMapper.map(transactionRequestDTO, TransactionEntity.class);
        return Optional.of(evaluateTransaction(transactionEntity));
    }

    @Override
    public boolean evaluateTransaction(TransactionEntity transaction) {
        Optional<RuleEntity> newPayeeRuleOpt =
                ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("NEW_PAYEE");

        if (newPayeeRuleOpt.isEmpty()) {
            throw new IllegalStateException("NEW_PAYEE rule is not configured in database");
        }

        // Reuse persisted row if txn_id already exists; otherwise save it first so it has an id.
        TransactionEntity resolvedTransaction = transactionEntityRepository
                .findByTxnId(transaction.getTxnId())
                .orElseGet(() -> {
                    transaction.setMonitorState(MonitorState.RECEIVED);
                    transaction.setFinalDecision(FinalDecision.PENDING);
                    transaction.setUpdatedAt(LocalDateTime.now());
                    return transactionEntityRepository.save(transaction);
                });

        RuleEntity newPayeeRule = newPayeeRuleOpt.get();

        boolean relationExists = transactionEntityRepository
                .existsByAccountIdAndPayeeIdAndTxnIdNot(
                        resolvedTransaction.getAccountId(),
                        resolvedTransaction.getPayeeId(),
                        resolvedTransaction.getTxnId());

        if (!relationExists) {
            resolvedTransaction.setMonitorState(MonitorState.HELD);
            resolvedTransaction.setUpdatedAt(LocalDateTime.now());
            resolvedTransaction.setDecidedAt(LocalDateTime.now());
            transactionEntityRepository.save(resolvedTransaction);

            AlertEntity alertEntity = new AlertEntity();
            String contextDescription = "Account " + resolvedTransaction.getAccountId()
                    + " has used payee " + resolvedTransaction.getPayeeId() + " for the first time";
            alertEntity.setTitle(resolveRuleTitle(newPayeeRule, "New Payee Detected"));
            alertEntity.setDescription(buildRuleDescription(newPayeeRule, contextDescription));
            alertEntity.setAlertCode("NP-" + System.currentTimeMillis());
            alertEntity.setSeverity(newPayeeRule.getSeverityDefault());
            alertEntity.setRule(newPayeeRule);
            alertEntity.setStatus(AlertStatus.OPEN);
            alertEntity.setCreatedAt(LocalDateTime.now());
            alertEntity.setUpdatedAt(LocalDateTime.now());

            alertEntityRepository.save(alertEntity);

            AlertTransactionEntity alertTransactionEntity = new AlertTransactionEntity();
            alertTransactionEntity.setTransaction(resolvedTransaction);
            alertTransactionEntity.setAlert(alertEntity);
            alertTransactionEntity.setCreatedAt(LocalDateTime.now());
            alertTransactionEntity.setRelationType(AlertRelationType.TRIGGERING_TRANSACTION);

            alertTransactionEntityRepository.save(alertTransactionEntity);
            return true;
        }

        // Do not override a hold created by another rule in multi-rule orchestration.
        if (MonitorState.HELD.equals(resolvedTransaction.getMonitorState())) {
            return false;
        }

        resolvedTransaction.setMonitorState(MonitorState.RELEASED);
        resolvedTransaction.setUpdatedAt(LocalDateTime.now());
        resolvedTransaction.setDecidedAt(LocalDateTime.now());
        resolvedTransaction.setDecisionReason("Payee relation already exists for account "
                + resolvedTransaction.getAccountId() + " and payee " + resolvedTransaction.getPayeeId());
        resolvedTransaction.setFinalDecision(FinalDecision.ALLOW);
        transactionEntityRepository.save(resolvedTransaction);

        return false;
    }

    private String resolveRuleTitle(RuleEntity ruleEntity, String fallbackTitle) {
        return (ruleEntity.getName() == null || ruleEntity.getName().isBlank())
                ? fallbackTitle
                : ruleEntity.getName();
    }

    private String buildRuleDescription(RuleEntity ruleEntity, String contextDescription) {
        if (ruleEntity.getDescription() == null || ruleEntity.getDescription().isBlank()) {
            return contextDescription;
        }
        return ruleEntity.getDescription() + " | " + contextDescription;
    }
}
