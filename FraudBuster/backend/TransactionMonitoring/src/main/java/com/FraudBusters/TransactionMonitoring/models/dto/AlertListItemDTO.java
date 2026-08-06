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
 * One alert row payload used by GET /api/alerts for list + right-side detail preview.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertListItemDTO {

    private String alertCode;

    private SeverityLevel severity;
    private String severityClass;

    private String ruleName;

    private AlertStatus status;
    private String statusClass;

    private String accountId;
    private BigDecimal amount;
    private String payeeId;
    private LocalDateTime createdAt;

    /** Customer contact details for operator use during investigation. */
    private String customerFullName;
    private String customerEmail;
    private String customerPhone;

    private String relatedTxnId;
    private BigDecimal relatedTxnAmount;
    private LocalDateTime relatedTxnTime;
}
