package com.FraudBusters.TransactionMonitoring.models.dto;

import com.FraudBusters.TransactionMonitoring.models.enums.AlertStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Read-only DTO for the alert status transition audit trail.
 * This record is always system/operator generated; no request DTO is needed.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertStatusHistoryResponseDTO {

    private Long id;
    private Long alertId;

    /** NULL for the initial OPEN entry */
    private AlertStatus oldStatus;
    private AlertStatus newStatus;
    private String changedBy;
    private String changeReason;
    private LocalDateTime changedAt;
}

