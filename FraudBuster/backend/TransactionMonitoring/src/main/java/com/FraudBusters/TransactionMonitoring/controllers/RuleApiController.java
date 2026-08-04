package com.FraudBusters.TransactionMonitoring.controllers;

import com.FraudBusters.TransactionMonitoring.dto.RulesPageResponse;
import com.FraudBusters.TransactionMonitoring.services.RuleQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rules")
public class RuleApiController {

    private final RuleQueryService ruleQueryService;

    public RuleApiController(RuleQueryService ruleQueryService) {
        this.ruleQueryService = ruleQueryService;
    }

    @GetMapping
    public RulesPageResponse getRules() {
        return ruleQueryService.getRulesPage();
    }
}

