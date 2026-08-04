package com.FraudBusters.TransactionMonitoring.models.dto;

import com.FraudBusters.TransactionMonitoring.models.enums.AlertStatus;
import com.FraudBusters.TransactionMonitoring.models.enums.SeverityLevel;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response DTO returned to the frontend for the Alerts list / detail screen.
 * Exposes rule info as a flat nested summary to avoid circular serialisation.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponseDTO {

    private Long id;
    private String alertCode;

    /** Flat summary of the rule that fired this alert */
    private Long ruleId;
    private String ruleCode;
    private String ruleName;

    private SeverityLevel severity;
    private AlertStatus status;
    private BigDecimal riskScore;
    private String title;
    private String description;
    private LocalDateTime holdExpiresAt;
    private LocalDateTime closedAt;
    private String closureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

