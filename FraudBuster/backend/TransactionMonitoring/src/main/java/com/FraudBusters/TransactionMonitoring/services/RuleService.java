package com.FraudBusters.TransactionMonitoring.services;

import com.FraudBusters.TransactionMonitoring.exceptions.ResourceNotFoundException;
import com.FraudBusters.TransactionMonitoring.models.RuleEntity;
import com.FraudBusters.TransactionMonitoring.models.dto.RuleResponseDTO;
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
}

