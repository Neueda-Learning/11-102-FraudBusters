package com.FraudBusters.TransactionMonitoring.models.dto;

import com.FraudBusters.TransactionMonitoring.models.enums.InlineMode;
import com.FraudBusters.TransactionMonitoring.models.enums.SeverityLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Incoming payload for creating or updating a rule from the Rules management screen.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleRequestDTO {

    @NotBlank
    @Size(max = 64)
    private String ruleCode;

    @NotBlank
    @Size(max = 150)
    private String name;

    @Size(max = 500)
    private String description;

    @NotBlank
    @Size(max = 50)
    private String ruleType;

    @NotNull
    private SeverityLevel severityDefault;

    @NotNull
    private InlineMode inlineMode;

    /** JSON string containing rule-specific thresholds/config */
    @NotBlank
    private String configJson;

    @NotNull
    private Boolean isActive;
}

