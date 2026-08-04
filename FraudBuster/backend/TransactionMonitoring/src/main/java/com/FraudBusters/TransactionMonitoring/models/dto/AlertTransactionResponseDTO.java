package com.FraudBusters.TransactionMonitoring.models.dto;

import com.FraudBusters.TransactionMonitoring.models.enums.AlertRelationType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Read-only DTO for the many-to-many link between an alert and its transactions.
 * Links are created internally; no request DTO is needed from the frontend.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertTransactionResponseDTO {

    private Long id;
    private Long alertId;
    private String alertCode;
    private Long transactionId;
    private String txnId;

    /** TRIGGERING_TRANSACTION or RELATED_TRANSACTION */
    private AlertRelationType relationType;
    private LocalDateTime createdAt;
}

