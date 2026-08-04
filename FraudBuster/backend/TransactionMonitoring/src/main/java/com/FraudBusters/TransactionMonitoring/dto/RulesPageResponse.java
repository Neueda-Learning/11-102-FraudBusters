package com.FraudBusters.TransactionMonitoring.dto;

import java.util.List;

public record RulesPageResponse(
        long totalRules,
        long activeRules,
        long inactiveRules,
        List<RuleViewResponse> rules
) {
}

