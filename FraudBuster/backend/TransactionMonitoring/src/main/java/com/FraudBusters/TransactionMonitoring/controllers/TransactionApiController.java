package com.FraudBusters.TransactionMonitoring.controllers;

import com.FraudBusters.TransactionMonitoring.dto.TransactionsPageDto;
import com.FraudBusters.TransactionMonitoring.services.TransactionQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionApiController {

    private final TransactionQueryService transactionQueryService;

    public TransactionApiController(TransactionQueryService transactionQueryService) {
        this.transactionQueryService = transactionQueryService;
    }

    @GetMapping
    public TransactionsPageDto getTransactionsPage() {
        return transactionQueryService.getTransactionsPage();
    }
}

