package com.FraudBusters.TransactionMonitoring.models.dto;

import com.FraudBusters.TransactionMonitoring.models.enums.AlertStatus;
import com.FraudBusters.TransactionMonitoring.models.enums.SeverityLevel;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One row item for GET /api/dashboard/recent-alerts.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardRecentAlertItemDTO {

    private String alertCode;
    private SeverityLevel severity;
    private String ruleName;
    private String accountId;
    private LocalDateTime createdAt;
    private AlertStatus status;
}

