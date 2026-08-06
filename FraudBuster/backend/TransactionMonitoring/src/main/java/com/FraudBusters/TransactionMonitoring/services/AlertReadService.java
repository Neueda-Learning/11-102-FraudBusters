package com.FraudBusters.TransactionMonitoring.services;

import com.FraudBusters.TransactionMonitoring.exceptions.ResourceNotFoundException;
import com.FraudBusters.TransactionMonitoring.models.AlertEntity;
import com.FraudBusters.TransactionMonitoring.models.AlertStatusHistoryEntity;
import com.FraudBusters.TransactionMonitoring.models.AlertTransactionEntity;
import com.FraudBusters.TransactionMonitoring.models.dto.AlertListItemDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.AlertListResponseDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.AlertHistoryListItemDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.AlertHistoryListResponseDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.AlertTimelineItemDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.AlertTimelineResponseDTO;
import com.FraudBusters.TransactionMonitoring.models.enums.AlertRelationType;
import com.FraudBusters.TransactionMonitoring.models.enums.AlertStatus;
import com.FraudBusters.TransactionMonitoring.models.enums.SeverityLevel;
import com.FraudBusters.TransactionMonitoring.repository.AlertEntityRepository;
import com.FraudBusters.TransactionMonitoring.repository.AlertStatusHistoryEntityRepo;
import com.FraudBusters.TransactionMonitoring.repository.AlertTransactionEntityRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Read-only service for Alert History and per-alert status timeline views.
 */
@Service
@RequiredArgsConstructor
public class AlertReadService {

    private final AlertEntityRepository alertEntityRepository;
    private final AlertStatusHistoryEntityRepo alertStatusHistoryEntityRepo;
    private final AlertTransactionEntityRepo alertTransactionEntityRepo;

    /**
     * Returns paged alerts list + global status counts for the Alerts screen.
     */
    @Transactional(readOnly = true)
    public AlertListResponseDTO getAlertsList(int page, int size, AlertStatus statusFilter) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AlertEntity> alertPage = statusFilter == null
                ? alertEntityRepository.findAll(pageable)
                : alertEntityRepository.findByStatus(statusFilter, pageable);

        List<AlertListItemDTO> alerts = alertPage.getContent()
                .stream()
                .map(this::toAlertListItemDTO)
                .toList();

