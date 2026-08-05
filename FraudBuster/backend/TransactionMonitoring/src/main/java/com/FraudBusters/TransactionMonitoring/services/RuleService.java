package com.FraudBusters.TransactionMonitoring.services;

import com.FraudBusters.TransactionMonitoring.exceptions.ResourceNotFoundException;
import com.FraudBusters.TransactionMonitoring.models.RuleEntity;
import com.FraudBusters.TransactionMonitoring.models.dto.RuleListItemDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.RuleResponseDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.RulesListResponseDTO;
import com.FraudBusters.TransactionMonitoring.repository.RuleEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RuleService {

    private final RuleEntityRepository ruleEntityRepository;

    /**
     * Returns all active (non-deleted) rules for the frontend rules list screen.
     */
    public List<RuleResponseDTO> getAllActiveRules() {
        return ruleEntityRepository.findByIsActiveTrueAndIsDeletedFalse()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Returns all non-deleted rules in a UI-ready payload for rules.html.
     */
    public RulesListResponseDTO getRulesPageData() {
        List<RuleEntity> rules = ruleEntityRepository.findByIsDeletedFalse();

        int total = rules.size();
        int active = (int) rules.stream().filter(r -> Boolean.TRUE.equals(r.getIsActive())).count();

        List<RuleListItemDTO> items = rules.stream()
                .map(this::toListItemDTO)
                .collect(Collectors.toList());

        return RulesListResponseDTO.builder()
                .totalRules(total)
                .activeRules(active)
                .inactiveRules(total - active)
                .rules(items)
                .build();
    }

    /**
     * Returns a single rule by its ruleCode for the rule detail view.
     */
    public RuleResponseDTO getRuleByCode(String ruleCode) {
        RuleEntity rule = ruleEntityRepository.findByRuleCodeAndIsDeletedFalse(ruleCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rule not found with code: " + ruleCode));
        return toDTO(rule);
    }

    // ----------------------------------------------------------------
    //  Mapper
    // ----------------------------------------------------------------

    private RuleResponseDTO toDTO(RuleEntity entity) {
        return RuleResponseDTO.builder()
                .id(entity.getId())
                .ruleCode(entity.getRuleCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .ruleType(entity.getRuleType())
                .severityDefault(entity.getSeverityDefault())
                .inlineMode(entity.getInlineMode())
                .isActive(entity.getIsActive())
                .build();
    }

    private RuleListItemDTO toListItemDTO(RuleEntity entity) {
        String severity = entity.getSeverityDefault() == null ? "LOW" : entity.getSeverityDefault().name();

        return RuleListItemDTO.builder()
                .ruleCode(entity.getRuleCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .parameter(entity.getConfigJson())
                .severity(severity)
                .severityClass(toSeverityClass(severity))
                .isActive(Boolean.TRUE.equals(entity.getIsActive()))
                .build();
    }

    private String toSeverityClass(String severity) {
        if ("HIGH".equals(severity) || "CRITICAL".equals(severity)) {
            return "sev-high";
        }
        if ("MEDIUM".equals(severity)) {
            return "sev-medium";
        }
        return "sev-low";
    }
}

