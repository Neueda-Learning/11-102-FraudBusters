package com.FraudBusters.TransactionMonitoring.controllers;

import com.FraudBusters.TransactionMonitoring.exceptions.ResourceNotFoundException;
import com.FraudBusters.TransactionMonitoring.models.dto.CentralRuleEvaluationResponseDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.TransactionRequestDTO;
import com.FraudBusters.TransactionMonitoring.services.Impl.AmountThresholdRuleEngineServiceImpl;
import com.FraudBusters.TransactionMonitoring.services.Impl.CentralRuleEngineServiceImpl;
import com.FraudBusters.TransactionMonitoring.services.Impl.DailyLimitRuleEngineServiceImpl;
import com.FraudBusters.TransactionMonitoring.services.Impl.NewPayeeRuleEngineImpl;
import com.FraudBusters.TransactionMonitoring.services.Impl.VelocityCheckRuleEngineImpl;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rule-engine")
public class RuleEngineServiceController {

    private final AmountThresholdRuleEngineServiceImpl amountThresholdRuleEngineService;
    private final DailyLimitRuleEngineServiceImpl dailyLimitRuleEngineService;
    private final NewPayeeRuleEngineImpl newPayeeRuleEngine;
    private final VelocityCheckRuleEngineImpl velocityCheckRuleEngine;
    private final CentralRuleEngineServiceImpl centralRuleEngineService;

    public RuleEngineServiceController(
            AmountThresholdRuleEngineServiceImpl amountThresholdRuleEngineService,
            DailyLimitRuleEngineServiceImpl dailyLimitRuleEngineService,
            NewPayeeRuleEngineImpl newPayeeRuleEngine,
            VelocityCheckRuleEngineImpl velocityCheckRuleEngine,
            CentralRuleEngineServiceImpl centralRuleEngineService) {
        this.amountThresholdRuleEngineService = amountThresholdRuleEngineService;
        this.dailyLimitRuleEngineService = dailyLimitRuleEngineService;
        this.newPayeeRuleEngine = newPayeeRuleEngine;
        this.velocityCheckRuleEngine = velocityCheckRuleEngine;
        this.centralRuleEngineService = centralRuleEngineService;
    }


    @PostMapping("/evaluate/amount-threshold")
    public ResponseEntity<Boolean> evaluateAmountThreshold(@Valid @RequestBody TransactionRequestDTO transactionRequestDTO) {
        return ResponseEntity.ok(amountThresholdRuleEngineService.evaluateTransaction(transactionRequestDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Amount threshold evaluation could not be completed")));
    }

    @PostMapping("/evaluate/daily-limit")
    public ResponseEntity<Boolean> evaluateDailyLimit(@Valid @RequestBody TransactionRequestDTO transactionRequestDTO) {
        return ResponseEntity.ok(dailyLimitRuleEngineService.evaluateTransaction(transactionRequestDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Daily limit evaluation could not be completed")));
    }

    @PostMapping("/evaluate/new-payee")
    public ResponseEntity<Boolean> evaluateNewPayee(@Valid @RequestBody TransactionRequestDTO transactionRequestDTO) {
        return ResponseEntity.ok(newPayeeRuleEngine.evaluateTransaction(transactionRequestDTO)
                .orElseThrow(() -> new ResourceNotFoundException("New payee evaluation could not be completed")));
    }

    @PostMapping("/evaluate/velocity-check")
    public ResponseEntity<Boolean> evaluateVelocityCheck(@Valid @RequestBody TransactionRequestDTO transactionRequestDTO) {
        return ResponseEntity.ok(velocityCheckRuleEngine.evaluateTransaction(transactionRequestDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Velocity evaluation could not be completed")));
    }

    @PostMapping("/evaluate/central")
    public ResponseEntity<CentralRuleEvaluationResponseDTO> evaluateCentral(@Valid @RequestBody TransactionRequestDTO transactionRequestDTO) {
        return ResponseEntity.ok(java.util.Optional.ofNullable(centralRuleEngineService.evaluateAgainstActiveRules(transactionRequestDTO))
                .orElseThrow(() -> new ResourceNotFoundException("Central rule evaluation could not be completed")));
    }

    @PostMapping("/evaluate/central/pending")
    public List<CentralRuleEvaluationResponseDTO> evaluatePendingCentral() {
        return centralRuleEngineService.evaluatePendingTransactionsBatch();
    }



}
