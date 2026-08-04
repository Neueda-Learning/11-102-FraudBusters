package com.FraudBusters.TransactionMonitoring.models.dto;

import com.FraudBusters.TransactionMonitoring.models.enums.AlertActionType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response DTO returned for the alert actions investigation diary.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertActionResponseDTO {

    private Long id;
    private Long alertId;

    /** Populated only when the action targets a specific transaction */
    private Long transactionId;
    private String transactionTxnId;

    private AlertActionType actionType;
    private String performedBy;
    private String actionNote;
    private LocalDateTime performedAt;
}

