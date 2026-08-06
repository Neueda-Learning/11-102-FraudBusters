package com.FraudBusters.TransactionMonitoring.services.Impl;

import com.FraudBusters.TransactionMonitoring.models.AlertEntity;
import com.FraudBusters.TransactionMonitoring.models.AlertTransactionEntity;
import com.FraudBusters.TransactionMonitoring.models.RuleEntity;
import com.FraudBusters.TransactionMonitoring.models.TransactionEntity;
import com.FraudBusters.TransactionMonitoring.models.dto.TransactionRequestDTO;
import com.FraudBusters.TransactionMonitoring.models.enums.AlertRelationType;
import com.FraudBusters.TransactionMonitoring.models.enums.AlertStatus;
import com.FraudBusters.TransactionMonitoring.models.enums.DecisionType;
import com.FraudBusters.TransactionMonitoring.models.enums.FinalDecision;
import com.FraudBusters.TransactionMonitoring.models.enums.MonitorState;
import com.FraudBusters.TransactionMonitoring.repository.AlertEntityRepository;
import com.FraudBusters.TransactionMonitoring.repository.AlertTransactionEntityRepo;
import com.FraudBusters.TransactionMonitoring.repository.RuleEntityRepository;
import com.FraudBusters.TransactionMonitoring.repository.TransactionDecisionEntityRepo;
import com.FraudBusters.TransactionMonitoring.repository.TransactionEntityRepository;
import com.FraudBusters.TransactionMonitoring.services.RuleEngineService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class VelocityCheckRuleEngineImpl implements RuleEngineService {

    private final AlertEntityRepository alertEntityRepository;
    private final RuleEntityRepository ruleEntityRepository;
    private final TransactionEntityRepository transactionEntityRepository;
    private final TransactionDecisionEntityRepo transactionDecisionEntityRepo;
    private final AlertTransactionEntityRepo alertTransactionEntityRepository;
    private final ModelMapper modelMapper;

    private static final Pattern WINDOW_MINUTES_PATTERN =
            Pattern.compile("\"windowMinutes\"\\s*:\\s*([0-9]+)");
    private static final Pattern MAX_TRANSACTIONS_PATTERN =
            Pattern.compile("\"maxTransactions\"\\s*:\\s*([0-9]+)");

    public VelocityCheckRuleEngineImpl(
            AlertEntityRepository alertEntityRepository,
            RuleEntityRepository ruleEntityRepository,
            TransactionEntityRepository transactionEntityRepository,
            TransactionDecisionEntityRepo transactionDecisionEntityRepo,
            AlertTransactionEntityRepo alertTransactionEntityRepository,
            ModelMapper modelMapper) {
        this.alertEntityRepository = alertEntityRepository;
        this.ruleEntityRepository = ruleEntityRepository;
        this.transactionEntityRepository = transactionEntityRepository;
        this.transactionDecisionEntityRepo = transactionDecisionEntityRepo;
        this.alertTransactionEntityRepository = alertTransactionEntityRepository;
        this.modelMapper = modelMapper;
    }

    public Optional<Boolean> evaluateTransaction(TransactionRequestDTO transactionRequestDTO) {
        TransactionEntity transactionEntity = modelMapper.map(transactionRequestDTO, TransactionEntity.class);
        return Optional.of(evaluateTransaction(transactionEntity));
    }

    @Override
    public boolean evaluateTransaction(TransactionEntity transaction) {
        Optional<RuleEntity> velocityRuleOpt =
                ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("VELOCITY_CHECK");

        if (velocityRuleOpt.isEmpty()) {
            throw new IllegalStateException("VELOCITY_CHECK rule is not configured in database");
        }

        TransactionEntity resolvedTransaction = transactionEntityRepository
                .findByTxnId(transaction.getTxnId())
                .orElseGet(() -> {
                    transaction.setMonitorState(MonitorState.RECEIVED);
                    transaction.setFinalDecision(FinalDecision.PENDING);
                    transaction.setUpdatedAt(LocalDateTime.now());
                    return transactionEntityRepository.save(transaction);
                });

        RuleEntity velocityRule = velocityRuleOpt.get();
        VelocityRuleParams params = getVelocityRuleParamsFromConfigJson(velocityRule);

        LocalDateTime evaluationTime = resolvedTransaction.getTxnTimestamp() != null
                ? resolvedTransaction.getTxnTimestamp()
                : LocalDateTime.now();
        LocalDateTime windowStart = evaluationTime.minusMinutes(params.windowMinutes());

        List<TransactionEntity> priorAllowedTransactions = transactionDecisionEntityRepo
                .findAllowedDebitTransactionsForAccountInWindow(
                        resolvedTransaction.getAccountId(),
                        windowStart,
                        evaluationTime,
                        DecisionType.ALLOW,
                        resolvedTransaction.getTxnId());

        int priorAllowedCount = priorAllowedTransactions.size();

        // Trigger on the current transaction if prior ALLOW count already reached the threshold.
        if (priorAllowedCount >= params.maxTransactions()) {
            resolvedTransaction.setMonitorState(MonitorState.HELD);
            resolvedTransaction.setUpdatedAt(LocalDateTime.now());
            resolvedTransaction.setDecidedAt(LocalDateTime.now());
            transactionEntityRepository.save(resolvedTransaction);

            AlertEntity alertEntity = new AlertEntity();
            String contextDescription = "Account " + resolvedTransaction.getAccountId()
                    + " has " + (priorAllowedCount + 1)
                    + " transactions within " + params.windowMinutes() + " minute window";
            alertEntity.setTitle(resolveRuleTitle(velocityRule, "Velocity Rule Triggered"));
            alertEntity.setDescription(buildRuleDescription(velocityRule, contextDescription));
            alertEntity.setAlertCode("VEL-" + System.currentTimeMillis());
            alertEntity.setSeverity(velocityRule.getSeverityDefault());
            alertEntity.setRule(velocityRule);
            alertEntity.setStatus(AlertStatus.OPEN);
            alertEntity.setCreatedAt(LocalDateTime.now());
            alertEntity.setUpdatedAt(LocalDateTime.now());
            alertEntityRepository.save(alertEntity);

            AlertTransactionEntity triggeringTransactionLink = new AlertTransactionEntity();
            triggeringTransactionLink.setTransaction(resolvedTransaction);
            triggeringTransactionLink.setAlert(alertEntity);
            triggeringTransactionLink.setCreatedAt(LocalDateTime.now());
            triggeringTransactionLink.setRelationType(AlertRelationType.TRIGGERING_TRANSACTION);
            alertTransactionEntityRepository.save(triggeringTransactionLink);

            // Keep alert context focused on the most recent N transactions from the window.
            for (TransactionEntity relatedTransaction : priorAllowedTransactions.stream().limit(params.maxTransactions()).toList()) {
                AlertTransactionEntity relatedTransactionLink = new AlertTransactionEntity();
                relatedTransactionLink.setTransaction(relatedTransaction);
                relatedTransactionLink.setAlert(alertEntity);
                relatedTransactionLink.setCreatedAt(LocalDateTime.now());
                relatedTransactionLink.setRelationType(AlertRelationType.RELATED_TRANSACTION);
                alertTransactionEntityRepository.save(relatedTransactionLink);
            }

            return true;
        }

        // Do not override a hold created by another rule in multi-rule orchestration.
        if (MonitorState.HELD.equals(resolvedTransaction.getMonitorState())) {
            return false;
        }

        resolvedTransaction.setMonitorState(MonitorState.RELEASED);
        resolvedTransaction.setUpdatedAt(LocalDateTime.now());
        resolvedTransaction.setDecidedAt(LocalDateTime.now());
        resolvedTransaction.setDecisionReason("Velocity check passed: "
                + (priorAllowedCount + 1)
                + " transaction(s) within " + params.windowMinutes()
                + " minutes, threshold is " + params.maxTransactions());
        resolvedTransaction.setFinalDecision(FinalDecision.ALLOW);
        transactionEntityRepository.save(resolvedTransaction);

        return false;
    }

    private VelocityRuleParams getVelocityRuleParamsFromConfigJson(RuleEntity ruleEntity) {
        String configJson = ruleEntity.getConfigJson();
        if (configJson == null || configJson.isBlank()) {
            throw new IllegalStateException("VELOCITY_CHECK rule config_json is empty");
        }

        Matcher windowMinutesMatcher = WINDOW_MINUTES_PATTERN.matcher(configJson);
        Matcher maxTransactionsMatcher = MAX_TRANSACTIONS_PATTERN.matcher(configJson);

        if (!windowMinutesMatcher.find()) {
            throw new IllegalStateException("windowMinutes is missing in VELOCITY_CHECK rule config_json");
        }
        if (!maxTransactionsMatcher.find()) {
            throw new IllegalStateException("maxTransactions is missing in VELOCITY_CHECK rule config_json");
        }

        int windowMinutes = Integer.parseInt(windowMinutesMatcher.group(1));
        int maxTransactions = Integer.parseInt(maxTransactionsMatcher.group(1));

        if (windowMinutes <= 0) {
            throw new IllegalStateException("windowMinutes must be greater than 0 for VELOCITY_CHECK");
        }
        if (maxTransactions <= 0) {
            throw new IllegalStateException("maxTransactions must be greater than 0 for VELOCITY_CHECK");
        }

        return new VelocityRuleParams(windowMinutes, maxTransactions);
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

    private record VelocityRuleParams(int windowMinutes, int maxTransactions) {
    }

}
