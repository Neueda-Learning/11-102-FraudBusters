package com.FraudBusters.TransactionMonitoring.services;

import com.FraudBusters.TransactionMonitoring.exceptions.ResourceNotFoundException;
import com.FraudBusters.TransactionMonitoring.models.RuleEntity;
import com.FraudBusters.TransactionMonitoring.models.dto.RuleListItemDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.RuleResponseDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.RuleUpdateRequestDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.RulesListResponseDTO;
import com.FraudBusters.TransactionMonitoring.repository.RuleEntityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RuleService {

    private final RuleEntityRepository ruleEntityRepository;
    private final ObjectMapper objectMapper;

    @Value("${rules.configure.password:12345}")
    private String configurePassword;

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

    /**
     * Updates rule fields that are editable from the frontend configure flow.
     * This method is controller-agnostic; API wiring can pass password from header.
     */
    @Transactional
    public RuleListItemDTO updateRuleForConfigure(String ruleCode,
                                                  RuleUpdateRequestDTO request,
                                                  String operatorPassword) {
        validateOperatorPassword(operatorPassword);

        if (request == null) {
            throw new IllegalArgumentException("Rule update payload is required.");
        }

        String cleanedConfigJson = validateAndNormalizeConfigJson(request.getConfigJson());

        RuleEntity rule = ruleEntityRepository.findByRuleCodeAndIsDeletedFalse(ruleCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rule not found with code: " + ruleCode));

        rule.setDescription(normalizeNullableText(request.getDescription()));
        rule.setConfigJson(cleanedConfigJson);
        rule.setSeverityDefault(request.getSeverityDefault());
        rule.setIsActive(request.getIsActive());

        RuleEntity saved = ruleEntityRepository.save(rule);
        return toListItemDTO(saved);
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

    private void validateOperatorPassword(String operatorPassword) {
        if (operatorPassword == null || operatorPassword.isBlank()) {
            throw new IllegalArgumentException("Operator password is required.");
        }
        if (!operatorPassword.equals(configurePassword)) {
            throw new IllegalArgumentException("Invalid operator password.");
        }
    }

    private String validateAndNormalizeConfigJson(String configJson) {
        if (configJson == null || configJson.isBlank()) {
            throw new IllegalArgumentException("Rule configJson is required.");
        }

        String cleaned = configJson.trim();
        try {
            objectMapper.readTree(cleaned);
        } catch (Exception exception) {
            throw new IllegalArgumentException("configJson must be valid JSON.");
        }
        return cleaned;
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }
}

