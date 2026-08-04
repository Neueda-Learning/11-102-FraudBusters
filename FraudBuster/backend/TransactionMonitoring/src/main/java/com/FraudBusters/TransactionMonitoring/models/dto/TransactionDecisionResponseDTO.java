package com.FraudBusters.TransactionMonitoring.models.dto;

import com.FraudBusters.TransactionMonitoring.models.enums.DecisionType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response DTO for transaction hold/release/decline decisions.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDecisionResponseDTO {

    private Long id;
    private Long transactionId;
    private String txnId;

    /** Populated only when the decision is linked to a specific alert */
    private Long alertId;
    private String alertCode;

    private DecisionType decision;
    private String decidedBy;
    private String decisionReason;
    private LocalDateTime decidedAt;
}

