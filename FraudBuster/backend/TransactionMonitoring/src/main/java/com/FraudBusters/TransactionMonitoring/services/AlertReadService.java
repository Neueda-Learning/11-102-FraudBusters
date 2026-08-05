package com.FraudBusters.TransactionMonitoring.services;

import com.FraudBusters.TransactionMonitoring.exceptions.ResourceNotFoundException;
import com.FraudBusters.TransactionMonitoring.models.AlertEntity;
import com.FraudBusters.TransactionMonitoring.models.AlertStatusHistoryEntity;
import com.FraudBusters.TransactionMonitoring.models.dto.AlertHistoryListItemDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.AlertHistoryListResponseDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.AlertTimelineItemDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.AlertTimelineResponseDTO;
import com.FraudBusters.TransactionMonitoring.models.enums.AlertStatus;
import com.FraudBusters.TransactionMonitoring.repository.AlertEntityRepository;
import com.FraudBusters.TransactionMonitoring.repository.AlertStatusHistoryEntityRepo;
import lombok.RequiredArgsConstructor;
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
}

