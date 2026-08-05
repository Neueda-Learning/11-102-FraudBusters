package com.FraudBusters.TransactionMonitoring.controllers;

import com.FraudBusters.TransactionMonitoring.models.dto.RuleResponseDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.RulesListResponseDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.RuleUpdateRequestDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.RuleListItemDTO;
import com.FraudBusters.TransactionMonitoring.services.RuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

/**
 * REST endpoints for the Operator Rules screen on the frontend.
 *
 * GET /api/rules          → list all active rules
 * GET /api/rules/{code}   → single rule detail by ruleCode
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/rules")
@RequiredArgsConstructor
public class RuleController {

    private final RuleService ruleService;

    /**
     * Returns all non-deleted rules with top-card counts.
     * Frontend uses this to populate rules summary cards and rule cards.
     *
     * Response example:
     * [
     *   {
     *     "id": 1,
     *     "ruleCode": "AMOUNT_THRESHOLD",
     *     "name": "Amount Threshold Rule",
     *     "description": "Trigger alert when a single transaction exceeds ...",
     *     "ruleType": "AMOUNT",
     *     "severityDefault": "HIGH",
     *     "inlineMode": "INLINE",
     *     "isActive": true
     *   }, ...
     * ]
     */
    @GetMapping
    public ResponseEntity<RulesListResponseDTO> getAllRules() {
        return ResponseEntity.ok(ruleService.getRulesPageData());
    }

    /**
     * Returns a single rule by its ruleCode.
     * Frontend uses this for the Rule Detail / hover-card view.
     *
     * @param ruleCode  e.g. "VELOCITY_CHECK"
     */
    @GetMapping("/{ruleCode}")
    public ResponseEntity<RuleResponseDTO> getRuleByCode(@PathVariable String ruleCode) {
        return ResponseEntity.ok(ruleService.getRuleByCode(ruleCode));
    }

    /**
     * Updates rule configuration fields from the frontend configure flow.
     *
     * Endpoint: PUT /api/rules/{ruleCode}
     * Required header: X-Operator-Password (operator authentication)
     * Request body: editable fields
     *
     * @param ruleCode  e.g. "AMOUNT_THRESHOLD"
     * @param request   DTO with description, configJson, severityDefault, isActive
     * @param operatorPassword operator password from header
     * @return updated rule as RuleListItemDTO
     */
    @PutMapping("/{ruleCode}")
    public ResponseEntity<RuleListItemDTO> updateRule(
            @PathVariable String ruleCode,
            @Valid @RequestBody RuleUpdateRequestDTO request,
            @RequestHeader(value = "X-Operator-Password", required = false) String operatorPassword) {
        try {
            RuleListItemDTO updated = ruleService.updateRuleForConfigure(ruleCode, request, operatorPassword);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().build();
        }
    }
}

