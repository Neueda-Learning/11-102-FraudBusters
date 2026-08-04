package com.FraudBusters.TransactionMonitoring.dto;

import java.time.LocalDateTime;

public record TransactionItemDto(
        String txnId,
        String accountId,
        String payeeId,
        String amount,
        String txnType,
        LocalDateTime txnTimestamp,
        boolean hasAlert,
        String alertSeverity,
        String alertLabel
) {
}

