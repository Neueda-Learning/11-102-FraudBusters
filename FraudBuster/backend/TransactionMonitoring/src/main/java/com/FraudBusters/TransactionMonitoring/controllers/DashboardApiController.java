package com.FraudBusters.TransactionMonitoring.controllers;

import com.FraudBusters.TransactionMonitoring.dto.DashboardSummaryResponse;
import com.FraudBusters.TransactionMonitoring.dto.RecentAlertResponse;
import com.FraudBusters.TransactionMonitoring.services.DashboardQueryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardApiController {

    private final DashboardQueryService dashboardQueryService;

    public DashboardApiController(DashboardQueryService dashboardQueryService) {
        this.dashboardQueryService = dashboardQueryService;
    }

    @GetMapping("/summary")
    public DashboardSummaryResponse getSummary() {
        return dashboardQueryService.getSummary();
    }

    @GetMapping("/recent-alerts")
    public List<RecentAlertResponse> getRecentAlerts() {
        return dashboardQueryService.getRecentAlerts();
    }
}

