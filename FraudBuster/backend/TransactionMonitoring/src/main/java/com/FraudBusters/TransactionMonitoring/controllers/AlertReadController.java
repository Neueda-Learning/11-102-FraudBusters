package com.FraudBusters.TransactionMonitoring.controllers;

import com.FraudBusters.TransactionMonitoring.models.dto.AlertHistoryListResponseDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.AlertTimelineResponseDTO;
import com.FraudBusters.TransactionMonitoring.services.AlertReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only alert APIs used by frontend list/detail history screens.
 *
 * Note: This controller is intentionally separated from AlertLifecycleController
 * so read endpoints remain isolated from status mutation endpoints.
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertReadController {

    private final AlertReadService alertReadService;

    /**
     * Returns closed and dismissed alerts for the Alert History screen.
     * Endpoint: GET /api/alerts/history
     */
    @GetMapping("/history")
    public ResponseEntity<AlertHistoryListResponseDTO> getTerminalAlertHistory() {
        return ResponseEntity.ok(alertReadService.getTerminalAlertHistory());
    }

    /**
     * Returns status transition timeline for one alert code.
     * Endpoint: GET /api/alerts/{alertCode}/history
     */
    @GetMapping("/{alertCode}/history")
    public ResponseEntity<AlertTimelineResponseDTO> getAlertStatusHistory(@PathVariable String alertCode) {
        return ResponseEntity.ok(alertReadService.getAlertTimeline(alertCode));
    }
}
