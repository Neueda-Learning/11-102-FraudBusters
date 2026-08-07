package com.FraudBusters.TransactionMonitoring.services;

import com.FraudBusters.TransactionMonitoring.models.AlertEntity;
import com.FraudBusters.TransactionMonitoring.models.AlertTransactionEntity;
import com.FraudBusters.TransactionMonitoring.models.dto.DashboardRecentAlertItemDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.DashboardSummaryResponseDTO;
import com.FraudBusters.TransactionMonitoring.models.enums.AlertRelationType;
import com.FraudBusters.TransactionMonitoring.models.enums.AlertStatus;
import com.FraudBusters.TransactionMonitoring.repository.AlertEntityRepository;
import com.FraudBusters.TransactionMonitoring.repository.AlertTransactionEntityRepo;
import com.FraudBusters.TransactionMonitoring.repository.TransactionEntityRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only dashboard data provider used by dashboard.html.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AlertEntityRepository alertEntityRepository;
    private final TransactionEntityRepository transactionEntityRepository;
    private final AlertTransactionEntityRepo alertTransactionEntityRepo;

    private static final List<String> DASHBOARD_RULE_ORDER = List.of(
            "AMOUNT_THRESHOLD",
            "VELOCITY",
            "NEW_PAYEE",
            "DAILY_LIMIT");

    /**
     * Builds top summary cards payload.
     * closedToday key is kept for frontend compatibility but value means resolved today.
     */
    @Transactional(readOnly = true)
    public DashboardSummaryResponseDTO getDashboardSummary() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfTomorrow = startOfToday.plusDays(1);

        long openAlerts = alertEntityRepository.countByStatus(AlertStatus.OPEN);
        long acknowledgedAlerts = alertEntityRepository.countByStatus(AlertStatus.ACKNOWLEDGED);
        long transactionsToday = transactionEntityRepository
                .countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(startOfToday, startOfTomorrow);

        long resolvedToday = alertEntityRepository.countByStatusInAndClosedAtGreaterThanEqualAndClosedAtLessThan(
                Arrays.asList(AlertStatus.CLOSED, AlertStatus.DISMISSED),
                startOfToday,
                startOfTomorrow);

        return DashboardSummaryResponseDTO.builder()
                .openAlerts(openAlerts)
                .acknowledgedAlerts(acknowledgedAlerts)
                .transactionsToday(transactionsToday)
                .closedToday(resolvedToday)
                .build();
    }

    /**
     * Returns top 5 newest OPEN alerts for the dashboard table.
     */
    @Transactional(readOnly = true)
    public List<DashboardRecentAlertItemDTO> getRecentOpenAlerts() {
        return alertEntityRepository.findTop5ByStatusOrderByCreatedAtDesc(AlertStatus.OPEN)
                .stream()
                .map(this::toRecentAlertItem)
                .toList();
    }

    /**
     * Returns the bar-chart data for the dashboard rule execution graph.
     * The frontend expects these stable keys:
     * AMOUNT_THRESHOLD, VELOCITY, NEW_PAYEE, DAILY_LIMIT.
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getRuleStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        DASHBOARD_RULE_ORDER.forEach(ruleKey -> stats.put(ruleKey, 0L));

        alertEntityRepository.countAlertsGroupedByRuleCode().forEach(row -> {
            if (row == null || row.length < 2 || row[0] == null) {
                return;
            }

            String dashboardKey = mapToDashboardRuleKey(row[0].toString());
            long count = row[1] instanceof Number number ? number.longValue() : 0L;

            if (stats.containsKey(dashboardKey)) {
                stats.put(dashboardKey, count);
            }
        });

        return stats;
    }

    private String mapToDashboardRuleKey(String ruleCode) {
        if ("VELOCITY_CHECK".equalsIgnoreCase(ruleCode)) {
            return "VELOCITY";
        }
        return ruleCode;
    }

    private DashboardRecentAlertItemDTO toRecentAlertItem(AlertEntity alert) {
        return DashboardRecentAlertItemDTO.builder()
                .alertCode(alert.getAlertCode())
                .severity(alert.getSeverity())
                .ruleName(alert.getRule() == null ? null : alert.getRule().getName())
                .accountId(resolveAccountId(alert))
                .createdAt(alert.getCreatedAt())
                .status(alert.getStatus())
                .build();
    }

    private String resolveAccountId(AlertEntity alert) {
        return alertTransactionEntityRepo
                .findFirstByAlertAndRelationTypeOrderByCreatedAtAsc(alert, AlertRelationType.TRIGGERING_TRANSACTION)
                .or(() -> alertTransactionEntityRepo.findFirstByAlertOrderByCreatedAtAsc(alert))
                .map(AlertTransactionEntity::getTransaction)
                .map(transaction -> transaction == null ? null : transaction.getAccountId())
                .orElse(null);
    }
}

