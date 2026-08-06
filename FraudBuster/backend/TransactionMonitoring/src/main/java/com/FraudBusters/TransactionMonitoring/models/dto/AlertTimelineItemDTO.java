package com.FraudBusters.TransactionMonitoring.models.dto;

import com.FraudBusters.TransactionMonitoring.models.enums.AlertStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * One transition event shown in the per-alert timeline dialog.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertTimelineItemDTO {

    private AlertStatus oldStatus;
    private AlertStatus newStatus;
    private String changedBy;
    private String changeReason;
    private LocalDateTime changedAt;
}

