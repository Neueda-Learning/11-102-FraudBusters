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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AmountThresholdRuleEngineServiceImpl implements RuleEngineService {

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

    private static final Pattern THRESHOLD_PATTERN =
            Pattern.compile("\"thresholdAmount\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");


    //implement  evaluateTransactionUsingAmountThreshold method to check if the transaction amount exceeds the threshold defined in the AMOUNT_THRESHOLD rule. If it does, create an alert and associate it with the transaction. If not, update the transaction state to RELEASED and set the final decision to ALLOW.


    public Optional<Boolean> evaluateTransaction(TransactionRequestDTO transactionRequestDTO) {
        TransactionEntity transactionEntity = modelMapper.map(transactionRequestDTO, TransactionEntity.class);
        return Optional.of(evaluateTransaction(transactionEntity));
    }

    @Override
    public boolean evaluateTransaction(TransactionEntity transaction) {
        Optional<RuleEntity> amountThresholdRuleOpt =
                ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("AMOUNT_THRESHOLD");

        if (amountThresholdRuleOpt.isEmpty()) {
            throw new IllegalStateException("AMOUNT_THRESHOLD rule is not configured in database");
        }

        // Resolve the transaction: if it already exists in DB, use that record (has the real id).
        // If it does not exist yet, save it first so it gets an id, then evaluate.
        TransactionEntity resolvedTransaction = transactionEntityRepository
                .findByTxnId(transaction.getTxnId())
                .orElseGet(() -> {
                    transaction.setMonitorState(MonitorState.RECEIVED);
                    transaction.setFinalDecision(FinalDecision.PENDING);
                    transaction.setCreatedAt(LocalDateTime.now());
                    transaction.setUpdatedAt(LocalDateTime.now());
                    return transactionEntityRepository.save(transaction);
                });

        RuleEntity amountThresholdRule = amountThresholdRuleOpt.get();
        BigDecimal thresholdAmount = getThresholdValueFromConfigJson(amountThresholdRule);

        if (resolvedTransaction.getAmount().compareTo(thresholdAmount) > 0) {
            resolvedTransaction.setMonitorState(MonitorState.HELD);
            resolvedTransaction.setUpdatedAt(LocalDateTime.now());
            resolvedTransaction.setDecidedAt(LocalDateTime.now());

            transactionEntityRepository.save(resolvedTransaction);

            AlertEntity alertEntity = new AlertEntity();
            String contextDescription = "Transaction amount " + resolvedTransaction.getAmount()
                    + " exceeds configured threshold " + thresholdAmount;
            alertEntity.setTitle(resolveRuleTitle(amountThresholdRule, "High Amount Transaction Detected"));
            alertEntity.setDescription(buildRuleDescription(amountThresholdRule, contextDescription));
            alertEntity.setAlertCode("AMT-" + System.currentTimeMillis());
            alertEntity.setSeverity(amountThresholdRule.getSeverityDefault());
            alertEntity.setRule(amountThresholdRule);
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
        resolvedTransaction.setDecisionReason("Transaction amount is within the defined threshold of " + thresholdAmount);
        resolvedTransaction.setFinalDecision(FinalDecision.ALLOW);
        transactionEntityRepository.save(resolvedTransaction);

        return false;
    }

    private BigDecimal getThresholdValueFromConfigJson(RuleEntity ruleEntity) {
        String configJson = ruleEntity.getConfigJson();
        if (configJson == null || configJson.isBlank()) {
            throw new IllegalStateException("AMOUNT_THRESHOLD rule config_json is empty");
        }

        Matcher matcher = THRESHOLD_PATTERN.matcher(configJson);
        if (!matcher.find()) {
            throw new IllegalStateException("thresholdAmount is missing in AMOUNT_THRESHOLD rule config_json");
        }

        return new BigDecimal(matcher.group(1));
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
