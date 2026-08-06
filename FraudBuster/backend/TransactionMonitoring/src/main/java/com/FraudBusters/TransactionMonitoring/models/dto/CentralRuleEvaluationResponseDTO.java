package com.FraudBusters.TransactionMonitoring.models.dto;

import com.FraudBusters.TransactionMonitoring.models.enums.FinalDecision;
import com.FraudBusters.TransactionMonitoring.models.enums.MonitorState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CentralRuleEvaluationResponseDTO {

    private String txnId;
    private boolean anyRuleTriggered;
    private int evaluatedRuleCount;
    private Map<String, Boolean> ruleResults;
    private List<String> skippedRuleCodes;
    private MonitorState monitorState;
    private FinalDecision finalDecision;
    private String decisionReason;
}

