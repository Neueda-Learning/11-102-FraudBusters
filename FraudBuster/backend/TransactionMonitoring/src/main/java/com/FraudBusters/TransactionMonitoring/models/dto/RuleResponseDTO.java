package com.FraudBusters.TransactionMonitoring.models.dto;

import com.FraudBusters.TransactionMonitoring.models.enums.InlineMode;
import com.FraudBusters.TransactionMonitoring.models.enums.SeverityLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO returned to the frontend for the Rules management screen.
 * Only fields that an operator needs to see are exposed here.
 * Internal fields like config_json, is_deleted are intentionally excluded.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleResponseDTO {

    private Long id;

    /** Machine-readable stable identifier, e.g. "VELOCITY_CHECK" */
    private String ruleCode;

    /** Human-friendly display name shown in the rules table */
    private String name;

    /** Full description of what this rule does */
    private String description;

    /** Category of the rule: VELOCITY, AMOUNT, PATTERN, etc. */
    private String ruleType;

    /** Default severity level when this rule fires: LOW / MEDIUM / HIGH / CRITICAL */
    private SeverityLevel severityDefault;

    /** When the rule fires: INLINE (can hold transaction) or POST_AUTH */
    private InlineMode inlineMode;

    /** Whether this rule is currently active */
    private Boolean isActive;
}

