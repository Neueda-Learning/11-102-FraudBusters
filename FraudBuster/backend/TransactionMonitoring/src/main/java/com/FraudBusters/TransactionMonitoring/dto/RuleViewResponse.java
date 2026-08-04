package com.FraudBusters.TransactionMonitoring.dto;

public record RuleViewResponse(
        String name,
        String description,
        String parameter,
        String severity,
        String severityClass
) {
}


