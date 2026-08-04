package com.FraudBusters.TransactionMonitoring.dto;

import java.util.List;

public record AlertsPageDto(
        long totalAlerts,
        long openAlerts,
        long acknowledgedAlerts,
        long investigatingAlerts,
        List<AlertItemDto> alerts
) {
}

