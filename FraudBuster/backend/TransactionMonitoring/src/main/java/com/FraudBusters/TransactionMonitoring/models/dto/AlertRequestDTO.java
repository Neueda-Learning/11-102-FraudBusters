package com.FraudBusters.TransactionMonitoring.models.dto;

import com.FraudBusters.TransactionMonitoring.models.enums.SeverityLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Incoming payload for creating a new alert (typically triggered by the rule engine).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRequestDTO {

    @NotBlank
    @Size(max = 64)
    private String alertCode;

    /** ID of the rule that triggered this alert */
    @NotNull
    private Long ruleId;

    @NotNull
    private SeverityLevel severity;

    @NotBlank
    @Size(max = 150)
    private String title;

    @Size(max = 1000)
    private String description;
}

