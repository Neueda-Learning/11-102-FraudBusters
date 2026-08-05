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
 * Incoming payload for updating rule fields from the configure flow.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleUpdateRequestDTO {

    @Size(max = 500)
    private String description;

    /** JSON string containing rule-specific thresholds/config */
    @NotBlank
    private String configJson;

    @NotNull
    private SeverityLevel severityDefault;

    @NotNull
    private Boolean isActive;
}

