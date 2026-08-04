package com.FraudBusters.TransactionMonitoring.controllers;

import com.FraudBusters.TransactionMonitoring.models.TransactionEntity;
import com.FraudBusters.TransactionMonitoring.services.RuleEngineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rule-engine")
public class RuleEngineServiceController {

    @Autowired
    private RuleEngineService ruleEngineService;


    @PostMapping("/evaluate/amount-threshold")
    public Boolean evaluateAmountThreshold(@RequestBody TransactionEntity transactionEntity) {
        return ruleEngineService.evaluateTransactionUsingAmountThreshold(transactionEntity);

    }



}
