package com.FraudBusters.TransactionMonitoring.controllers;

import com.FraudBusters.TransactionMonitoring.models.AlertEntity;
import com.FraudBusters.TransactionMonitoring.services.AlertOpsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertOpsController {

    @Autowired
    private AlertOpsService alertOpsService;

    @GetMapping("/all")
    public List<AlertEntity> getAllAlerts() {
        return alertOpsService.getAllAlerts();
    }

    @GetMapping("/active")
    public List<AlertEntity> getActiveAlerts() {
        return alertOpsService.getActiveAlerts();
    }
}
