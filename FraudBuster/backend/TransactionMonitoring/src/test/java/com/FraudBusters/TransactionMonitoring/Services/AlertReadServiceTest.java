package com.FraudBusters.TransactionMonitoring.Services;

import com.FraudBusters.TransactionMonitoring.exceptions.ResourceNotFoundException;
import com.FraudBusters.TransactionMonitoring.models.AlertEntity;
import com.FraudBusters.TransactionMonitoring.models.AlertStatusHistoryEntity;
import com.FraudBusters.TransactionMonitoring.models.AlertTransactionEntity;
import com.FraudBusters.TransactionMonitoring.models.RuleEntity;
import com.FraudBusters.TransactionMonitoring.models.TransactionEntity;
import com.FraudBusters.TransactionMonitoring.models.dto.AlertHistoryListResponseDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.AlertListItemDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.AlertListResponseDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.AlertTimelineResponseDTO;
import com.FraudBusters.TransactionMonitoring.models.enums.AlertRelationType;
import com.FraudBusters.TransactionMonitoring.models.enums.AlertStatus;
import com.FraudBusters.TransactionMonitoring.models.enums.SeverityLevel;
import com.FraudBusters.TransactionMonitoring.models.enums.TransactionType;
import com.FraudBusters.TransactionMonitoring.repository.AlertEntityRepository;
import com.FraudBusters.TransactionMonitoring.repository.AlertStatusHistoryEntityRepo;
import com.FraudBusters.TransactionMonitoring.repository.AlertTransactionEntityRepo;
import com.FraudBusters.TransactionMonitoring.services.AlertReadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertReadServiceTest {

    @Mock
    private AlertEntityRepository alertEntityRepository;
    @Mock
    private AlertStatusHistoryEntityRepo alertStatusHistoryEntityRepo;
    @Mock
    private AlertTransactionEntityRepo alertTransactionEntityRepo;

    @InjectMocks
    private AlertReadService service;

    @Test
    void getAlertsList_whenPageAndSizeInvalidAndNoStatusFilter_thenNormalizesAndUsesFindAllPath() {
        // given
        AlertEntity alert = alert("AL-6001", SeverityLevel.HIGH, AlertStatus.OPEN);
        Page<AlertEntity> page = new PageImpl<>(List.of(alert), PageRequest.of(0, 1), 1);

        when(alertEntityRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(alertEntityRepository.count()).thenReturn(50L);
        when(alertEntityRepository.countByStatus(AlertStatus.OPEN)).thenReturn(7L);
        when(alertEntityRepository.countByStatus(AlertStatus.ACKNOWLEDGED)).thenReturn(3L);
        when(alertEntityRepository.countByStatus(AlertStatus.INVESTIGATING)).thenReturn(2L);

        AlertTransactionEntity triggeringLink = link(alert, transaction("TXN-6001", "ACC-1", "PAY-1"));
        when(alertTransactionEntityRepo.findFirstByAlertAndRelationTypeOrderByCreatedAtAsc(alert, AlertRelationType.TRIGGERING_TRANSACTION))
                .thenReturn(Optional.of(triggeringLink));

        // when
        AlertListResponseDTO response = service.getAlertsList(-5, 0, null);

        // then
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(alertEntityRepository).findAll(pageableCaptor.capture());
        Pageable captured = pageableCaptor.getValue();
        assertEquals(0, captured.getPageNumber());
        assertEquals(1, captured.getPageSize());

        verify(alertEntityRepository, never()).findByStatus(any(AlertStatus.class), any(Pageable.class));

        assertEquals(0, response.getPage());
        assertEquals(1, response.getSize());
        assertEquals(1, response.getTotalPages());
        assertEquals(1L, response.getTotalElements());
        assertFalse(response.isHasNext());
        assertFalse(response.isHasPrevious());
        assertEquals(50L, response.getTotalAlerts());
        assertEquals(7L, response.getOpenAlerts());
        assertEquals(3L, response.getAcknowledgedAlerts());
        assertEquals(2L, response.getInvestigatingAlerts());
        assertEquals(1, response.getAlerts().size());
    }

    @Test
    void getAlertsList_whenStatusFilterProvided_thenUsesFindByStatusPath() {
        // given
        AlertEntity alert = alert("AL-6002", SeverityLevel.MEDIUM, AlertStatus.ACKNOWLEDGED);
        Page<AlertEntity> page = new PageImpl<>(List.of(alert), PageRequest.of(2, 4), 10);

        when(alertEntityRepository.findByStatus(eq(AlertStatus.ACKNOWLEDGED), any(Pageable.class))).thenReturn(page);
        when(alertTransactionEntityRepo.findFirstByAlertAndRelationTypeOrderByCreatedAtAsc(alert, AlertRelationType.TRIGGERING_TRANSACTION))
                .thenReturn(Optional.empty());
        when(alertTransactionEntityRepo.findFirstByAlertOrderByCreatedAtAsc(alert))
                .thenReturn(Optional.of(link(alert, transaction("TXN-6002", "ACC-2", "PAY-2"))));

        // when
        AlertListResponseDTO response = service.getAlertsList(2, 4, AlertStatus.ACKNOWLEDGED);

        // then
        verify(alertEntityRepository).findByStatus(eq(AlertStatus.ACKNOWLEDGED), any(Pageable.class));
        verify(alertEntityRepository, never()).findAll(any(Pageable.class));
        assertEquals(2, response.getPage());
        assertEquals(4, response.getSize());
        assertEquals(1, response.getAlerts().size());
        assertEquals("st-ack", response.getAlerts().get(0).getStatusClass());
    }

    @Test
    void getAlertsList_whenNoTriggeringLink_thenFallsBackToOldestAnyRelationLink() {
        // given
        AlertEntity alert = alert("AL-6003", SeverityLevel.LOW, AlertStatus.INVESTIGATING);
        Page<AlertEntity> page = new PageImpl<>(List.of(alert), PageRequest.of(0, 5), 1);

        TransactionEntity fallbackTxn = transaction("TXN-6003", "ACC-FALLBACK", "PAY-FALLBACK");
        fallbackTxn.setAmount(new BigDecimal("123.45"));

        when(alertEntityRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(alertTransactionEntityRepo.findFirstByAlertAndRelationTypeOrderByCreatedAtAsc(alert, AlertRelationType.TRIGGERING_TRANSACTION))
                .thenReturn(Optional.empty());
        when(alertTransactionEntityRepo.findFirstByAlertOrderByCreatedAtAsc(alert))
                .thenReturn(Optional.of(link(alert, fallbackTxn)));

        // when
        AlertListResponseDTO response = service.getAlertsList(0, 5, null);

        // then
        AlertListItemDTO item = response.getAlerts().get(0);
        assertEquals("ACC-FALLBACK", item.getAccountId());
        assertEquals("PAY-FALLBACK", item.getPayeeId());
        assertEquals(new BigDecimal("123.45"), item.getAmount());
        assertEquals("TXN-6003", item.getRelatedTxnId());
    }

    @Test
    void getAlertsList_whenSeverityOrStatusOrLinkDataMissing_thenUsesDefaultClassesAndNullTransactionFields() {
        // given
        AlertEntity alert = alert("AL-6004", null, null);
        Page<AlertEntity> page = new PageImpl<>(List.of(alert), PageRequest.of(0, 3), 1);

        AlertTransactionEntity linkWithNullTxn = new AlertTransactionEntity();
        linkWithNullTxn.setAlert(alert);

        when(alertEntityRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(alertTransactionEntityRepo.findFirstByAlertAndRelationTypeOrderByCreatedAtAsc(alert, AlertRelationType.TRIGGERING_TRANSACTION))
                .thenReturn(Optional.of(linkWithNullTxn));

        // when
        AlertListResponseDTO response = service.getAlertsList(0, 3, null);

        // then
        AlertListItemDTO item = response.getAlerts().get(0);
        assertEquals("sev-low", item.getSeverityClass());
        assertEquals("st-open", item.getStatusClass());
        assertNull(item.getAccountId());
        assertNull(item.getAmount());
        assertNull(item.getPayeeId());
        assertNull(item.getRelatedTxnId());
    }

    @Test
    void getTerminalAlertHistory_whenTerminalAlertsExist_thenMapsFieldsAndTotal() {
        // given
        AlertEntity closed = alert("AL-6005", SeverityLevel.CRITICAL, AlertStatus.CLOSED);
        closed.setClosureReason("Fraud confirmed");
        closed.setClosedAt(LocalDateTime.of(2026, 8, 6, 15, 30));

        AlertEntity dismissedNoRule = alert("AL-6006", SeverityLevel.LOW, AlertStatus.DISMISSED);
        dismissedNoRule.setRule(null);
        dismissedNoRule.setClosureReason("False positive");
        dismissedNoRule.setClosedAt(LocalDateTime.of(2026, 8, 6, 15, 20));

        when(alertEntityRepository.findByStatusInOrderByClosedAtDesc(List.of(AlertStatus.CLOSED, AlertStatus.DISMISSED)))
                .thenReturn(List.of(closed, dismissedNoRule));

        // when
        AlertHistoryListResponseDTO response = service.getTerminalAlertHistory();

        // then
        assertEquals(2, response.getTotal());
        assertEquals("AL-6005", response.getHistory().get(0).getAlertCode());
        assertEquals("Fraud Rule", response.getHistory().get(0).getRuleName());
        assertEquals(AlertStatus.CLOSED, response.getHistory().get(0).getFinalStatus());
        assertEquals("Fraud confirmed", response.getHistory().get(0).getNotes());

        assertEquals("AL-6006", response.getHistory().get(1).getAlertCode());
        assertNull(response.getHistory().get(1).getRuleName());
        assertEquals(AlertStatus.DISMISSED, response.getHistory().get(1).getFinalStatus());
        assertEquals("False positive", response.getHistory().get(1).getNotes());
    }

    @Test
    void getAlertTimeline_whenAlertCodeNotFound_thenThrowsResourceNotFoundAndSkipsHistoryQuery() {
        // given
        when(alertEntityRepository.findByAlertCode("AL-404")).thenReturn(Optional.empty());

        // when
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.getAlertTimeline("AL-404")
        );

        // then
        assertEquals("Alert not found with alertCode: AL-404", exception.getMessage());
        verify(alertEntityRepository).findByAlertCode("AL-404");
        verifyNoInteractions(alertStatusHistoryEntityRepo);
    }

    @Test
    void getAlertTimeline_whenAlertExists_thenReturnsOldestFirstTimelineAndTransitionCount() {
        // given
        AlertEntity alert = alert("AL-6007", SeverityLevel.HIGH, AlertStatus.INVESTIGATING);
        when(alertEntityRepository.findByAlertCode("AL-6007")).thenReturn(Optional.of(alert));

        AlertStatusHistoryEntity t1 = new AlertStatusHistoryEntity();
        t1.setOldStatus(AlertStatus.OPEN);
        t1.setNewStatus(AlertStatus.ACKNOWLEDGED);
        t1.setChangedBy("operator-1");
        t1.setChangeReason("Acknowledged");
        t1.setChangedAt(LocalDateTime.of(2026, 8, 6, 15, 0));

        AlertStatusHistoryEntity t2 = new AlertStatusHistoryEntity();
        t2.setOldStatus(AlertStatus.ACKNOWLEDGED);
        t2.setNewStatus(AlertStatus.INVESTIGATING);
        t2.setChangedBy("operator-2");
        t2.setChangeReason("Started investigation");
        t2.setChangedAt(LocalDateTime.of(2026, 8, 6, 15, 5));

        when(alertStatusHistoryEntityRepo.findByAlertAlertCodeOrderByChangedAtAsc("AL-6007"))
                .thenReturn(List.of(t1, t2));

        // when
        AlertTimelineResponseDTO response = service.getAlertTimeline("AL-6007");

        // then
        assertEquals("AL-6007", response.getAlertCode());
        assertEquals(2, response.getTotalTransitions());
        assertEquals(AlertStatus.OPEN, response.getStatusHistory().get(0).getOldStatus());
        assertEquals(AlertStatus.ACKNOWLEDGED, response.getStatusHistory().get(0).getNewStatus());
        assertEquals("operator-1", response.getStatusHistory().get(0).getChangedBy());
        assertEquals("Acknowledged", response.getStatusHistory().get(0).getChangeReason());

        assertEquals(AlertStatus.ACKNOWLEDGED, response.getStatusHistory().get(1).getOldStatus());
        assertEquals(AlertStatus.INVESTIGATING, response.getStatusHistory().get(1).getNewStatus());
        assertEquals("operator-2", response.getStatusHistory().get(1).getChangedBy());
        assertEquals("Started investigation", response.getStatusHistory().get(1).getChangeReason());
    }

    private AlertEntity alert(String code, SeverityLevel severity, AlertStatus status) {
        AlertEntity alert = new AlertEntity();
        alert.setAlertCode(code);
        alert.setSeverity(severity);
        alert.setStatus(status);
        alert.setCreatedAt(LocalDateTime.of(2026, 8, 6, 14, 0));

        RuleEntity rule = new RuleEntity();
        rule.setName("Fraud Rule");
        alert.setRule(rule);
        return alert;
    }

    private TransactionEntity transaction(String txnId, String accountId, String payeeId) {
        TransactionEntity transaction = new TransactionEntity();
        transaction.setTxnId(txnId);
        transaction.setAccountId(accountId);
        transaction.setPayeeId(payeeId);
        transaction.setAmount(new BigDecimal("10.00"));
        transaction.setTxnType(TransactionType.DEBIT);
        transaction.setTxnTimestamp(LocalDateTime.of(2026, 8, 6, 13, 30));
        return transaction;
    }

    private AlertTransactionEntity link(AlertEntity alert, TransactionEntity transaction) {
        AlertTransactionEntity link = new AlertTransactionEntity();
        link.setAlert(alert);
        link.setTransaction(transaction);
        return link;
    }
}

