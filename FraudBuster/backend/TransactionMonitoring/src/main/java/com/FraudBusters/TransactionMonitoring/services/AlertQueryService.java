package com.FraudBusters.TransactionMonitoring.services;

import com.FraudBusters.TransactionMonitoring.dto.AlertItemDto;
import com.FraudBusters.TransactionMonitoring.dto.AlertHistoryItemDto;
import com.FraudBusters.TransactionMonitoring.dto.AlertHistoryPageDto;
import com.FraudBusters.TransactionMonitoring.dto.AlertsPageDto;
import com.FraudBusters.TransactionMonitoring.models.AlertEntity;
import com.FraudBusters.TransactionMonitoring.models.AlertTransactionEntity;
import com.FraudBusters.TransactionMonitoring.models.RuleEntity;
import com.FraudBusters.TransactionMonitoring.models.TransactionEntity;
import com.FraudBusters.TransactionMonitoring.models.enums.AlertStatus;
import com.FraudBusters.TransactionMonitoring.models.enums.SeverityLevel;
import com.FraudBusters.TransactionMonitoring.repository.AlertRepository;
import com.FraudBusters.TransactionMonitoring.repository.AlertTransactionRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class AlertQueryService {

    private final AlertRepository alertRepository;
    private final AlertTransactionRepository alertTransactionRepository;

    public AlertQueryService(AlertRepository alertRepository, AlertTransactionRepository alertTransactionRepository) {
        this.alertRepository = alertRepository;
        this.alertTransactionRepository = alertTransactionRepository;
    }

    public AlertsPageDto getAlertsPage() {
        List<AlertEntity> alerts = alertRepository.findAllWithRuleOrderByCreatedAtDesc();

        List<Long> alertIds = alerts.stream().map(AlertEntity::getId).toList();
        List<AlertTransactionEntity> links = alertIds.isEmpty()
                ? List.of()
                : alertTransactionRepository.findByAlertIdsWithTransaction(alertIds);

        Map<Long, TransactionEntity> firstTxnByAlertId = new HashMap<>();
        for (AlertTransactionEntity link : links) {
            Long alertId = link.getAlert().getId();
            firstTxnByAlertId.putIfAbsent(alertId, link.getTransaction());
        }

        List<AlertItemDto> items = alerts.stream()
                .map(alert -> toAlertItem(alert, firstTxnByAlertId.get(alert.getId())))
                .toList();

        long total = alerts.size();
        long open = alerts.stream().filter(a -> a.getStatus() == AlertStatus.OPEN).count();
        long acknowledged = alerts.stream().filter(a -> a.getStatus() == AlertStatus.ACKNOWLEDGED).count();
        long investigating = alerts.stream().filter(a -> a.getStatus() == AlertStatus.INVESTIGATING).count();

        return new AlertsPageDto(total, open, acknowledged, investigating, items);
    }

    public AlertHistoryPageDto getHistoryPage() {
        List<AlertEntity> alerts = alertRepository.findHistoryAlertsWithRuleOrderByClosedAtDesc();

        List<AlertHistoryItemDto> rows = alerts.stream()
                .map(this::toHistoryItem)
                .toList();

        return new AlertHistoryPageDto(rows.size(), rows);
    }

    private AlertHistoryItemDto toHistoryItem(AlertEntity alert) {
        String severity = alert.getSeverity().name();
        String severityClass = toSeverityClass(alert.getSeverity());
        String status = alert.getStatus().name();
        String statusClass = toStatusClass(alert.getStatus());
        String ruleName = alert.getRule() != null ? alert.getRule().getName() : "Unknown Rule";

        String notes = (alert.getClosureReason() != null && !alert.getClosureReason().isBlank())
                ? alert.getClosureReason()
                : (alert.getDescription() != null ? alert.getDescription() : "-");

        return new AlertHistoryItemDto(
                alert.getAlertCode(),
                severity,
                severityClass,
                ruleName,
                alert.getCreatedAt(),
                alert.getClosedAt(),
                status,
                statusClass,
                notes
        );
    }

    private AlertItemDto toAlertItem(AlertEntity alert, TransactionEntity transaction) {
        String severity = alert.getSeverity().name();
        String severityClass = toSeverityClass(alert.getSeverity());
        String status = alert.getStatus().name();
        String statusClass = toStatusClass(alert.getStatus());

        String ruleName = alert.getRule() != null ? alert.getRule().getName() : "Unknown Rule";
        String threshold = extractThreshold(alert.getRule());

        String accountId = transaction != null ? transaction.getAccountId() : "-";
        String amount = transaction != null ? formatMoney(transaction.getAmount()) : "-";
        String payeeId = transaction != null ? transaction.getPayeeId() : "-";

        String relatedTxnId = transaction != null ? transaction.getTxnId() : "-";
        String relatedTxnAmount = transaction != null ? formatMoney(transaction.getAmount()) : "-";
        String relatedTxnTime = transaction != null && transaction.getTxnTimestamp() != null
                ? transaction.getTxnTimestamp().toString()
                : "-";

        return new AlertItemDto(
                alert.getAlertCode(),
                severity,
                severityClass,
                ruleName,
                status,
                statusClass,
                accountId,
                amount,
                payeeId,
                alert.getCreatedAt(),
                threshold,
                relatedTxnId,
                relatedTxnAmount,
                relatedTxnTime
        );
    }

    private String toSeverityClass(SeverityLevel severity) {
        if (severity == SeverityLevel.HIGH || severity == SeverityLevel.CRITICAL) {
            return "sev-high";
        }
        if (severity == SeverityLevel.MEDIUM) {
            return "sev-medium";
        }
        return "sev-low";
    }

    private String toStatusClass(AlertStatus status) {
        return switch (status) {
            case OPEN -> "st-open";
            case ACKNOWLEDGED -> "st-acknowledged";
            case INVESTIGATING -> "st-investigating";
            case CLOSED -> "st-closed";
            case DISMISSED -> "st-dismissed";
        };
    }

    private String extractThreshold(RuleEntity rule) {
        if (rule == null) {
            return "DB value";
        }

        String code = rule.getRuleCode();
        String config = rule.getConfigJson();

        if ("AMOUNT_THRESHOLD".equals(code)) {
            return valueOrDb(config, "thresholdAmount");
        }
        if ("VELOCITY_CHECK".equals(code)) {
            return "> " + valueOrDb(config, "maxTransactions") + " in " + valueOrDb(config, "windowMinutes") + " mins";
        }
        if ("DAILY_LIMIT".equals(code)) {
            return valueOrDb(config, "dailyLimitAmount");
        }
        if ("NEW_PAYEE".equals(code)) {
            return "First txn to any payee";
        }
        return "DB value";
    }

    private String valueOrDb(String rawJson, String key) {
        if (rawJson == null || rawJson.isBlank()) {
            return "DB value";
        }

        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(\"([^\"]+)\"|([0-9.]+))");
        Matcher matcher = pattern.matcher(rawJson);
        if (matcher.find()) {
            String stringValue = matcher.group(2);
            String numericValue = matcher.group(3);
            return stringValue != null ? stringValue : numericValue;
        }
        return "DB value";
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) {
            return "-";
        }
        return "$" + value.stripTrailingZeros().toPlainString();
    }
}



