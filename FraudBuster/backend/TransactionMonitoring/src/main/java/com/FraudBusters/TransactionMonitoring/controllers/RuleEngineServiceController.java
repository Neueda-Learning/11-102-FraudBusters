package com.FraudBusters.TransactionMonitoring.controllers;

import com.FraudBusters.TransactionMonitoring.models.TransactionEntity;
import com.FraudBusters.TransactionMonitoring.services.Impl.AmountThresholdRuleEngineServiceImpl;
import com.FraudBusters.TransactionMonitoring.services.Impl.DailyLimitRuleEngineServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rule-engine")
public class RuleEngineServiceController {

    @Autowired
    private AmountThresholdRuleEngineServiceImpl amountThresholdRuleEngineService;

    @Autowired
    private DailyLimitRuleEngineServiceImpl dailyLimitRuleEngineService;


    @PostMapping("/evaluate/amount-threshold")
    public Boolean evaluateAmountThreshold(@RequestBody TransactionEntity transactionEntity) {
        return amountThresholdRuleEngineService.evaluateTransaction(transactionEntity);

    }

    @PostMapping("/evaluate/daily-limit")
    public Boolean evaluateDailyLimit(@RequestBody TransactionEntity transactionEntity) {
        return dailyLimitRuleEngineService.evaluateTransaction(transactionEntity);

    }



}
