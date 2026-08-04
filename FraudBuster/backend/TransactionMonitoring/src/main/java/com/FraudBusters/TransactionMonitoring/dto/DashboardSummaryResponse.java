package com.FraudBusters.TransactionMonitoring.dto;

public record DashboardSummaryResponse(
        long openAlerts,
        long acknowledgedAlerts,
        long transactionsToday,
        long closedToday
) {
}