        return AlertListResponseDTO.builder()
                .page(alertPage.getNumber())
                .size(alertPage.getSize())
                .totalPages(alertPage.getTotalPages())
                .totalElements(alertPage.getTotalElements())
                .hasNext(alertPage.hasNext())
                .hasPrevious(alertPage.hasPrevious())
                .totalAlerts(alertEntityRepository.count())
                .openAlerts(alertEntityRepository.countByStatus(AlertStatus.OPEN))
                .acknowledgedAlerts(alertEntityRepository.countByStatus(AlertStatus.ACKNOWLEDGED))
                .investigatingAlerts(alertEntityRepository.countByStatus(AlertStatus.INVESTIGATING))
                .alerts(alerts)
                .build();
    }

    /**
     * Returns closed/dismissed alerts for the Alert History page.
     */
    @Transactional(readOnly = true)
    public AlertHistoryListResponseDTO getTerminalAlertHistory() {
        List<AlertStatus> terminalStatuses = Arrays.asList(AlertStatus.CLOSED, AlertStatus.DISMISSED);

        List<AlertHistoryListItemDTO> history = alertEntityRepository
                .findByStatusInOrderByClosedAtDesc(terminalStatuses)
                .stream()
                .map(this::toHistoryItemDTO)
                .collect(Collectors.toList());

        return AlertHistoryListResponseDTO.builder()
                .total(history.size())
                .history(history)
                .build();
    }

    /**
     * Returns oldest-first status transitions for one alert.
     */
    @Transactional(readOnly = true)
    public AlertTimelineResponseDTO getAlertTimeline(String alertCode) {
        // Validate alert existence to preserve 404 behavior for invalid alertCode.
        alertEntityRepository.findByAlertCode(alertCode)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found with alertCode: " + alertCode));

        List<AlertTimelineItemDTO> timeline = alertStatusHistoryEntityRepo
                .findByAlertAlertCodeOrderByChangedAtAsc(alertCode)
                .stream()
                .map(this::toTimelineItemDTO)
                .collect(Collectors.toList());

        return AlertTimelineResponseDTO.builder()
                .alertCode(alertCode)
                .totalTransitions(timeline.size())
                .statusHistory(timeline)
                .build();
    }

    private AlertHistoryListItemDTO toHistoryItemDTO(AlertEntity alert) {
        return AlertHistoryListItemDTO.builder()
                .alertCode(alert.getAlertCode())
                .severity(alert.getSeverity())
                .ruleName(alert.getRule() == null ? null : alert.getRule().getName())
                .openedAt(alert.getCreatedAt())
                .closedAt(alert.getClosedAt())
                .finalStatus(alert.getStatus())
                .notes(alert.getClosureReason())
                .build();
    }

    private AlertTimelineItemDTO toTimelineItemDTO(AlertStatusHistoryEntity history) {
        return AlertTimelineItemDTO.builder()
                .oldStatus(history.getOldStatus())
                .newStatus(history.getNewStatus())
                .changedBy(history.getChangedBy())
                .changeReason(history.getChangeReason())
                .changedAt(history.getChangedAt())
                .build();
    }

    private AlertListItemDTO toAlertListItemDTO(AlertEntity alert) {
        AlertTransactionEntity link = alertTransactionEntityRepo
                .findFirstByAlertAndRelationTypeOrderByCreatedAtAsc(alert, AlertRelationType.TRIGGERING_TRANSACTION)
                .or(() -> alertTransactionEntityRepo.findFirstByAlertOrderByCreatedAtAsc(alert))
                .orElse(null);

        return AlertListItemDTO.builder()
                .alertCode(alert.getAlertCode())
                .severity(alert.getSeverity())
                .severityClass(toSeverityClass(alert.getSeverity()))
                .ruleName(alert.getRule() == null ? null : alert.getRule().getName())
                .status(alert.getStatus())
                .statusClass(toStatusClass(alert.getStatus()))
                .accountId(link == null || link.getTransaction() == null ? null : link.getTransaction().getAccountId())
                .amount(link == null || link.getTransaction() == null ? null : link.getTransaction().getAmount())
                .payeeId(link == null || link.getTransaction() == null ? null : link.getTransaction().getPayeeId())
                .createdAt(alert.getCreatedAt())
                .customerFullName(link == null || link.getTransaction() == null ? null : link.getTransaction().getCustomerFullName())
                .customerEmail(link == null || link.getTransaction() == null ? null : link.getTransaction().getCustomerEmail())
                .customerPhone(link == null || link.getTransaction() == null ? null : link.getTransaction().getCustomerPhone())
                .relatedTxnId(link == null || link.getTransaction() == null ? null : link.getTransaction().getTxnId())
                .relatedTxnAmount(link == null || link.getTransaction() == null ? null : link.getTransaction().getAmount())
                .relatedTxnTime(link == null || link.getTransaction() == null ? null : link.getTransaction().getTxnTimestamp())
                .build();
    }

    private String toSeverityClass(SeverityLevel severity) {
        if (severity == null) {
            return "sev-low";
        }

        return switch (severity) {
            case CRITICAL -> "sev-critical";
            case HIGH -> "sev-high";
            case MEDIUM -> "sev-medium";
            case LOW -> "sev-low";
        };
    }

    private String toStatusClass(AlertStatus status) {
        if (status == null) {
            return "st-open";
        }

        return switch (status) {
            case OPEN -> "st-open";
            case ACKNOWLEDGED -> "st-ack";
            case INVESTIGATING -> "st-investigating";
            case CLOSED -> "st-closed";
            case DISMISSED -> "st-dismissed";
        };
    }
}
