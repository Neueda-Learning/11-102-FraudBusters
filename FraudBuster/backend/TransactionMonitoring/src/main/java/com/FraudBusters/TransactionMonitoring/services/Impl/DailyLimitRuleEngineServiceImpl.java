package com.FraudBusters.TransactionMonitoring.services.Impl;

import com.FraudBusters.TransactionMonitoring.models.AlertEntity;
import com.FraudBusters.TransactionMonitoring.models.AlertTransactionEntity;
import com.FraudBusters.TransactionMonitoring.models.RuleEntity;
import com.FraudBusters.TransactionMonitoring.models.TransactionEntity;
import com.FraudBusters.TransactionMonitoring.models.enums.*;
import com.FraudBusters.TransactionMonitoring.repository.AlertEntityRepository;
import com.FraudBusters.TransactionMonitoring.repository.AlertTransactionEntityRepo;
import com.FraudBusters.TransactionMonitoring.repository.TransactionDecisionEntityRepo;
import com.FraudBusters.TransactionMonitoring.repository.RuleEntityRepository;
import com.FraudBusters.TransactionMonitoring.repository.TransactionEntityRepository;
import com.FraudBusters.TransactionMonitoring.services.RuleEngineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
public class DailyLimitRuleEngineServiceImpl implements RuleEngineService {


    @Autowired
    private AlertEntityRepository alertEntityRepository;
    @Autowired
    private RuleEntityRepository ruleEntityRepository;
    @Autowired
    private TransactionEntityRepository transactionEntityRepository;
    @Autowired
    private AlertTransactionEntityRepo alertTransactionEntityRepository;
    @Autowired
    private TransactionDecisionEntityRepo transactionDecisionEntityRepo;

    private static final Pattern DAILY_LIMIT_PATTERN =
            Pattern.compile("\\\"dailyLimitAmount\\\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");


    @Override
    public boolean evaluateTransaction(TransactionEntity transaction) {
        Optional<RuleEntity> dailyLimitRuleOpt =
                ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("DAILY_LIMIT");

        if (dailyLimitRuleOpt.isEmpty()) {
            throw new IllegalStateException("DAILY_LIMIT rule is not configured in database");
        }

        // Reuse persisted row if txn_id already exists to avoid duplicate key violations.
        TransactionEntity resolvedTransaction = transactionEntityRepository
                .findByTxnId(transaction.getTxnId())
                .orElseGet(() -> {
                    transaction.setMonitorState(MonitorState.RECEIVED);
                    transaction.setFinalDecision(FinalDecision.PENDING);
                    transaction.setUpdatedAt(LocalDateTime.now());
                    return transactionEntityRepository.save(transaction);
                });

        RuleEntity dailyLimitRule = dailyLimitRuleOpt.get();
        BigDecimal dailyLimitAmount = getDailyLimitFromConfigJson(dailyLimitRule);

        // Sum only successful (ALLOW) DEBIT transactions already completed for this account today.
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

        BigDecimal previousDailyTotal = transactionDecisionEntityRepo
                .sumAllowedDebitTransactionsForAccountByDay(
                        resolvedTransaction.getAccountId(),
                        startOfDay,
                        endOfDay,
                        DecisionType.ALLOW,
                        resolvedTransaction.getTxnId());

        // Explicitly include the current transaction in the daily-limit check.
        BigDecimal projectedDailyTotal = previousDailyTotal.add(resolvedTransaction.getAmount());

        if (projectedDailyTotal.compareTo(dailyLimitAmount) > 0) {
            resolvedTransaction.setMonitorState(MonitorState.HELD);
            resolvedTransaction.setUpdatedAt(LocalDateTime.now());
            resolvedTransaction.setDecidedAt(LocalDateTime.now());

            transactionEntityRepository.save(resolvedTransaction);

            AlertEntity alertEntity = new AlertEntity();
            alertEntity.setTitle("Daily Limit Exceeded");
            alertEntity.setDescription("Account " + resolvedTransaction.getAccountId() +
                    " has spent " + projectedDailyTotal + " today, exceeding the daily limit of " + dailyLimitAmount);
            alertEntity.setAlertCode("DL-" + System.currentTimeMillis());
            alertEntity.setSeverity(SeverityLevel.HIGH);
            alertEntity.setRule(dailyLimitRule);
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

        resolvedTransaction.setMonitorState(MonitorState.RELEASED);
        resolvedTransaction.setUpdatedAt(LocalDateTime.now());
        resolvedTransaction.setDecidedAt(LocalDateTime.now());
        resolvedTransaction.setDecisionReason("Daily total of " + projectedDailyTotal +
                " is within the daily limit of " + dailyLimitAmount);
        resolvedTransaction.setFinalDecision(FinalDecision.ALLOW);
        transactionEntityRepository.save(resolvedTransaction);

        return false;

    }

    private BigDecimal getDailyLimitFromConfigJson(RuleEntity ruleEntity) {
        String configJson = ruleEntity.getConfigJson();
        if (configJson == null || configJson.isBlank()) {
            throw new IllegalStateException("DAILY_LIMIT rule config_json is empty");
        }

        Matcher matcher = DAILY_LIMIT_PATTERN.matcher(configJson);
        if (!matcher.find()) {
            throw new IllegalStateException("dailyLimitAmount is missing in DAILY_LIMIT rule config_json");
        }

        return new BigDecimal(matcher.group(1));
    }

}
