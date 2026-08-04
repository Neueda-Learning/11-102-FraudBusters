package com.FraudBusters.TransactionMonitoring.services;

import com.FraudBusters.TransactionMonitoring.dto.DashboardSummaryResponse;
import com.FraudBusters.TransactionMonitoring.dto.RecentAlertResponse;
import com.FraudBusters.TransactionMonitoring.models.AlertEntity;
import com.FraudBusters.TransactionMonitoring.models.enums.AlertStatus;
import com.FraudBusters.TransactionMonitoring.repository.AlertRepository;
import com.FraudBusters.TransactionMonitoring.repository.TransactionRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DashboardQueryService {

    private final AlertRepository alertRepository;
    private final TransactionRepository transactionRepository;

    public DashboardQueryService(AlertRepository alertRepository, TransactionRepository transactionRepository) {
        this.alertRepository = alertRepository;
        this.transactionRepository = transactionRepository;
    }

    public DashboardSummaryResponse getSummary() {
        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();

        long openAlerts = alertRepository.countByStatus(AlertStatus.OPEN);
        long acknowledged = alertRepository.countByStatus(AlertStatus.ACKNOWLEDGED);
        long closedToday = alertRepository.countByStatusAndClosedAtBetween(AlertStatus.CLOSED, dayStart, dayEnd);
        long transactionsToday = transactionRepository.countByTxnTimestampBetween(dayStart, dayEnd);

        return new DashboardSummaryResponse(openAlerts, acknowledged, transactionsToday, closedToday);
    }

    public List<RecentAlertResponse> getRecentAlerts() {
        List<AlertEntity> alerts = alertRepository.findTop10ByStatusInOrderByCreatedAtDesc(
                List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED, AlertStatus.INVESTIGATING));

        return alerts.stream()
                .map(alert -> new RecentAlertResponse(
                        alert.getAlertCode(),
                        alert.getSeverity().name(),
                        alert.getRule() != null ? alert.getRule().getName() : "Unknown Rule",
                        alert.getStatus().name(),
                        alert.getCreatedAt()))
                .toList();
    }
}


