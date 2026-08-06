package com.FraudBusters.TransactionMonitoring.models.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response envelope for GET /api/alerts used by alerts.html list filters and counts.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertListResponseDTO {

    private int page;
    private int size;
    private int totalPages;
    private long totalElements;
    private boolean hasNext;
    private boolean hasPrevious;

    private long totalAlerts;
    private long openAlerts;
    private long acknowledgedAlerts;
    private long investigatingAlerts;

    private List<AlertListItemDTO> alerts;
}
