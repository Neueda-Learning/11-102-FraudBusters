package com.FraudBusters.TransactionMonitoring.controllers;

import com.FraudBusters.TransactionMonitoring.dto.AlertsPageDto;
import com.FraudBusters.TransactionMonitoring.dto.AlertHistoryPageDto;
import com.FraudBusters.TransactionMonitoring.services.AlertQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
public class AlertApiController {

    private final AlertQueryService alertQueryService;

    public AlertApiController(AlertQueryService alertQueryService) {
        this.alertQueryService = alertQueryService;
    }

    @GetMapping
    public AlertsPageDto getAlertsPage() {
        return alertQueryService.getAlertsPage();
    }

    @GetMapping("/history")
    public AlertHistoryPageDto getAlertsHistoryPage() {
        return alertQueryService.getHistoryPage();
    }
}


