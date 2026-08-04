package com.FraudBusters.TransactionMonitoring.dto;

import java.time.LocalDateTime;

public record AlertItemDto(
        String alertCode,
        String severity,
        String severityClass,
        String ruleName,
        String status,
        String statusClass,
        String accountId,
        String amount,
        String payeeId,
        LocalDateTime createdAt,
        String threshold,
        String relatedTxnId,
        String relatedTxnAmount,
        String relatedTxnTime
) {
}

