package com.FraudBusters.TransactionMonitoring.services;

import com.FraudBusters.TransactionMonitoring.models.AlertEntity;
import com.FraudBusters.TransactionMonitoring.models.AlertTransactionEntity;
import com.FraudBusters.TransactionMonitoring.models.RuleEntity;
import com.FraudBusters.TransactionMonitoring.models.TransactionEntity;
import com.FraudBusters.TransactionMonitoring.models.dto.DashboardRecentAlertItemDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.DashboardSummaryResponseDTO;
import com.FraudBusters.TransactionMonitoring.models.enums.AlertRelationType;
import com.FraudBusters.TransactionMonitoring.models.enums.AlertStatus;
import com.FraudBusters.TransactionMonitoring.models.enums.SeverityLevel;
import com.FraudBusters.TransactionMonitoring.models.enums.TransactionType;
import com.FraudBusters.TransactionMonitoring.repository.AlertEntityRepository;
import com.FraudBusters.TransactionMonitoring.repository.AlertTransactionEntityRepo;
import com.FraudBusters.TransactionMonitoring.repository.TransactionEntityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private AlertEntityRepository alertEntityRepository;
    @Mock
    private TransactionEntityRepository transactionEntityRepository;
    @Mock
    private AlertTransactionEntityRepo alertTransactionEntityRepo;

    @InjectMocks
    private DashboardService service;

    @Test
    void getDashboardSummary_whenRepositoriesReturnCounts_thenBuildsExpectedSummary() {
        // given
        when(alertEntityRepository.countByStatus(AlertStatus.OPEN)).thenReturn(12L);
        when(alertEntityRepository.countByStatus(AlertStatus.ACKNOWLEDGED)).thenReturn(5L);
        when(transactionEntityRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any())).thenReturn(40L);
        when(alertEntityRepository.countByStatusInAndClosedAtGreaterThanEqualAndClosedAtLessThan(any(), any(), any())).thenReturn(9L);

        // when
        DashboardSummaryResponseDTO response = service.getDashboardSummary();

        // then
        assertEquals(12L, response.getOpenAlerts());
        assertEquals(5L, response.getAcknowledgedAlerts());
        assertEquals(40L, response.getTransactionsToday());
        assertEquals(9L, response.getClosedToday());
    }

    @Test
    void getDashboardSummary_whenBuildingDateWindow_thenUsesSameDayHalfOpenRangeAndResolvedStatuses() {
        // given
        when(alertEntityRepository.countByStatus(AlertStatus.OPEN)).thenReturn(0L);
        when(alertEntityRepository.countByStatus(AlertStatus.ACKNOWLEDGED)).thenReturn(0L);
        when(transactionEntityRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any())).thenReturn(0L);
        when(alertEntityRepository.countByStatusInAndClosedAtGreaterThanEqualAndClosedAtLessThan(any(), any(), any())).thenReturn(0L);

        // when
        service.getDashboardSummary();

        // then
        ArgumentCaptor<LocalDateTime> txStartCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> txEndCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(transactionEntityRepository).countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                txStartCaptor.capture(), txEndCaptor.capture());

        LocalDateTime txStart = txStartCaptor.getValue();
        LocalDateTime txEnd = txEndCaptor.getValue();
        assertEquals(txStart.plusDays(1), txEnd);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AlertStatus>> statusesCaptor = ArgumentCaptor.forClass((Class) List.class);
        ArgumentCaptor<LocalDateTime> resolvedStartCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> resolvedEndCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        verify(alertEntityRepository).countByStatusInAndClosedAtGreaterThanEqualAndClosedAtLessThan(
                statusesCaptor.capture(), resolvedStartCaptor.capture(), resolvedEndCaptor.capture());

        List<AlertStatus> statuses = statusesCaptor.getValue();
        assertEquals(List.of(AlertStatus.CLOSED, AlertStatus.DISMISSED), statuses);
        assertEquals(txStart, resolvedStartCaptor.getValue());
        assertEquals(txEnd, resolvedEndCaptor.getValue());
    }

    @Test
    void getRecentOpenAlerts_whenTriggeringLinkExists_thenPrefersTriggeringAccountMapping() {
        // given
        AlertEntity alert = alert("AL-7001", SeverityLevel.HIGH, AlertStatus.OPEN, "Amount Rule");
        TransactionEntity txn = transaction("TXN-7001", "ACC-TRIGGER");

        when(alertEntityRepository.findTop5ByStatusOrderByCreatedAtDesc(AlertStatus.OPEN)).thenReturn(List.of(alert));
        when(alertTransactionEntityRepo.findFirstByAlertAndRelationTypeOrderByCreatedAtAsc(alert, AlertRelationType.TRIGGERING_TRANSACTION))
                .thenReturn(Optional.of(link(alert, txn)));

        // when
        List<DashboardRecentAlertItemDTO> response = service.getRecentOpenAlerts();

        // then
        assertEquals(1, response.size());
        DashboardRecentAlertItemDTO item = response.get(0);
        assertEquals("AL-7001", item.getAlertCode());
        assertEquals(SeverityLevel.HIGH, item.getSeverity());
        assertEquals("Amount Rule", item.getRuleName());
        assertEquals("ACC-TRIGGER", item.getAccountId());
        assertEquals(AlertStatus.OPEN, item.getStatus());
        verify(alertTransactionEntityRepo, never()).findFirstByAlertOrderByCreatedAtAsc(alert);
    }

    @Test
    void getRecentOpenAlerts_whenNoTriggeringLink_thenFallsBackToOldestLink() {
        // given
        AlertEntity alert = alert("AL-7002", SeverityLevel.MEDIUM, AlertStatus.OPEN, "Velocity Rule");
        TransactionEntity fallbackTxn = transaction("TXN-7002", "ACC-FALLBACK");

        when(alertEntityRepository.findTop5ByStatusOrderByCreatedAtDesc(AlertStatus.OPEN)).thenReturn(List.of(alert));
        when(alertTransactionEntityRepo.findFirstByAlertAndRelationTypeOrderByCreatedAtAsc(alert, AlertRelationType.TRIGGERING_TRANSACTION))
                .thenReturn(Optional.empty());
        when(alertTransactionEntityRepo.findFirstByAlertOrderByCreatedAtAsc(alert))
                .thenReturn(Optional.of(link(alert, fallbackTxn)));

        // when
        List<DashboardRecentAlertItemDTO> response = service.getRecentOpenAlerts();

        // then
        assertEquals(1, response.size());
        assertEquals("ACC-FALLBACK", response.get(0).getAccountId());
    }

    @Test
    void getRecentOpenAlerts_whenSelectedLinkHasNullTransaction_thenAccountIdIsNull() {
        // given
        AlertEntity alert = alert("AL-7003", SeverityLevel.LOW, AlertStatus.OPEN, "New Payee Rule");
        AlertTransactionEntity linkWithNullTxn = new AlertTransactionEntity();
        linkWithNullTxn.setAlert(alert);

        when(alertEntityRepository.findTop5ByStatusOrderByCreatedAtDesc(AlertStatus.OPEN)).thenReturn(List.of(alert));
        when(alertTransactionEntityRepo.findFirstByAlertAndRelationTypeOrderByCreatedAtAsc(alert, AlertRelationType.TRIGGERING_TRANSACTION))
                .thenReturn(Optional.of(linkWithNullTxn));

        // when
        List<DashboardRecentAlertItemDTO> response = service.getRecentOpenAlerts();

        // then
        assertEquals(1, response.size());
        assertNull(response.get(0).getAccountId());
    }

    @Test
    void getRecentOpenAlerts_whenNoLinksExist_thenAccountIdIsNull() {
        // given
        AlertEntity alert = alert("AL-7004", SeverityLevel.CRITICAL, AlertStatus.OPEN, "Daily Limit Rule");

        when(alertEntityRepository.findTop5ByStatusOrderByCreatedAtDesc(AlertStatus.OPEN)).thenReturn(List.of(alert));
        when(alertTransactionEntityRepo.findFirstByAlertAndRelationTypeOrderByCreatedAtAsc(alert, AlertRelationType.TRIGGERING_TRANSACTION))
                .thenReturn(Optional.empty());
        when(alertTransactionEntityRepo.findFirstByAlertOrderByCreatedAtAsc(alert))
                .thenReturn(Optional.empty());

        // when
        List<DashboardRecentAlertItemDTO> response = service.getRecentOpenAlerts();

        // then
        assertEquals(1, response.size());
        assertNull(response.get(0).getAccountId());
    }

    @Test
    void getRecentOpenAlerts_whenAlertRuleIsNull_thenRuleNameIsNull() {
        // given
        AlertEntity alert = alert("AL-7005", SeverityLevel.HIGH, AlertStatus.OPEN, null);
        alert.setRule(null);

        when(alertEntityRepository.findTop5ByStatusOrderByCreatedAtDesc(AlertStatus.OPEN)).thenReturn(List.of(alert));
        when(alertTransactionEntityRepo.findFirstByAlertAndRelationTypeOrderByCreatedAtAsc(alert, AlertRelationType.TRIGGERING_TRANSACTION))
                .thenReturn(Optional.empty());
        when(alertTransactionEntityRepo.findFirstByAlertOrderByCreatedAtAsc(alert))
                .thenReturn(Optional.empty());

        // when
        List<DashboardRecentAlertItemDTO> response = service.getRecentOpenAlerts();

        // then
        assertEquals(1, response.size());
        assertNull(response.get(0).getRuleName());
    }

    @Test
    void getRecentOpenAlerts_whenRepositoryReturnsEmpty_thenReturnsEmptyList() {
        // given
        when(alertEntityRepository.findTop5ByStatusOrderByCreatedAtDesc(AlertStatus.OPEN)).thenReturn(List.of());

        // when
        List<DashboardRecentAlertItemDTO> response = service.getRecentOpenAlerts();

        // then
        assertTrue(response.isEmpty());
    }

    private AlertEntity alert(String code, SeverityLevel severity, AlertStatus status, String ruleName) {
        AlertEntity alert = new AlertEntity();
        alert.setAlertCode(code);
        alert.setSeverity(severity);
        alert.setStatus(status);
        alert.setCreatedAt(LocalDateTime.of(2026, 8, 6, 16, 0));

        RuleEntity rule = new RuleEntity();
        rule.setName(ruleName);
        alert.setRule(rule);
        return alert;
    }

    private TransactionEntity transaction(String txnId, String accountId) {
        TransactionEntity transaction = new TransactionEntity();
        transaction.setTxnId(txnId);
        transaction.setAccountId(accountId);
        transaction.setPayeeId("PAY-" + txnId);
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setTxnType(TransactionType.DEBIT);
        transaction.setTxnTimestamp(LocalDateTime.of(2026, 8, 6, 16, 5));
        return transaction;
    }

    private AlertTransactionEntity link(AlertEntity alert, TransactionEntity txn) {
        AlertTransactionEntity link = new AlertTransactionEntity();
        link.setAlert(alert);
        link.setTransaction(txn);
        return link;
    }
}

