package com.FraudBusters.TransactionMonitoring.dto;

import java.time.LocalDateTime;

public record RecentAlertResponse(
        String alertCode,
        String severity,
        String ruleName,
        String status,
        LocalDateTime createdAt
) {
}

