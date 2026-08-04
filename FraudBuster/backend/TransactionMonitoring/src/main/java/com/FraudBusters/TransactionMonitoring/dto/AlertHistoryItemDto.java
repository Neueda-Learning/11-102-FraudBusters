package com.FraudBusters.TransactionMonitoring.dto;

import java.time.LocalDateTime;

public record AlertHistoryItemDto(
        String alertCode,
        String severity,
        String severityClass,
        String ruleName,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        String finalStatus,
        String finalStatusClass,
        String notes
) {
}

