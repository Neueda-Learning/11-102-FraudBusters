package com.FraudBusters.TransactionMonitoring.dto;

import java.util.List;

public record TransactionsPageDto(
        long totalTransactions,
        String totalVolume,
        long alertsTriggered,
        List<TransactionItemDto> transactions
) {
}

