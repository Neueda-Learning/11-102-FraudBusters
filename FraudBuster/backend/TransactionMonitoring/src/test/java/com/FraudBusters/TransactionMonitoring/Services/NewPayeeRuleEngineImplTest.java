package com.FraudBusters.TransactionMonitoring.Services;

import com.FraudBusters.TransactionMonitoring.models.AlertEntity;
import com.FraudBusters.TransactionMonitoring.models.AlertTransactionEntity;
import com.FraudBusters.TransactionMonitoring.models.RuleEntity;
import com.FraudBusters.TransactionMonitoring.models.TransactionEntity;
import com.FraudBusters.TransactionMonitoring.models.dto.TransactionRequestDTO;
import com.FraudBusters.TransactionMonitoring.models.enums.AlertRelationType;
import com.FraudBusters.TransactionMonitoring.models.enums.AlertStatus;
import com.FraudBusters.TransactionMonitoring.models.enums.FinalDecision;
import com.FraudBusters.TransactionMonitoring.models.enums.MonitorState;
import com.FraudBusters.TransactionMonitoring.models.enums.SeverityLevel;
import com.FraudBusters.TransactionMonitoring.models.enums.TransactionType;
import com.FraudBusters.TransactionMonitoring.repository.AlertEntityRepository;
import com.FraudBusters.TransactionMonitoring.repository.AlertTransactionEntityRepo;
import com.FraudBusters.TransactionMonitoring.repository.RuleEntityRepository;
import com.FraudBusters.TransactionMonitoring.repository.TransactionEntityRepository;
import com.FraudBusters.TransactionMonitoring.services.Impl.NewPayeeRuleEngineImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewPayeeRuleEngineImplTest {

    @Mock
    private AlertEntityRepository alertEntityRepository;
    @Mock
    private RuleEntityRepository ruleEntityRepository;
    @Mock
    private TransactionEntityRepository transactionEntityRepository;
    @Mock
    private AlertTransactionEntityRepo alertTransactionEntityRepository;
    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private NewPayeeRuleEngineImpl service;

    @Test
    void evaluateTransaction_whenRuleMissing_thenThrowsAndSkipsFurtherProcessing() {
        // given
        TransactionEntity transaction = transaction("TXN-4001", MonitorState.RECEIVED, FinalDecision.PENDING);
        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("NEW_PAYEE")).thenReturn(Optional.empty());

        // when
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.evaluateTransaction(transaction)
        );

        // then
        assertEquals("NEW_PAYEE rule is not configured in database", exception.getMessage());
        verify(ruleEntityRepository).findByRuleCodeAndIsDeletedFalse("NEW_PAYEE");
        verifyNoInteractions(transactionEntityRepository, alertEntityRepository, alertTransactionEntityRepository, modelMapper);
    }

    @Test
    void evaluateTransaction_whenExistingTransactionHasFirstTimePayee_thenHoldsAndCreatesAlertAndLink() {
        // given
        TransactionEntity existingTransaction = transaction("TXN-4002", MonitorState.RECEIVED, FinalDecision.PENDING);
        RuleEntity rule = rule("Configured New Payee Rule", "Configured description", SeverityLevel.CRITICAL);

        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("NEW_PAYEE")).thenReturn(Optional.of(rule));
        when(transactionEntityRepository.findByTxnId("TXN-4002")).thenReturn(Optional.of(existingTransaction));
        when(transactionEntityRepository.existsByAccountIdAndPayeeIdAndTxnIdNot(
                existingTransaction.getAccountId(), existingTransaction.getPayeeId(), "TXN-4002")).thenReturn(false);
        when(transactionEntityRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertEntityRepository.save(any(AlertEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertTransactionEntityRepository.save(any(AlertTransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        boolean triggered = service.evaluateTransaction(existingTransaction);

        // then
        assertTrue(triggered);
        assertEquals(MonitorState.HELD, existingTransaction.getMonitorState());
        assertEquals(FinalDecision.PENDING, existingTransaction.getFinalDecision());
        assertNotNull(existingTransaction.getUpdatedAt());
        assertNotNull(existingTransaction.getDecidedAt());
        verify(transactionEntityRepository).findByTxnId("TXN-4002");
        verify(transactionEntityRepository).existsByAccountIdAndPayeeIdAndTxnIdNot(
                existingTransaction.getAccountId(), existingTransaction.getPayeeId(), "TXN-4002");
        verify(transactionEntityRepository).save(existingTransaction);

        ArgumentCaptor<AlertEntity> alertCaptor = ArgumentCaptor.forClass(AlertEntity.class);
        verify(alertEntityRepository).save(alertCaptor.capture());
        AlertEntity savedAlert = alertCaptor.getValue();
        assertEquals("Configured New Payee Rule", savedAlert.getTitle());
        assertEquals("Configured description | Account ACC-TXN-4002 has used payee PAY-TXN-4002 for the first time", savedAlert.getDescription());
        assertTrue(savedAlert.getAlertCode().startsWith("NP-"));
        assertEquals(SeverityLevel.CRITICAL, savedAlert.getSeverity());
        assertSame(rule, savedAlert.getRule());
        assertEquals(AlertStatus.OPEN, savedAlert.getStatus());
        assertNotNull(savedAlert.getCreatedAt());
        assertNotNull(savedAlert.getUpdatedAt());

        ArgumentCaptor<AlertTransactionEntity> linkCaptor = ArgumentCaptor.forClass(AlertTransactionEntity.class);
        verify(alertTransactionEntityRepository).save(linkCaptor.capture());
        AlertTransactionEntity savedLink = linkCaptor.getValue();
        assertSame(existingTransaction, savedLink.getTransaction());
        assertSame(savedAlert, savedLink.getAlert());
        assertEquals(AlertRelationType.TRIGGERING_TRANSACTION, savedLink.getRelationType());
        assertNotNull(savedLink.getCreatedAt());
    }

    @Test
    void evaluateTransaction_whenRuleMetadataBlank_thenUsesFallbackAlertTitleAndDescription() {
        // given
        TransactionEntity existingTransaction = transaction("TXN-4003", MonitorState.RECEIVED, FinalDecision.PENDING);
        RuleEntity rule = rule("   ", "   ", SeverityLevel.HIGH);

        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("NEW_PAYEE")).thenReturn(Optional.of(rule));
        when(transactionEntityRepository.findByTxnId("TXN-4003")).thenReturn(Optional.of(existingTransaction));
        when(transactionEntityRepository.existsByAccountIdAndPayeeIdAndTxnIdNot(
                existingTransaction.getAccountId(), existingTransaction.getPayeeId(), "TXN-4003")).thenReturn(false);
        when(transactionEntityRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertEntityRepository.save(any(AlertEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        boolean triggered = service.evaluateTransaction(existingTransaction);

        // then
        assertTrue(triggered);

        ArgumentCaptor<AlertEntity> alertCaptor = ArgumentCaptor.forClass(AlertEntity.class);
        verify(alertEntityRepository).save(alertCaptor.capture());
        AlertEntity savedAlert = alertCaptor.getValue();
        assertEquals("New Payee Detected", savedAlert.getTitle());
        assertEquals("Account ACC-TXN-4003 has used payee PAY-TXN-4003 for the first time", savedAlert.getDescription());
    }

    @Test
    void evaluateTransaction_whenNewTransactionHasKnownPayee_thenPersistsAndReleasesIt() {
        // given
        TransactionEntity newTransaction = transaction("TXN-4004", null, null);
        RuleEntity rule = rule("New Payee Rule", "Rule description", SeverityLevel.HIGH);

        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("NEW_PAYEE")).thenReturn(Optional.of(rule));
        when(transactionEntityRepository.findByTxnId("TXN-4004")).thenReturn(Optional.empty());
        when(transactionEntityRepository.existsByAccountIdAndPayeeIdAndTxnIdNot(
                newTransaction.getAccountId(), newTransaction.getPayeeId(), "TXN-4004")).thenReturn(true);
        when(transactionEntityRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        boolean triggered = service.evaluateTransaction(newTransaction);

        // then
        assertFalse(triggered);
        verify(transactionEntityRepository).findByTxnId("TXN-4004");
        verify(transactionEntityRepository).existsByAccountIdAndPayeeIdAndTxnIdNot(
                newTransaction.getAccountId(), newTransaction.getPayeeId(), "TXN-4004");
        verify(transactionEntityRepository, times(2)).save(newTransaction);
        assertEquals(MonitorState.RELEASED, newTransaction.getMonitorState());
        assertEquals(FinalDecision.ALLOW, newTransaction.getFinalDecision());
        assertEquals("Payee relation already exists for account ACC-TXN-4004 and payee PAY-TXN-4004", newTransaction.getDecisionReason());
        assertNotNull(newTransaction.getUpdatedAt());
        assertNotNull(newTransaction.getDecidedAt());
        verifyNoInteractions(alertEntityRepository, alertTransactionEntityRepository, modelMapper);
    }

    @Test
    void evaluateTransaction_whenExistingTransactionHasKnownPayee_thenReleasesTransaction() {
        // given
        TransactionEntity existingTransaction = transaction("TXN-4005", MonitorState.RECEIVED, FinalDecision.PENDING);
        RuleEntity rule = rule("New Payee Rule", "Rule description", SeverityLevel.MEDIUM);

        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("NEW_PAYEE")).thenReturn(Optional.of(rule));
        when(transactionEntityRepository.findByTxnId("TXN-4005")).thenReturn(Optional.of(existingTransaction));
        when(transactionEntityRepository.existsByAccountIdAndPayeeIdAndTxnIdNot(
                existingTransaction.getAccountId(), existingTransaction.getPayeeId(), "TXN-4005")).thenReturn(true);
        when(transactionEntityRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        boolean triggered = service.evaluateTransaction(existingTransaction);

        // then
        assertFalse(triggered);
        assertEquals(MonitorState.RELEASED, existingTransaction.getMonitorState());
        assertEquals(FinalDecision.ALLOW, existingTransaction.getFinalDecision());
        assertEquals("Payee relation already exists for account ACC-TXN-4005 and payee PAY-TXN-4005", existingTransaction.getDecisionReason());
        verify(transactionEntityRepository).save(existingTransaction);
        verifyNoInteractions(alertEntityRepository, alertTransactionEntityRepository, modelMapper);
    }

    @Test
    void evaluateTransaction_whenTransactionAlreadyHeldAndPayeeKnown_thenDoesNotOverrideOrSave() {
        // given
        TransactionEntity heldTransaction = transaction("TXN-4006", MonitorState.HELD, FinalDecision.PENDING);
        RuleEntity rule = rule("New Payee Rule", "Rule description", SeverityLevel.HIGH);

        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("NEW_PAYEE")).thenReturn(Optional.of(rule));
        when(transactionEntityRepository.findByTxnId("TXN-4006")).thenReturn(Optional.of(heldTransaction));
        when(transactionEntityRepository.existsByAccountIdAndPayeeIdAndTxnIdNot(
                heldTransaction.getAccountId(), heldTransaction.getPayeeId(), "TXN-4006")).thenReturn(true);

        // when
        boolean triggered = service.evaluateTransaction(heldTransaction);

        // then
        assertFalse(triggered);
        assertEquals(MonitorState.HELD, heldTransaction.getMonitorState());
        assertEquals(FinalDecision.PENDING, heldTransaction.getFinalDecision());
        verify(transactionEntityRepository).findByTxnId("TXN-4006");
        verify(transactionEntityRepository).existsByAccountIdAndPayeeIdAndTxnIdNot(
                heldTransaction.getAccountId(), heldTransaction.getPayeeId(), "TXN-4006");
        verify(transactionEntityRepository, never()).save(any(TransactionEntity.class));
        verifyNoInteractions(alertEntityRepository, alertTransactionEntityRepository, modelMapper);
    }

    @Test
    void evaluateTransaction_whenCalledWithDto_thenMapsDtoAndReturnsDelegatedResult() {
        // given
        TransactionRequestDTO request = TransactionRequestDTO.builder()
                .txnId("TXN-4007")
                .accountId("ACC-4007")
                .payeeId("PAY-4007")
                .amount(new BigDecimal("600.00"))
                .currency("USD")
                .txnType(TransactionType.DEBIT)
                .txnTimestamp(LocalDateTime.now())
                .build();

        TransactionEntity mappedTransaction = transaction("TXN-4007", MonitorState.RECEIVED, FinalDecision.PENDING);
        RuleEntity rule = rule("New Payee Rule", "Rule description", SeverityLevel.LOW);

        when(modelMapper.map(request, TransactionEntity.class)).thenReturn(mappedTransaction);
        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("NEW_PAYEE")).thenReturn(Optional.of(rule));
        when(transactionEntityRepository.findByTxnId("TXN-4007")).thenReturn(Optional.of(mappedTransaction));
        when(transactionEntityRepository.existsByAccountIdAndPayeeIdAndTxnIdNot(
                mappedTransaction.getAccountId(), mappedTransaction.getPayeeId(), "TXN-4007")).thenReturn(true);
        when(transactionEntityRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Optional<Boolean> result = service.evaluateTransaction(request);

        // then
        assertTrue(result.isPresent());
        assertFalse(result.get());
        verify(modelMapper).map(request, TransactionEntity.class);
        verify(transactionEntityRepository).findByTxnId("TXN-4007");
        verify(transactionEntityRepository).existsByAccountIdAndPayeeIdAndTxnIdNot(
                mappedTransaction.getAccountId(), mappedTransaction.getPayeeId(), "TXN-4007");
        verify(transactionEntityRepository).save(mappedTransaction);
        verifyNoInteractions(alertEntityRepository, alertTransactionEntityRepository);
    }

    @Test
    void evaluateTransaction_whenRuleIsPresent_thenUsesResolvedTransactionValuesForRelationLookup() {
        // given
        TransactionEntity incomingTransaction = transaction("TXN-4008", MonitorState.RECEIVED, FinalDecision.PENDING);
        TransactionEntity persistedTransaction = transaction("TXN-4008", MonitorState.RECEIVED, FinalDecision.PENDING);
        persistedTransaction.setAccountId("ACC-PERSISTED");
        persistedTransaction.setPayeeId("PAY-PERSISTED");
        RuleEntity rule = rule("New Payee Rule", "Rule description", SeverityLevel.HIGH);

        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("NEW_PAYEE")).thenReturn(Optional.of(rule));
        when(transactionEntityRepository.findByTxnId("TXN-4008")).thenReturn(Optional.of(persistedTransaction));
        when(transactionEntityRepository.existsByAccountIdAndPayeeIdAndTxnIdNot(
                "ACC-PERSISTED", "PAY-PERSISTED", "TXN-4008")).thenReturn(true);
        when(transactionEntityRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        boolean triggered = service.evaluateTransaction(incomingTransaction);

        // then
        assertFalse(triggered);
        verify(transactionEntityRepository).existsByAccountIdAndPayeeIdAndTxnIdNot(
                "ACC-PERSISTED", "PAY-PERSISTED", "TXN-4008");
        verifyNoMoreInteractions(alertEntityRepository, alertTransactionEntityRepository);
    }

    private RuleEntity rule(String name, String description, SeverityLevel severity) {
        RuleEntity rule = new RuleEntity();
        rule.setRuleCode("NEW_PAYEE");
        rule.setName(name);
        rule.setDescription(description);
        rule.setSeverityDefault(severity);
        rule.setIsDeleted(false);
        rule.setIsActive(true);
        return rule;
    }

    private TransactionEntity transaction(String txnId, MonitorState monitorState, FinalDecision finalDecision) {
        TransactionEntity transaction = new TransactionEntity();
        transaction.setTxnId(txnId);
        transaction.setAccountId("ACC-" + txnId);
        transaction.setPayeeId("PAY-" + txnId);
        transaction.setAmount(new BigDecimal("500.00"));
        transaction.setCurrency("USD");
        transaction.setTxnType(TransactionType.DEBIT);
        transaction.setTxnTimestamp(LocalDateTime.of(2026, 8, 6, 13, 0));
        transaction.setMonitorState(monitorState);
        transaction.setFinalDecision(finalDecision);
        return transaction;
    }
}

