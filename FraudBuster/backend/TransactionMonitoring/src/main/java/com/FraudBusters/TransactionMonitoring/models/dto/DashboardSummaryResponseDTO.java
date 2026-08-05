package com.FraudBusters.TransactionMonitoring.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Summary cards payload for GET /api/dashboard/summary.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponseDTO {

    private Long openAlerts;
    private Long acknowledgedAlerts;
    private Long transactionsToday;

    /**
     * Backward-compatible key used by current frontend card binding.
     * Value represents resolved alerts count (CLOSED + DISMISSED) for today.
     */
    private Long closedToday;
}

