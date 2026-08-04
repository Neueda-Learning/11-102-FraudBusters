package com.FraudBusters.TransactionMonitoring.dto;

import java.util.List;

public record AlertHistoryPageDto(
        long totalHistoricalAlerts,
        List<AlertHistoryItemDto> history
) {
}

