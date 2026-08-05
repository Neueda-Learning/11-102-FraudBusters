package com.FraudBusters.TransactionMonitoring.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response envelope for GET /api/alerts/history.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertHistoryListResponseDTO {

    private int total;
    private List<AlertHistoryListItemDTO> history;
}

