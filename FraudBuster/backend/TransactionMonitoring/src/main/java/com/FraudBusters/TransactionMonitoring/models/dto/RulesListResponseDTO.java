package com.FraudBusters.TransactionMonitoring.models.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Rules page response shape expected by frontend.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RulesListResponseDTO {

    private Integer totalRules;
    private Integer activeRules;
    private Integer inactiveRules;
    private List<RuleListItemDTO> rules;
}

