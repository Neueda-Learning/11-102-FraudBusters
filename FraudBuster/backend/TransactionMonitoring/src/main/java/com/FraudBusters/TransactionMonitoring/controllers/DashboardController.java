package com.FraudBusters.TransactionMonitoring.controllers;

import com.FraudBusters.TransactionMonitoring.models.dto.DashboardRecentAlertItemDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.DashboardSummaryResponseDTO;
import com.FraudBusters.TransactionMonitoring.services.DashboardService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard APIs used by frontend summary cards and recent open alerts table.
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /** Endpoint: GET /api/dashboard/summary */
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponseDTO> getSummary() {
        return ResponseEntity.ok(dashboardService.getDashboardSummary());
    }

    /** Endpoint: GET /api/dashboard/recent-alerts */
    @GetMapping("/recent-alerts")
    public ResponseEntity<List<DashboardRecentAlertItemDTO>> getRecentOpenAlerts() {
        return ResponseEntity.ok(dashboardService.getRecentOpenAlerts());
    }
}

