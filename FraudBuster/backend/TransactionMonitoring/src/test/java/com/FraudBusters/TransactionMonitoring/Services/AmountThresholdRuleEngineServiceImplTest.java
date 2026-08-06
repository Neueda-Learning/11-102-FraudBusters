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
import com.FraudBusters.TransactionMonitoring.services.Impl.AmountThresholdRuleEngineServiceImpl;
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
class AmountThresholdRuleEngineServiceImplTest {

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
    private AmountThresholdRuleEngineServiceImpl service;

    @Test
    void evaluateTransaction_whenAmountThresholdRuleMissing_thenThrowsAndSkipsFurtherProcessing() {
        // given
        TransactionEntity transaction = transaction("TXN-1001", "6000.00", MonitorState.RECEIVED, FinalDecision.PENDING);
        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("AMOUNT_THRESHOLD")).thenReturn(Optional.empty());

        // when
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.evaluateTransaction(transaction)
        );

        // then
        assertEquals("AMOUNT_THRESHOLD rule is not configured in database", exception.getMessage());
        verify(ruleEntityRepository).findByRuleCodeAndIsDeletedFalse("AMOUNT_THRESHOLD");
        verifyNoInteractions(transactionEntityRepository, alertEntityRepository, alertTransactionEntityRepository, modelMapper);
    }

    @Test
    void evaluateTransaction_whenConfigJsonIsBlank_thenThrowsValidationError() {
        // given
        TransactionEntity existingTransaction = transaction("TXN-1002", "3000.00", MonitorState.RECEIVED, FinalDecision.PENDING);
        RuleEntity rule = rule("   ", "Amount Rule", "Alert on high amount", SeverityLevel.HIGH);

        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("AMOUNT_THRESHOLD")).thenReturn(Optional.of(rule));
        when(transactionEntityRepository.findByTxnId("TXN-1002")).thenReturn(Optional.of(existingTransaction));

        // when
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.evaluateTransaction(existingTransaction)
        );

        // then
        assertEquals("AMOUNT_THRESHOLD rule config_json is empty", exception.getMessage());
        verify(ruleEntityRepository).findByRuleCodeAndIsDeletedFalse("AMOUNT_THRESHOLD");
        verify(transactionEntityRepository).findByTxnId("TXN-1002");
        verifyNoMoreInteractions(transactionEntityRepository);
        verifyNoInteractions(alertEntityRepository, alertTransactionEntityRepository, modelMapper);
    }

    @Test
    void evaluateTransaction_whenThresholdAmountMissingInConfig_thenThrowsValidationError() {
        // given
        TransactionEntity existingTransaction = transaction("TXN-1003", "3000.00", MonitorState.RECEIVED, FinalDecision.PENDING);
        RuleEntity rule = rule("{\"limit\": 2500}", "Amount Rule", "Alert on high amount", SeverityLevel.HIGH);

        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("AMOUNT_THRESHOLD")).thenReturn(Optional.of(rule));
        when(transactionEntityRepository.findByTxnId("TXN-1003")).thenReturn(Optional.of(existingTransaction));

        // when
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.evaluateTransaction(existingTransaction)
        );

        // then
        assertEquals("thresholdAmount is missing in AMOUNT_THRESHOLD rule config_json", exception.getMessage());
        verify(ruleEntityRepository).findByRuleCodeAndIsDeletedFalse("AMOUNT_THRESHOLD");
        verify(transactionEntityRepository).findByTxnId("TXN-1003");
        verifyNoMoreInteractions(transactionEntityRepository);
        verifyNoInteractions(alertEntityRepository, alertTransactionEntityRepository, modelMapper);
    }

    @Test
    void evaluateTransaction_whenExistingTransactionExceedsThreshold_thenHoldsTransactionAndCreatesAlertAndLink() {
        // given
        TransactionEntity existingTransaction = transaction("TXN-1004", "5000.00", MonitorState.RECEIVED, FinalDecision.PENDING);
        RuleEntity rule = rule("{\"thresholdAmount\": 2500}", "Configured Amount Rule", "Configured rule description", SeverityLevel.CRITICAL);

        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("AMOUNT_THRESHOLD")).thenReturn(Optional.of(rule));
        when(transactionEntityRepository.findByTxnId("TXN-1004")).thenReturn(Optional.of(existingTransaction));
        when(transactionEntityRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertEntityRepository.save(any(AlertEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertTransactionEntityRepository.save(any(AlertTransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        boolean flagged = service.evaluateTransaction(existingTransaction);

        // then
        assertTrue(flagged);
        assertEquals(MonitorState.HELD, existingTransaction.getMonitorState());
        assertNotNull(existingTransaction.getUpdatedAt());
        assertNotNull(existingTransaction.getDecidedAt());
        verify(transactionEntityRepository).findByTxnId("TXN-1004");
        verify(transactionEntityRepository).save(existingTransaction);

        ArgumentCaptor<AlertEntity> alertCaptor = ArgumentCaptor.forClass(AlertEntity.class);
        verify(alertEntityRepository).save(alertCaptor.capture());
        AlertEntity savedAlert = alertCaptor.getValue();
        assertEquals("Configured Amount Rule", savedAlert.getTitle());
        assertEquals("Configured rule description | Transaction amount 5000.00 exceeds configured threshold 2500", savedAlert.getDescription());
        assertTrue(savedAlert.getAlertCode().startsWith("AMT-"));
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
        TransactionEntity existingTransaction = transaction("TXN-1005", "3000.00", MonitorState.RECEIVED, FinalDecision.PENDING);
        RuleEntity rule = rule("{\"thresholdAmount\": 2500}", "   ", "   ", SeverityLevel.HIGH);

        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("AMOUNT_THRESHOLD")).thenReturn(Optional.of(rule));
        when(transactionEntityRepository.findByTxnId("TXN-1005")).thenReturn(Optional.of(existingTransaction));
        when(transactionEntityRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertEntityRepository.save(any(AlertEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        boolean flagged = service.evaluateTransaction(existingTransaction);

        // then
        assertTrue(flagged);

        ArgumentCaptor<AlertEntity> alertCaptor = ArgumentCaptor.forClass(AlertEntity.class);
        verify(alertEntityRepository).save(alertCaptor.capture());
        AlertEntity savedAlert = alertCaptor.getValue();
        assertEquals("High Amount Transaction Detected", savedAlert.getTitle());
        assertEquals("Transaction amount 3000.00 exceeds configured threshold 2500", savedAlert.getDescription());
    }

    @Test
    void evaluateTransaction_whenNewTransactionIsBelowThreshold_thenPersistsNewTransactionAndReleasesIt() {
        // given
        TransactionEntity newTransaction = transaction("TXN-1006", "1200.00", null, null);
        RuleEntity rule = rule("{\"thresholdAmount\": 2500}", "Amount Rule", "Rule description", SeverityLevel.HIGH);

        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("AMOUNT_THRESHOLD")).thenReturn(Optional.of(rule));
        when(transactionEntityRepository.findByTxnId("TXN-1006")).thenReturn(Optional.empty());
        when(transactionEntityRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        boolean flagged = service.evaluateTransaction(newTransaction);

        // then
        assertFalse(flagged);
        verify(transactionEntityRepository).findByTxnId("TXN-1006");
        verify(transactionEntityRepository, times(2)).save(newTransaction);
        assertEquals(MonitorState.RELEASED, newTransaction.getMonitorState());
        assertEquals(FinalDecision.ALLOW, newTransaction.getFinalDecision());
        assertEquals("Transaction amount is within the defined threshold of 2500", newTransaction.getDecisionReason());
        assertNotNull(newTransaction.getCreatedAt());
        assertNotNull(newTransaction.getUpdatedAt());
        assertNotNull(newTransaction.getDecidedAt());
        verifyNoInteractions(alertEntityRepository, alertTransactionEntityRepository, modelMapper);
    }

    @Test
    void evaluateTransaction_whenAmountEqualsThreshold_thenReturnsFalseAndReleasesTransaction() {
        // given
        TransactionEntity existingTransaction = transaction("TXN-1007", "2500.00", MonitorState.RECEIVED, FinalDecision.PENDING);
        RuleEntity rule = rule("{\"thresholdAmount\": 2500}", "Amount Rule", "Rule description", SeverityLevel.MEDIUM);

        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("AMOUNT_THRESHOLD")).thenReturn(Optional.of(rule));
        when(transactionEntityRepository.findByTxnId("TXN-1007")).thenReturn(Optional.of(existingTransaction));
        when(transactionEntityRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        boolean flagged = service.evaluateTransaction(existingTransaction);

        // then
        assertFalse(flagged);
        assertEquals(MonitorState.RELEASED, existingTransaction.getMonitorState());
        assertEquals(FinalDecision.ALLOW, existingTransaction.getFinalDecision());
        verify(transactionEntityRepository).save(existingTransaction);
        verifyNoInteractions(alertEntityRepository, alertTransactionEntityRepository, modelMapper);
    }

    @Test
    void evaluateTransaction_whenExistingTransactionAlreadyHeldAndBelowThreshold_thenDoesNotOverrideOrSave() {
        // given
        TransactionEntity heldTransaction = transaction("TXN-1008", "1500.00", MonitorState.HELD, FinalDecision.PENDING);
        RuleEntity rule = rule("{\"thresholdAmount\": 2500}", "Amount Rule", "Rule description", SeverityLevel.HIGH);

        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("AMOUNT_THRESHOLD")).thenReturn(Optional.of(rule));
        when(transactionEntityRepository.findByTxnId("TXN-1008")).thenReturn(Optional.of(heldTransaction));

        // when
        boolean flagged = service.evaluateTransaction(heldTransaction);

        // then
        assertFalse(flagged);
        assertEquals(MonitorState.HELD, heldTransaction.getMonitorState());
        assertEquals(FinalDecision.PENDING, heldTransaction.getFinalDecision());
        verify(transactionEntityRepository).findByTxnId("TXN-1008");
        verify(transactionEntityRepository, never()).save(any(TransactionEntity.class));
        verifyNoInteractions(alertEntityRepository, alertTransactionEntityRepository, modelMapper);
    }

    @Test
    void evaluateTransaction_whenCalledWithDto_thenMapsDtoAndReturnsDelegatedResult() {
        // given
        TransactionRequestDTO request = TransactionRequestDTO.builder()
                .txnId("TXN-1009")
                .accountId("ACC-1009")
                .payeeId("PAY-1009")
                .amount(new BigDecimal("500.00"))
                .currency("USD")
                .txnType(TransactionType.DEBIT)
                .txnTimestamp(LocalDateTime.now())
                .build();

        TransactionEntity mappedTransaction = transaction("TXN-1009", "500.00", MonitorState.RECEIVED, FinalDecision.PENDING);
        RuleEntity rule = rule("{\"thresholdAmount\": 2500}", "Amount Rule", "Rule description", SeverityLevel.LOW);

        when(modelMapper.map(request, TransactionEntity.class)).thenReturn(mappedTransaction);
        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("AMOUNT_THRESHOLD")).thenReturn(Optional.of(rule));
        when(transactionEntityRepository.findByTxnId("TXN-1009")).thenReturn(Optional.of(mappedTransaction));
        when(transactionEntityRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Optional<Boolean> result = service.evaluateTransaction(request);

        // then
        assertTrue(result.isPresent());
        assertFalse(result.get());
        verify(modelMapper).map(request, TransactionEntity.class);
        verify(transactionEntityRepository).findByTxnId("TXN-1009");
        verify(transactionEntityRepository).save(mappedTransaction);
        verifyNoInteractions(alertEntityRepository, alertTransactionEntityRepository);
    }

    private RuleEntity rule(String configJson, String name, String description, SeverityLevel severity) {
        RuleEntity rule = new RuleEntity();
        rule.setRuleCode("AMOUNT_THRESHOLD");
        rule.setConfigJson(configJson);
        rule.setName(name);
        rule.setDescription(description);
        rule.setSeverityDefault(severity);
        rule.setIsDeleted(false);
        rule.setIsActive(true);
        return rule;
    }

    private TransactionEntity transaction(String txnId, String amount, MonitorState monitorState, FinalDecision finalDecision) {
        TransactionEntity transaction = new TransactionEntity();
        transaction.setTxnId(txnId);
        transaction.setAccountId("ACC-" + txnId);
        transaction.setPayeeId("PAY-" + txnId);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setCurrency("USD");
        transaction.setTxnType(TransactionType.DEBIT);
        transaction.setTxnTimestamp(LocalDateTime.of(2026, 8, 6, 10, 0));
        transaction.setMonitorState(monitorState);
        transaction.setFinalDecision(finalDecision);
        return transaction;
    }
}

