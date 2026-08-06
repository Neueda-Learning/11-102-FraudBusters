package com.FraudBusters.TransactionMonitoring.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response envelope for GET /api/alerts/{alertCode}/history.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertTimelineResponseDTO {

    private String alertCode;
    private int totalTransitions;
    private List<AlertTimelineItemDTO> statusHistory;
}

