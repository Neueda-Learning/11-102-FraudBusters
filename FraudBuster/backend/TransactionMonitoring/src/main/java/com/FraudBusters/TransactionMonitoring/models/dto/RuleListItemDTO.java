package com.FraudBusters.TransactionMonitoring.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Rule card payload used by the frontend rules page.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleListItemDTO {

    private String ruleCode;
    private String name;
    private String description;
    private String parameter;
    private String severity;
    private String severityClass;
    private Boolean isActive;
}

