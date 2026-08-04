package com.FraudBusters.TransactionMonitoring.models.dto;

import com.FraudBusters.TransactionMonitoring.models.enums.DecisionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Incoming payload when an operator or the system records a hold/release/decline decision.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDecisionRequestDTO {

    @NotNull
    private Long transactionId;

    /** Optional: link this decision to a specific alert */
    private Long alertId;

    @NotNull
    private DecisionType decision;

    @NotBlank
    @Size(max = 100)
    private String decidedBy;

    @NotBlank
    @Size(max = 500)
    private String decisionReason;
}

