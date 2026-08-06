package com.FraudBusters.TransactionMonitoring.models.dto;

import com.FraudBusters.TransactionMonitoring.models.enums.AlertStatus;
import com.FraudBusters.TransactionMonitoring.models.enums.SeverityLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * One row item for the Alert History table (terminal alerts only).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertHistoryListItemDTO {

    private String alertCode;
    private SeverityLevel severity;
    private String ruleName;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private AlertStatus finalStatus;
    private String notes;
}

