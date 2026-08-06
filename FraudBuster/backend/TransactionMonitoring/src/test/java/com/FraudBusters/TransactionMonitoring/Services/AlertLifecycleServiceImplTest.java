package com.FraudBusters.TransactionMonitoring.Services;

import com.FraudBusters.TransactionMonitoring.exceptions.InvalidAlertTransitionException;
import com.FraudBusters.TransactionMonitoring.exceptions.InvalidLifecycleActionException;
import com.FraudBusters.TransactionMonitoring.exceptions.ResourceNotFoundException;
import com.FraudBusters.TransactionMonitoring.models.AlertEntity;
import com.FraudBusters.TransactionMonitoring.models.AlertStatusHistoryEntity;
import com.FraudBusters.TransactionMonitoring.models.AlertTransactionEntity;
import com.FraudBusters.TransactionMonitoring.models.TransactionDecisionEntity;
import com.FraudBusters.TransactionMonitoring.models.TransactionEntity;
import com.FraudBusters.TransactionMonitoring.models.enums.AlertStatus;
import com.FraudBusters.TransactionMonitoring.models.enums.DecisionType;
import com.FraudBusters.TransactionMonitoring.models.enums.FinalDecision;
import com.FraudBusters.TransactionMonitoring.models.enums.MonitorState;
import com.FraudBusters.TransactionMonitoring.repository.AlertEntityRepository;
import com.FraudBusters.TransactionMonitoring.repository.AlertStatusHistoryEntityRepo;
import com.FraudBusters.TransactionMonitoring.repository.AlertTransactionEntityRepo;
import com.FraudBusters.TransactionMonitoring.repository.TransactionDecisionEntityRepo;
import com.FraudBusters.TransactionMonitoring.repository.TransactionEntityRepository;
import com.FraudBusters.TransactionMonitoring.services.Impl.AlertLifecycleServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertLifecycleServiceImplTest {

    @Mock
    private AlertEntityRepository alertEntityRepository;
    @Mock
    private AlertStatusHistoryEntityRepo alertStatusHistoryRepository;
    @Mock
    private AlertTransactionEntityRepo alertTransactionRepository;
    @Mock
    private TransactionDecisionEntityRepo transactionDecisionRepository;
    @Mock
    private TransactionEntityRepository transactionRepository;

    @InjectMocks
    private AlertLifecycleServiceImpl service;

    @Test
    void acknowledgeAlert_whenOpenAndNoReasonOrDecider_thenUsesDefaultsAndWritesHistory() {
        // given
        AlertEntity alert = alertWithStatus("AL-1001", AlertStatus.OPEN);
        when(alertEntityRepository.findByAlertCode("AL-1001")).thenReturn(Optional.of(alert));

        // when
        service.acknowledgeAlert("AL-1001", null, null);

        // then
        assertEquals(AlertStatus.ACKNOWLEDGED, alert.getStatus());
        verify(alertEntityRepository).save(alert);

        ArgumentCaptor<AlertStatusHistoryEntity> historyCaptor = ArgumentCaptor.forClass(AlertStatusHistoryEntity.class);
        verify(alertStatusHistoryRepository).save(historyCaptor.capture());

        AlertStatusHistoryEntity history = historyCaptor.getValue();
        assertSame(alert, history.getAlert());
        assertEquals(AlertStatus.OPEN, history.getOldStatus());
        assertEquals(AlertStatus.ACKNOWLEDGED, history.getNewStatus());
        assertEquals("operator-1", history.getChangedBy());
        assertEquals("Operator acknowledged the alert.", history.getChangeReason());
    }

    @Test
    void acknowledgeAlert_whenReasonAndDeciderHaveSpaces_thenTrimsBeforePersisting() {
        // given
        AlertEntity alert = alertWithStatus("AL-1002", AlertStatus.OPEN);
        when(alertEntityRepository.findByAlertCode("AL-1002")).thenReturn(Optional.of(alert));

        // when
        service.acknowledgeAlert("AL-1002", "  checked by ops  ", "  op-22  ");

        // then
        ArgumentCaptor<AlertStatusHistoryEntity> historyCaptor = ArgumentCaptor.forClass(AlertStatusHistoryEntity.class);
        verify(alertStatusHistoryRepository).save(historyCaptor.capture());

        AlertStatusHistoryEntity history = historyCaptor.getValue();
        assertEquals("op-22", history.getChangedBy());
        assertEquals("checked by ops", history.getChangeReason());
    }

    @Test
    void acknowledgeAlert_whenAlertIsNotOpen_thenThrowsInvalidTransition() {
        // given
        AlertEntity alert = alertWithStatus("AL-1003", AlertStatus.INVESTIGATING);
        when(alertEntityRepository.findByAlertCode("AL-1003")).thenReturn(Optional.of(alert));

        // when
        InvalidAlertTransitionException exception = assertThrows(
                InvalidAlertTransitionException.class,
                () -> service.acknowledgeAlert("AL-1003", "ok", "operator-2")
        );

        // then
        assertTrue(exception.getMessage().contains("Cannot move alert to ACKNOWLEDGED"));
        verify(alertEntityRepository, never()).save(alert);
        verify(alertStatusHistoryRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void investigateAlert_whenAlertNotFound_thenThrowsResourceNotFoundAndNoWrites() {
        // given
        when(alertEntityRepository.findByAlertCode("AL-404")).thenReturn(Optional.empty());

        // when
        assertThrows(ResourceNotFoundException.class,
                () -> service.investigateAlert("AL-404", "start", "operator-3"));

        // then
        verify(alertEntityRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(alertStatusHistoryRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void investigateAlert_whenReasonTooLong_thenThrowsValidationErrorBeforeRepositoryCalls() {
        // given
        String reason501 = "x".repeat(501);

        // when
        InvalidLifecycleActionException exception = assertThrows(
                InvalidLifecycleActionException.class,
                () -> service.investigateAlert("AL-2001", reason501, "operator-2")
        );

        // then
        assertEquals("reason cannot exceed 500 characters.", exception.getMessage());
        verifyNoInteractions(alertEntityRepository, alertStatusHistoryRepository, alertTransactionRepository,
                transactionDecisionRepository, transactionRepository);
    }

    @Test
    void acknowledgeAlert_whenDeciderTooLong_thenThrowsValidationErrorBeforeRepositoryCalls() {
        // given
        String decidedBy101 = "d".repeat(101);

        // when
        InvalidLifecycleActionException exception = assertThrows(
                InvalidLifecycleActionException.class,
                () -> service.acknowledgeAlert("AL-2002", "reason", decidedBy101)
        );

        // then
        assertEquals("decidedBy cannot exceed 100 characters.", exception.getMessage());
        verifyNoInteractions(alertEntityRepository, alertStatusHistoryRepository, alertTransactionRepository,
                transactionDecisionRepository, transactionRepository);
    }

    @Test
    void closeAlert_whenReasonBlank_thenThrowsRequiredReasonErrorBeforeRepositoryCalls() {
        // given
        String blankReason = "   ";

        // when
        InvalidLifecycleActionException exception = assertThrows(
                InvalidLifecycleActionException.class,
                () -> service.closeAlert("AL-3001", blankReason, "operator-5")
        );

        // then
        assertEquals("Reason is required for close action.", exception.getMessage());
        verifyNoInteractions(alertEntityRepository, alertStatusHistoryRepository, alertTransactionRepository,
                transactionDecisionRepository, transactionRepository);
    }

    @Test
    void closeAlert_whenStatusIsInvestigatingAndTwoLinkedTransactions_thenDeclinesAllAndPersistsAudit() {
        // given
        AlertEntity alert = alertWithStatus("AL-3002", AlertStatus.INVESTIGATING);
        TransactionEntity txn1 = transaction("TXN-1");
        TransactionEntity txn2 = transaction("TXN-2");

        AlertTransactionEntity link1 = new AlertTransactionEntity();
        link1.setAlert(alert);
        link1.setTransaction(txn1);

        AlertTransactionEntity link2 = new AlertTransactionEntity();
        link2.setAlert(alert);
        link2.setTransaction(txn2);

        when(alertEntityRepository.findByAlertCode("AL-3002")).thenReturn(Optional.of(alert));
        when(alertTransactionRepository.findByAlert(alert)).thenReturn(List.of(link1, link2));

        // when
        service.closeAlert("AL-3002", "  confirmed fraud  ", "  senior-op  ");

        // then
        assertEquals(AlertStatus.CLOSED, alert.getStatus());
        assertNotNull(alert.getClosedAt());
        assertEquals("confirmed fraud", alert.getClosureReason());
        verify(alertEntityRepository).save(alert);

        ArgumentCaptor<AlertStatusHistoryEntity> historyCaptor = ArgumentCaptor.forClass(AlertStatusHistoryEntity.class);
        verify(alertStatusHistoryRepository).save(historyCaptor.capture());
        AlertStatusHistoryEntity history = historyCaptor.getValue();
        assertEquals(AlertStatus.INVESTIGATING, history.getOldStatus());
        assertEquals(AlertStatus.CLOSED, history.getNewStatus());
        assertEquals("senior-op", history.getChangedBy());
        assertEquals("confirmed fraud", history.getChangeReason());

        ArgumentCaptor<TransactionDecisionEntity> decisionCaptor = ArgumentCaptor.forClass(TransactionDecisionEntity.class);
        verify(transactionDecisionRepository, times(2)).save(decisionCaptor.capture());

        List<TransactionDecisionEntity> decisions = decisionCaptor.getAllValues();
        assertEquals(DecisionType.DECLINE, decisions.get(0).getDecision());
        assertEquals("senior-op", decisions.get(0).getDecidedBy());
        assertEquals("confirmed fraud", decisions.get(0).getDecisionReason());
        assertSame(alert, decisions.get(0).getAlert());
        assertEquals(DecisionType.DECLINE, decisions.get(1).getDecision());

        verify(transactionRepository, times(2)).save(org.mockito.ArgumentMatchers.any(TransactionEntity.class));
        assertEquals(MonitorState.DECLINED, txn1.getMonitorState());
        assertEquals(FinalDecision.DECLINE, txn1.getFinalDecision());
        assertEquals("confirmed fraud", txn1.getDecisionReason());
        assertNotNull(txn1.getDecidedAt());

        assertEquals(MonitorState.DECLINED, txn2.getMonitorState());
        assertEquals(FinalDecision.DECLINE, txn2.getFinalDecision());
        assertEquals("confirmed fraud", txn2.getDecisionReason());
        assertNotNull(txn2.getDecidedAt());
    }

    @Test
    void closeAlert_whenNoLinkedTransactions_thenOnlyAlertAndHistoryAreSaved() {
        // given
        AlertEntity alert = alertWithStatus("AL-3003", AlertStatus.INVESTIGATING);
        when(alertEntityRepository.findByAlertCode("AL-3003")).thenReturn(Optional.of(alert));
        when(alertTransactionRepository.findByAlert(alert)).thenReturn(List.of());

        // when
        service.closeAlert("AL-3003", "fraud confirmed", "operator-7");

        // then
        verify(alertEntityRepository).save(alert);
        verify(alertStatusHistoryRepository).save(org.mockito.ArgumentMatchers.any(AlertStatusHistoryEntity.class));
        verify(transactionDecisionRepository, never()).save(org.mockito.ArgumentMatchers.any(TransactionDecisionEntity.class));
        verify(transactionRepository, never()).save(org.mockito.ArgumentMatchers.any(TransactionEntity.class));
    }

    @Test
    void dismissAlert_whenStatusIsClosed_thenThrowsInvalidTransition() {
        // given
        AlertEntity alert = alertWithStatus("AL-4001", AlertStatus.CLOSED);
        when(alertEntityRepository.findByAlertCode("AL-4001")).thenReturn(Optional.of(alert));

        // when
        InvalidAlertTransitionException exception = assertThrows(
                InvalidAlertTransitionException.class,
                () -> service.dismissAlert("AL-4001", "false positive", "operator-9")
        );

        // then
        assertTrue(exception.getMessage().contains("terminal status"));
        verify(alertEntityRepository, never()).save(alert);
        verify(alertStatusHistoryRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void dismissAlert_whenReasonBlank_thenThrowsRequiredReasonErrorBeforeRepositoryCalls() {
        // given
        String blankReason = "   ";

        // when
        InvalidLifecycleActionException exception = assertThrows(
                InvalidLifecycleActionException.class,
                () -> service.dismissAlert("AL-4002", blankReason, "operator-1")
        );

        // then
        assertEquals("Reason is required for dismiss action.", exception.getMessage());
        verifyNoInteractions(alertEntityRepository, alertStatusHistoryRepository, alertTransactionRepository,
                transactionDecisionRepository, transactionRepository);
    }

    @Test
    void dismissAlert_whenOpenWithLinkedTransactions_thenAllowsAllAndPersistsAudit() {
        // given
        AlertEntity alert = alertWithStatus("AL-4003", AlertStatus.OPEN);
        TransactionEntity txn = transaction("TXN-ALLOW-1");

        AlertTransactionEntity link = new AlertTransactionEntity();
        link.setAlert(alert);
        link.setTransaction(txn);

        when(alertEntityRepository.findByAlertCode("AL-4003")).thenReturn(Optional.of(alert));
        when(alertTransactionRepository.findByAlert(alert)).thenReturn(List.of(link));

        // when
        service.dismissAlert("AL-4003", "  false positive  ", "  reviewer-1  ");

        // then
        assertEquals(AlertStatus.DISMISSED, alert.getStatus());
        assertNotNull(alert.getClosedAt());
        assertEquals("false positive", alert.getClosureReason());

        ArgumentCaptor<TransactionDecisionEntity> decisionCaptor = ArgumentCaptor.forClass(TransactionDecisionEntity.class);
        verify(transactionDecisionRepository).save(decisionCaptor.capture());

        TransactionDecisionEntity decision = decisionCaptor.getValue();
        assertEquals(DecisionType.ALLOW, decision.getDecision());
        assertEquals("reviewer-1", decision.getDecidedBy());
        assertEquals("false positive", decision.getDecisionReason());
        assertSame(alert, decision.getAlert());

        assertEquals(MonitorState.RELEASED, txn.getMonitorState());
        assertEquals(FinalDecision.ALLOW, txn.getFinalDecision());
        assertEquals("false positive", txn.getDecisionReason());
        assertNotNull(txn.getDecidedAt());
        verify(transactionRepository).save(txn);
    }

    private AlertEntity alertWithStatus(String alertCode, AlertStatus status) {
        AlertEntity alert = new AlertEntity();
        alert.setAlertCode(alertCode);
        alert.setStatus(status);
        return alert;
    }

    private TransactionEntity transaction(String txnId) {
        TransactionEntity txn = new TransactionEntity();
        txn.setTxnId(txnId);
        txn.setMonitorState(MonitorState.HELD);
        txn.setFinalDecision(FinalDecision.PENDING);
        return txn;
    }
}
