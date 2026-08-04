package com.FraudBusters.TransactionMonitoring.models.dto;

import com.FraudBusters.TransactionMonitoring.models.enums.AlertActionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Incoming payload when an operator or system records an action on an alert.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertActionRequestDTO {

    /** ID of the alert this action belongs to */
    @NotNull
    private Long alertId;

    /** Optional: ID of a specific transaction this action relates to */
    private Long transactionId;

    @NotNull
    private AlertActionType actionType;

    @NotBlank
    @Size(max = 100)
    private String performedBy;

    @Size(max = 1000)
    private String actionNote;
}

