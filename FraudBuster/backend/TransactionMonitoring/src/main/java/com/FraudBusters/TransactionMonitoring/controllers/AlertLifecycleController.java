package com.FraudBusters.TransactionMonitoring.controllers;

import com.FraudBusters.TransactionMonitoring.exceptions.InvalidLifecycleActionException;
import com.FraudBusters.TransactionMonitoring.services.AlertLifecycleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for operator-driven alert lifecycle actions.
 *
 * This controller only orchestrates HTTP input/output.
 * All transition validation and database writes stay in AlertLifecycleService.
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Validated
public class AlertLifecycleController {

    private static final String DEFAULT_DECIDED_BY = "operator-1";
    private static final String DEFAULT_ACKNOWLEDGE_REASON = "Operator acknowledged the alert.";
    private static final String DEFAULT_INVESTIGATE_REASON = "Operator started investigation.";
    private static final String DEFAULT_CLOSE_REASON = "Alert closed by operator (default reason).";
    private static final String DEFAULT_DISMISS_REASON = "Alert dismissed as false positive (default reason).";

    private final AlertLifecycleService alertLifecycleService;

    /**
     * Marks an OPEN alert as ACKNOWLEDGED.
     *
     * Endpoint: POST /api/alerts/{alertCode}/acknowledge
     */
    @PostMapping("/{alertCode}/acknowledge")
    public ResponseEntity<String> acknowledgeAlert(@PathVariable @NotBlank @Size(max = 64) String alertCode,
                                                   @Valid @RequestBody(required = false) AlertLifecycleRequest request) {
        String decidedBy = resolveDecidedBy(request);
        String reason = resolveReason(request, DEFAULT_ACKNOWLEDGE_REASON);
        alertLifecycleService.acknowledgeAlert(alertCode, reason, decidedBy);
        return ResponseEntity.ok("Alert acknowledged successfully.");
    }

    /**
     * Marks an ACKNOWLEDGED alert as INVESTIGATING.
     *
     * Endpoint: POST /api/alerts/{alertCode}/investigate
     */
    @PostMapping("/{alertCode}/investigate")
    public ResponseEntity<String> investigateAlert(@PathVariable @NotBlank @Size(max = 64) String alertCode,
                                                   @Valid @RequestBody(required = false) AlertLifecycleRequest request) {
        String decidedBy = resolveDecidedBy(request);
        String reason = resolveReason(request, DEFAULT_INVESTIGATE_REASON);
        alertLifecycleService.investigateAlert(alertCode, reason, decidedBy);
        return ResponseEntity.ok("Alert moved to investigating successfully.");
    }

    /**
     * Closes an alert after fraud confirmation.
     *
     * Endpoint: POST /api/alerts/{alertCode}/close
     * Request body requires a mandatory reason and actor (decidedBy).
     */
    @PostMapping("/{alertCode}/close")
    public ResponseEntity<String> closeAlert(@PathVariable @NotBlank @Size(max = 64) String alertCode,
                                             @Valid @RequestBody(required = false) AlertLifecycleRequest request) {
        String decidedBy = resolveDecidedBy(request);
        String reason = resolveRequiredReason(request, "close");
        alertLifecycleService.closeAlert(alertCode, reason, decidedBy);
        return ResponseEntity.ok("Alert closed successfully.");
    }

    /**
     * Dismisses an alert as false positive.
     *
     * Endpoint: POST /api/alerts/{alertCode}/dismiss
     * Request body requires a mandatory reason and actor (decidedBy).
     */
    @PostMapping("/{alertCode}/dismiss")
    public ResponseEntity<String> dismissAlert(@PathVariable @NotBlank @Size(max = 64) String alertCode,
                                               @Valid @RequestBody(required = false) AlertLifecycleRequest request) {
        String decidedBy = resolveDecidedBy(request);
        String reason = resolveRequiredReason(request, "dismiss");
        alertLifecycleService.dismissAlert(alertCode, reason, decidedBy);
        return ResponseEntity.ok("Alert dismissed successfully.");
    }

    /**
     * Uses frontend-sent operator when present; otherwise applies MVP default.
     */
    private String resolveDecidedBy(AlertLifecycleRequest request) {
        if (request == null || request.getDecidedBy() == null || request.getDecidedBy().isBlank()) {
            return DEFAULT_DECIDED_BY;
        }
        return request.getDecidedBy().trim();
    }

    /**
     * Uses frontend-sent reason when present; otherwise applies endpoint-specific default.
     */
    private String resolveReason(AlertLifecycleRequest request, String defaultReason) {
        if (request == null || request.getReason() == null || request.getReason().isBlank()) {
            return defaultReason;
        }
        return request.getReason().trim();
    }

    /**
     * Enforces mandatory reason for terminal actions.
     */
    private String resolveRequiredReason(AlertLifecycleRequest request, String actionName) {
        if (request == null || request.getReason() == null || request.getReason().isBlank()) {
            throw new InvalidLifecycleActionException(
                    "Reason is required for " + actionName + " action.");
        }
        return request.getReason().trim();
    }

    /**
     * Minimal request payload used by alert lifecycle actions.
     * Keeps controller contract explicit without mixing persistence models into API.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlertLifecycleRequest {

        @Size(max = 500)
        private String reason;

        @Size(max = 100)
        private String decidedBy;
    }
}
