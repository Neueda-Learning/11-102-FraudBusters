package com.FraudBusters.TransactionMonitoring.Services;

import com.FraudBusters.TransactionMonitoring.models.AlertEntity;
import com.FraudBusters.TransactionMonitoring.models.AlertTransactionEntity;
import com.FraudBusters.TransactionMonitoring.models.RuleEntity;
import com.FraudBusters.TransactionMonitoring.models.TransactionEntity;
import com.FraudBusters.TransactionMonitoring.models.dto.TransactionRequestDTO;
import com.FraudBusters.TransactionMonitoring.models.enums.AlertRelationType;
import com.FraudBusters.TransactionMonitoring.models.enums.AlertStatus;
import com.FraudBusters.TransactionMonitoring.models.enums.DecisionType;
import com.FraudBusters.TransactionMonitoring.models.enums.FinalDecision;
import com.FraudBusters.TransactionMonitoring.models.enums.MonitorState;
import com.FraudBusters.TransactionMonitoring.models.enums.SeverityLevel;
import com.FraudBusters.TransactionMonitoring.models.enums.TransactionType;
import com.FraudBusters.TransactionMonitoring.repository.AlertEntityRepository;
import com.FraudBusters.TransactionMonitoring.repository.AlertTransactionEntityRepo;
import com.FraudBusters.TransactionMonitoring.repository.RuleEntityRepository;
import com.FraudBusters.TransactionMonitoring.repository.TransactionDecisionEntityRepo;
import com.FraudBusters.TransactionMonitoring.repository.TransactionEntityRepository;
import com.FraudBusters.TransactionMonitoring.services.Impl.DailyLimitRuleEngineServiceImpl;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyLimitRuleEngineServiceImplTest {

    @Mock
    private AlertEntityRepository alertEntityRepository;
    @Mock
    private RuleEntityRepository ruleEntityRepository;
    @Mock
    private TransactionEntityRepository transactionEntityRepository;
    @Mock
    private AlertTransactionEntityRepo alertTransactionEntityRepository;
    @Mock
    private TransactionDecisionEntityRepo transactionDecisionEntityRepo;
    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private DailyLimitRuleEngineServiceImpl service;

    @Test
    void evaluateTransaction_whenDailyLimitRuleMissing_thenThrowsAndSkipsFurtherProcessing() {
        // given
        TransactionEntity transaction = transaction("TXN-3001", "800.00", MonitorState.RECEIVED, FinalDecision.PENDING);
        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("DAILY_LIMIT")).thenReturn(Optional.empty());

        // when
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.evaluateTransaction(transaction)
        );

        // then
        assertEquals("DAILY_LIMIT rule is not configured in database", exception.getMessage());
        verify(ruleEntityRepository).findByRuleCodeAndIsDeletedFalse("DAILY_LIMIT");
        verifyNoInteractions(transactionEntityRepository, alertEntityRepository,
                alertTransactionEntityRepository, transactionDecisionEntityRepo, modelMapper);
    }

    @Test
    void evaluateTransaction_whenConfigJsonIsBlank_thenThrowsValidationError() {
        // given
        TransactionEntity existingTransaction = transaction("TXN-3002", "900.00", MonitorState.RECEIVED, FinalDecision.PENDING);
        RuleEntity rule = rule("   ", "Daily Limit Rule", "Rule description", SeverityLevel.HIGH);

        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("DAILY_LIMIT")).thenReturn(Optional.of(rule));
        when(transactionEntityRepository.findByTxnId("TXN-3002")).thenReturn(Optional.of(existingTransaction));

        // when
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.evaluateTransaction(existingTransaction)
        );

        // then
        assertEquals("DAILY_LIMIT rule config_json is empty", exception.getMessage());
        verify(ruleEntityRepository).findByRuleCodeAndIsDeletedFalse("DAILY_LIMIT");
        verify(transactionEntityRepository).findByTxnId("TXN-3002");
        verifyNoMoreInteractions(transactionEntityRepository);
        verifyNoInteractions(alertEntityRepository, alertTransactionEntityRepository, transactionDecisionEntityRepo, modelMapper);
    }

    @Test
    void evaluateTransaction_whenDailyLimitAmountMissingInConfig_thenThrowsValidationError() {
        // given
        TransactionEntity existingTransaction = transaction("TXN-3003", "950.00", MonitorState.RECEIVED, FinalDecision.PENDING);
        RuleEntity rule = rule("{\"limit\": 2500}", "Daily Limit Rule", "Rule description", SeverityLevel.HIGH);

        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("DAILY_LIMIT")).thenReturn(Optional.of(rule));
        when(transactionEntityRepository.findByTxnId("TXN-3003")).thenReturn(Optional.of(existingTransaction));

        // when
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.evaluateTransaction(existingTransaction)
        );

        // then
        assertEquals("dailyLimitAmount is missing in DAILY_LIMIT rule config_json", exception.getMessage());
        verify(ruleEntityRepository).findByRuleCodeAndIsDeletedFalse("DAILY_LIMIT");
        verify(transactionEntityRepository).findByTxnId("TXN-3003");
        verifyNoMoreInteractions(transactionEntityRepository);
        verifyNoInteractions(alertEntityRepository, alertTransactionEntityRepository, transactionDecisionEntityRepo, modelMapper);
    }

    @Test
    void evaluateTransaction_whenProjectedTotalExceedsLimit_thenHoldsTransactionAndCreatesAlertAndLink() {
        // given
        TransactionEntity existingTransaction = transaction("TXN-3004", "1200.00", MonitorState.RECEIVED, FinalDecision.PENDING);
        RuleEntity rule = rule("{\"dailyLimitAmount\": 2500}", "Configured Daily Rule", "Configured daily description", SeverityLevel.CRITICAL);

        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("DAILY_LIMIT")).thenReturn(Optional.of(rule));
        when(transactionEntityRepository.findByTxnId("TXN-3004")).thenReturn(Optional.of(existingTransaction));
        when(transactionDecisionEntityRepo.sumAllowedDebitTransactionsForAccountByDay(
                eq(existingTransaction.getAccountId()), any(LocalDateTime.class), any(LocalDateTime.class),
                eq(DecisionType.ALLOW), eq("TXN-3004")))
                .thenReturn(new BigDecimal("1500.00"));
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
        verify(transactionEntityRepository).findByTxnId("TXN-3004");
        verify(transactionDecisionEntityRepo).sumAllowedDebitTransactionsForAccountByDay(
                eq(existingTransaction.getAccountId()), any(LocalDateTime.class), any(LocalDateTime.class),
                eq(DecisionType.ALLOW), eq("TXN-3004"));
        verify(transactionEntityRepository).save(existingTransaction);

        ArgumentCaptor<AlertEntity> alertCaptor = ArgumentCaptor.forClass(AlertEntity.class);
        verify(alertEntityRepository).save(alertCaptor.capture());
        AlertEntity savedAlert = alertCaptor.getValue();
        assertEquals("Configured Daily Rule", savedAlert.getTitle());
        assertEquals("Configured daily description | Account ACC-TXN-3004 projected daily total 2700.00 exceeds configured daily limit 2500", savedAlert.getDescription());
        assertTrue(savedAlert.getAlertCode().startsWith("DL-"));
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
        TransactionEntity existingTransaction = transaction("TXN-3005", "1000.00", MonitorState.RECEIVED, FinalDecision.PENDING);
        RuleEntity rule = rule("{\"dailyLimitAmount\": 1500}", "   ", "   ", SeverityLevel.HIGH);

        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("DAILY_LIMIT")).thenReturn(Optional.of(rule));
        when(transactionEntityRepository.findByTxnId("TXN-3005")).thenReturn(Optional.of(existingTransaction));
        when(transactionDecisionEntityRepo.sumAllowedDebitTransactionsForAccountByDay(anyString(), any(LocalDateTime.class), any(LocalDateTime.class), eq(DecisionType.ALLOW), anyString()))
                .thenReturn(new BigDecimal("600.00"));
        when(transactionEntityRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertEntityRepository.save(any(AlertEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        boolean flagged = service.evaluateTransaction(existingTransaction);

        // then
        assertTrue(flagged);

        ArgumentCaptor<AlertEntity> alertCaptor = ArgumentCaptor.forClass(AlertEntity.class);
        verify(alertEntityRepository).save(alertCaptor.capture());
        AlertEntity savedAlert = alertCaptor.getValue();
        assertEquals("Daily Limit Exceeded", savedAlert.getTitle());
        assertEquals("Account ACC-TXN-3005 projected daily total 1600.00 exceeds configured daily limit 1500", savedAlert.getDescription());
    }

    @Test
    void evaluateTransaction_whenNewTransactionIsWithinLimit_thenPersistsNewTransactionAndReleasesIt() {
        // given
        TransactionEntity newTransaction = transaction("TXN-3006", "700.00", null, null);
        RuleEntity rule = rule("{\"dailyLimitAmount\": 2500}", "Daily Rule", "Rule description", SeverityLevel.HIGH);

        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("DAILY_LIMIT")).thenReturn(Optional.of(rule));
        when(transactionEntityRepository.findByTxnId("TXN-3006")).thenReturn(Optional.empty());
        when(transactionDecisionEntityRepo.sumAllowedDebitTransactionsForAccountByDay(
                eq(newTransaction.getAccountId()), any(LocalDateTime.class), any(LocalDateTime.class),
                eq(DecisionType.ALLOW), eq("TXN-3006")))
                .thenReturn(new BigDecimal("1000.00"));
        when(transactionEntityRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        boolean flagged = service.evaluateTransaction(newTransaction);

        // then
        assertFalse(flagged);
        verify(transactionEntityRepository).findByTxnId("TXN-3006");
        verify(transactionEntityRepository, times(2)).save(newTransaction);
        assertEquals(MonitorState.RELEASED, newTransaction.getMonitorState());
        assertEquals(FinalDecision.ALLOW, newTransaction.getFinalDecision());
        assertEquals("Daily total of 1700.00 is within the daily limit of 2500", newTransaction.getDecisionReason());
        assertNotNull(newTransaction.getUpdatedAt());
        assertNotNull(newTransaction.getDecidedAt());
        verifyNoInteractions(alertEntityRepository, alertTransactionEntityRepository, modelMapper);
    }

    @Test
    void evaluateTransaction_whenProjectedTotalEqualsLimit_thenReturnsFalseAndReleasesTransaction() {
        // given
        TransactionEntity existingTransaction = transaction("TXN-3007", "500.00", MonitorState.RECEIVED, FinalDecision.PENDING);
        RuleEntity rule = rule("{\"dailyLimitAmount\": 2500}", "Daily Rule", "Rule description", SeverityLevel.MEDIUM);

        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("DAILY_LIMIT")).thenReturn(Optional.of(rule));
        when(transactionEntityRepository.findByTxnId("TXN-3007")).thenReturn(Optional.of(existingTransaction));
        when(transactionDecisionEntityRepo.sumAllowedDebitTransactionsForAccountByDay(
                eq(existingTransaction.getAccountId()), any(LocalDateTime.class), any(LocalDateTime.class),
                eq(DecisionType.ALLOW), eq("TXN-3007")))
                .thenReturn(new BigDecimal("2000.00"));
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
    void evaluateTransaction_whenTransactionAlreadyHeldAndProjectedTotalWithinLimit_thenDoesNotOverrideOrSave() {
        // given
        TransactionEntity heldTransaction = transaction("TXN-3008", "400.00", MonitorState.HELD, FinalDecision.PENDING);
        RuleEntity rule = rule("{\"dailyLimitAmount\": 2500}", "Daily Rule", "Rule description", SeverityLevel.HIGH);

        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("DAILY_LIMIT")).thenReturn(Optional.of(rule));
        when(transactionEntityRepository.findByTxnId("TXN-3008")).thenReturn(Optional.of(heldTransaction));
        when(transactionDecisionEntityRepo.sumAllowedDebitTransactionsForAccountByDay(
                eq(heldTransaction.getAccountId()), any(LocalDateTime.class), any(LocalDateTime.class),
                eq(DecisionType.ALLOW), eq("TXN-3008")))
                .thenReturn(new BigDecimal("1800.00"));

        // when
        boolean flagged = service.evaluateTransaction(heldTransaction);

        // then
        assertFalse(flagged);
        assertEquals(MonitorState.HELD, heldTransaction.getMonitorState());
        assertEquals(FinalDecision.PENDING, heldTransaction.getFinalDecision());
        verify(transactionEntityRepository).findByTxnId("TXN-3008");
        verify(transactionEntityRepository, never()).save(any(TransactionEntity.class));
        verifyNoInteractions(alertEntityRepository, alertTransactionEntityRepository, modelMapper);
    }

    @Test
    void evaluateTransaction_whenCalledWithDto_thenMapsDtoAndReturnsDelegatedResult() {
        // given
        TransactionRequestDTO request = TransactionRequestDTO.builder()
                .txnId("TXN-3009")
                .accountId("ACC-3009")
                .payeeId("PAY-3009")
                .amount(new BigDecimal("300.00"))
                .currency("USD")
                .txnType(TransactionType.DEBIT)
                .txnTimestamp(LocalDateTime.now())
                .build();

        TransactionEntity mappedTransaction = transaction("TXN-3009", "300.00", MonitorState.RECEIVED, FinalDecision.PENDING);
        RuleEntity rule = rule("{\"dailyLimitAmount\": 2500}", "Daily Rule", "Rule description", SeverityLevel.LOW);

        when(modelMapper.map(request, TransactionEntity.class)).thenReturn(mappedTransaction);
        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("DAILY_LIMIT")).thenReturn(Optional.of(rule));
        when(transactionEntityRepository.findByTxnId("TXN-3009")).thenReturn(Optional.of(mappedTransaction));
        when(transactionDecisionEntityRepo.sumAllowedDebitTransactionsForAccountByDay(
                eq(mappedTransaction.getAccountId()), any(LocalDateTime.class), any(LocalDateTime.class),
                eq(DecisionType.ALLOW), eq("TXN-3009")))
                .thenReturn(new BigDecimal("1700.00"));
        when(transactionEntityRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Optional<Boolean> result = service.evaluateTransaction(request);

        // then
        assertTrue(result.isPresent());
        assertFalse(result.get());
        verify(modelMapper).map(request, TransactionEntity.class);
        verify(transactionEntityRepository).findByTxnId("TXN-3009");
        verify(transactionEntityRepository).save(mappedTransaction);
        verifyNoInteractions(alertEntityRepository, alertTransactionEntityRepository);
    }

    private RuleEntity rule(String configJson, String name, String description, SeverityLevel severity) {
        RuleEntity rule = new RuleEntity();
        rule.setRuleCode("DAILY_LIMIT");
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
        transaction.setTxnTimestamp(LocalDateTime.of(2026, 8, 6, 12, 0));
        transaction.setMonitorState(monitorState);
        transaction.setFinalDecision(finalDecision);
        return transaction;
    }
}

