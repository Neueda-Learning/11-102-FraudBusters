package com.FraudBusters.TransactionMonitoring.services;

import com.FraudBusters.TransactionMonitoring.dto.RuleViewResponse;
import com.FraudBusters.TransactionMonitoring.dto.RulesPageResponse;
import com.FraudBusters.TransactionMonitoring.models.RuleEntity;
import com.FraudBusters.TransactionMonitoring.repository.RuleRepository;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class RuleQueryService {

    private final RuleRepository ruleRepository;

    public RuleQueryService(RuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    public RulesPageResponse getRulesPage() {
        List<RuleEntity> entities = ruleRepository.findByIsDeletedFalseOrderByIdAsc();

        List<RuleViewResponse> rules = entities.stream()
                .map(this::toRuleView)
                .toList();

        long total = ruleRepository.countByIsDeletedFalse();
        long active = ruleRepository.countByIsDeletedFalseAndIsActiveTrue();
        long inactive = Math.max(0, total - active);

        return new RulesPageResponse(total, active, inactive, rules);
    }

    private RuleViewResponse toRuleView(RuleEntity entity) {
        String severity = entity.getSeverityDefault().name();
        String severityClass = switch (severity) {
            case "HIGH", "CRITICAL" -> "sev-high";
            case "MEDIUM" -> "sev-medium";
            default -> "sev-low";
        };

        String parameter = buildParameter(entity);
        String description = buildDescription(entity);

        return new RuleViewResponse(entity.getName(), description, parameter, severity, severityClass);
    }

    private String buildDescription(RuleEntity entity) {
        if (entity.getDescription() != null && !entity.getDescription().isBlank()) {
            return entity.getDescription();
        }
        return "Rule definition loaded from database.";
    }

    private String buildParameter(RuleEntity entity) {
        String ruleCode = entity.getRuleCode();
        String config = entity.getConfigJson();

        if ("AMOUNT_THRESHOLD".equals(ruleCode)) {
            return "amount > " + valueOrDb(config, "thresholdAmount");
        }

        if ("VELOCITY_CHECK".equals(ruleCode)) {
            return "> " + valueOrDb(config, "maxTransactions") + " txns in "
                    + valueOrDb(config, "windowMinutes") + " mins";
        }

        if ("NEW_PAYEE".equals(ruleCode)) {
            return "First txn to any payee";
        }

        if ("DAILY_LIMIT".equals(ruleCode)) {
            return "Daily total > " + valueOrDb(config, "dailyLimitAmount");
        }

        return "Configured in DB";
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
}



