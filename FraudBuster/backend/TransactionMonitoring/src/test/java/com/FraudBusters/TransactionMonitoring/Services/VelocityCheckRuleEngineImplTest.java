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
import com.FraudBusters.TransactionMonitoring.services.Impl.VelocityCheckRuleEngineImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
class VelocityCheckRuleEngineImplTest {

    @Mock
    private AlertEntityRepository alertEntityRepository;
    @Mock
    private RuleEntityRepository ruleEntityRepository;
    @Mock
    private TransactionEntityRepository transactionEntityRepository;
    @Mock
    private TransactionDecisionEntityRepo transactionDecisionEntityRepo;
    @Mock
    private AlertTransactionEntityRepo alertTransactionEntityRepository;
    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private VelocityCheckRuleEngineImpl service;

    @Test
    void evaluateTransaction_whenRuleMissing_thenThrowsAndSkipsFurtherProcessing() {
        // given
        TransactionEntity transaction = transaction("TXN-5001", MonitorState.RECEIVED, FinalDecision.PENDING, LocalDateTime.of(2026, 8, 6, 14, 0));
        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("VELOCITY_CHECK")).thenReturn(Optional.empty());

        // when
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.evaluateTransaction(transaction)
        );

        // then
        assertEquals("VELOCITY_CHECK rule is not configured in database", exception.getMessage());
        verify(ruleEntityRepository).findByRuleCodeAndIsDeletedFalse("VELOCITY_CHECK");
        verifyNoInteractions(transactionEntityRepository, transactionDecisionEntityRepo,
                alertEntityRepository, alertTransactionEntityRepository, modelMapper);
    }

    @Test
    void evaluateTransaction_whenConfigJsonIsBlank_thenThrowsValidationError() {
        // given
        TransactionEntity existingTransaction = transaction("TXN-5002", MonitorState.RECEIVED, FinalDecision.PENDING, LocalDateTime.of(2026, 8, 6, 14, 0));
        RuleEntity rule = rule("   ", "Velocity Rule", "Velocity description", SeverityLevel.HIGH);

        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("VELOCITY_CHECK")).thenReturn(Optional.of(rule));
        when(transactionEntityRepository.findByTxnId("TXN-5002")).thenReturn(Optional.of(existingTransaction));

        // when
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.evaluateTransaction(existingTransaction)
        );

        // then
        assertEquals("VELOCITY_CHECK rule config_json is empty", exception.getMessage());
        verify(ruleEntityRepository).findByRuleCodeAndIsDeletedFalse("VELOCITY_CHECK");
        verify(transactionEntityRepository).findByTxnId("TXN-5002");
        verifyNoMoreInteractions(transactionEntityRepository);
        verifyNoInteractions(transactionDecisionEntityRepo, alertEntityRepository, alertTransactionEntityRepository, modelMapper);
    }

    @Test
    void evaluateTransaction_whenWindowMinutesMissing_thenThrowsValidationError() {
        // given
        TransactionEntity existingTransaction = transaction("TXN-5003", MonitorState.RECEIVED, FinalDecision.PENDING, LocalDateTime.of(2026, 8, 6, 14, 0));
        RuleEntity rule = rule("{\"maxTransactions\": 3}", "Velocity Rule", "Velocity description", SeverityLevel.HIGH);

        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("VELOCITY_CHECK")).thenReturn(Optional.of(rule));
        when(transactionEntityRepository.findByTxnId("TXN-5003")).thenReturn(Optional.of(existingTransaction));

        // when
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.evaluateTransaction(existingTransaction)
        );

        // then
        assertEquals("windowMinutes is missing in VELOCITY_CHECK rule config_json", exception.getMessage());
        verify(ruleEntityRepository).findByRuleCodeAndIsDeletedFalse("VELOCITY_CHECK");
        verify(transactionEntityRepository).findByTxnId("TXN-5003");
        verifyNoMoreInteractions(transactionEntityRepository);
        verifyNoInteractions(transactionDecisionEntityRepo, alertEntityRepository, alertTransactionEntityRepository, modelMapper);
    }

    @Test
    void evaluateTransaction_whenMaxTransactionsMissing_thenThrowsValidationError() {
        // given
        TransactionEntity existingTransaction = transaction("TXN-5004", MonitorState.RECEIVED, FinalDecision.PENDING, LocalDateTime.of(2026, 8, 6, 14, 0));
        RuleEntity rule = rule("{\"windowMinutes\": 5}", "Velocity Rule", "Velocity description", SeverityLevel.HIGH);

        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("VELOCITY_CHECK")).thenReturn(Optional.of(rule));
        when(transactionEntityRepository.findByTxnId("TXN-5004")).thenReturn(Optional.of(existingTransaction));

        // when
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.evaluateTransaction(existingTransaction)
        );

        // then
        assertEquals("maxTransactions is missing in VELOCITY_CHECK rule config_json", exception.getMessage());
        verify(ruleEntityRepository).findByRuleCodeAndIsDeletedFalse("VELOCITY_CHECK");
        verify(transactionEntityRepository).findByTxnId("TXN-5004");
        verifyNoMoreInteractions(transactionEntityRepository);
        verifyNoInteractions(transactionDecisionEntityRepo, alertEntityRepository, alertTransactionEntityRepository, modelMapper);
    }

    @Test
    void evaluateTransaction_whenWindowMinutesIsZero_thenThrowsValidationError() {
        // given
        TransactionEntity existingTransaction = transaction("TXN-5005", MonitorState.RECEIVED, FinalDecision.PENDING, LocalDateTime.of(2026, 8, 6, 14, 0));
        RuleEntity rule = rule("{\"windowMinutes\": 0, \"maxTransactions\": 3}", "Velocity Rule", "Velocity description", SeverityLevel.HIGH);

        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("VELOCITY_CHECK")).thenReturn(Optional.of(rule));
        when(transactionEntityRepository.findByTxnId("TXN-5005")).thenReturn(Optional.of(existingTransaction));

        // when
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.evaluateTransaction(existingTransaction)
        );

        // then
        assertEquals("windowMinutes must be greater than 0 for VELOCITY_CHECK", exception.getMessage());
        verify(ruleEntityRepository).findByRuleCodeAndIsDeletedFalse("VELOCITY_CHECK");
        verify(transactionEntityRepository).findByTxnId("TXN-5005");
        verifyNoMoreInteractions(transactionEntityRepository);
        verifyNoInteractions(transactionDecisionEntityRepo, alertEntityRepository, alertTransactionEntityRepository, modelMapper);
    }

    @Test
    void evaluateTransaction_whenMaxTransactionsIsZero_thenThrowsValidationError() {
        // given
        TransactionEntity existingTransaction = transaction("TXN-5006", MonitorState.RECEIVED, FinalDecision.PENDING, LocalDateTime.of(2026, 8, 6, 14, 0));
        RuleEntity rule = rule("{\"windowMinutes\": 5, \"maxTransactions\": 0}", "Velocity Rule", "Velocity description", SeverityLevel.HIGH);

        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("VELOCITY_CHECK")).thenReturn(Optional.of(rule));
        when(transactionEntityRepository.findByTxnId("TXN-5006")).thenReturn(Optional.of(existingTransaction));

        // when
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.evaluateTransaction(existingTransaction)
        );

        // then
        assertEquals("maxTransactions must be greater than 0 for VELOCITY_CHECK", exception.getMessage());
        verify(ruleEntityRepository).findByRuleCodeAndIsDeletedFalse("VELOCITY_CHECK");
        verify(transactionEntityRepository).findByTxnId("TXN-5006");
        verifyNoMoreInteractions(transactionEntityRepository);
        verifyNoInteractions(transactionDecisionEntityRepo, alertEntityRepository, alertTransactionEntityRepository, modelMapper);
    }

    @Test
    void evaluateTransaction_whenPriorAllowedCountReachesThreshold_thenHoldsAndCreatesTriggeringAndRelatedLinks() {
        // given
        TransactionEntity existingTransaction = transaction("TXN-5007", MonitorState.RECEIVED, FinalDecision.PENDING, LocalDateTime.of(2026, 8, 6, 14, 30));
        TransactionEntity related1 = transaction("TXN-OLD-1", MonitorState.RELEASED, FinalDecision.ALLOW, LocalDateTime.of(2026, 8, 6, 14, 26));
        TransactionEntity related2 = transaction("TXN-OLD-2", MonitorState.RELEASED, FinalDecision.ALLOW, LocalDateTime.of(2026, 8, 6, 14, 27));
        TransactionEntity related3 = transaction("TXN-OLD-3", MonitorState.RELEASED, FinalDecision.ALLOW, LocalDateTime.of(2026, 8, 6, 14, 28));
        RuleEntity rule = rule("{\"windowMinutes\": 5, \"maxTransactions\": 2}", "Configured Velocity Rule", "Configured velocity description", SeverityLevel.CRITICAL);

        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("VELOCITY_CHECK")).thenReturn(Optional.of(rule));
        when(transactionEntityRepository.findByTxnId("TXN-5007")).thenReturn(Optional.of(existingTransaction));
        when(transactionDecisionEntityRepo.findAllowedDebitTransactionsForAccountInWindow(
                eq(existingTransaction.getAccountId()), any(LocalDateTime.class), eq(existingTransaction.getTxnTimestamp()),
                eq(DecisionType.ALLOW), eq("TXN-5007")))
                .thenReturn(List.of(related1, related2, related3));
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
        verify(transactionDecisionEntityRepo).findAllowedDebitTransactionsForAccountInWindow(
                eq(existingTransaction.getAccountId()), any(LocalDateTime.class), eq(existingTransaction.getTxnTimestamp()),
                eq(DecisionType.ALLOW), eq("TXN-5007"));
        verify(transactionEntityRepository).save(existingTransaction);

        ArgumentCaptor<AlertEntity> alertCaptor = ArgumentCaptor.forClass(AlertEntity.class);
        verify(alertEntityRepository).save(alertCaptor.capture());
        AlertEntity savedAlert = alertCaptor.getValue();
        assertEquals("Configured Velocity Rule", savedAlert.getTitle());
        assertEquals("Configured velocity description | Account ACC-TXN-5007 has 4 transactions within 5 minute window", savedAlert.getDescription());
        assertTrue(savedAlert.getAlertCode().startsWith("VEL-"));
        assertEquals(SeverityLevel.CRITICAL, savedAlert.getSeverity());
        assertSame(rule, savedAlert.getRule());
        assertEquals(AlertStatus.OPEN, savedAlert.getStatus());
        assertNotNull(savedAlert.getCreatedAt());
        assertNotNull(savedAlert.getUpdatedAt());

        ArgumentCaptor<AlertTransactionEntity> linkCaptor = ArgumentCaptor.forClass(AlertTransactionEntity.class);
        verify(alertTransactionEntityRepository, times(3)).save(linkCaptor.capture());
        List<AlertTransactionEntity> links = linkCaptor.getAllValues();

        assertSame(existingTransaction, links.get(0).getTransaction());
        assertSame(savedAlert, links.get(0).getAlert());
        assertEquals(AlertRelationType.TRIGGERING_TRANSACTION, links.get(0).getRelationType());

        assertSame(related1, links.get(1).getTransaction());
        assertEquals(AlertRelationType.RELATED_TRANSACTION, links.get(1).getRelationType());
        assertSame(related2, links.get(2).getTransaction());
        assertEquals(AlertRelationType.RELATED_TRANSACTION, links.get(2).getRelationType());
    }

    @Test
    void evaluateTransaction_whenRuleMetadataBlank_thenUsesFallbackAlertTitleAndDescription() {
        // given
        TransactionEntity existingTransaction = transaction("TXN-5008", MonitorState.RECEIVED, FinalDecision.PENDING, LocalDateTime.of(2026, 8, 6, 14, 30));
        TransactionEntity related = transaction("TXN-OLD-4", MonitorState.RELEASED, FinalDecision.ALLOW, LocalDateTime.of(2026, 8, 6, 14, 29));
        RuleEntity rule = rule("{\"windowMinutes\": 5, \"maxTransactions\": 1}", "   ", "   ", SeverityLevel.HIGH);

        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("VELOCITY_CHECK")).thenReturn(Optional.of(rule));
        when(transactionEntityRepository.findByTxnId("TXN-5008")).thenReturn(Optional.of(existingTransaction));
        when(transactionDecisionEntityRepo.findAllowedDebitTransactionsForAccountInWindow(anyString(), any(LocalDateTime.class), any(LocalDateTime.class), eq(DecisionType.ALLOW), eq("TXN-5008")))
                .thenReturn(List.of(related));
        when(transactionEntityRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertEntityRepository.save(any(AlertEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        boolean triggered = service.evaluateTransaction(existingTransaction);

        // then
        assertTrue(triggered);

        ArgumentCaptor<AlertEntity> alertCaptor = ArgumentCaptor.forClass(AlertEntity.class);
        verify(alertEntityRepository).save(alertCaptor.capture());
        AlertEntity savedAlert = alertCaptor.getValue();
        assertEquals("Velocity Rule Triggered", savedAlert.getTitle());
        assertEquals("Account ACC-TXN-5008 has 2 transactions within 5 minute window", savedAlert.getDescription());
    }

    @Test
    void evaluateTransaction_whenNewTransactionHasNoVelocityBreach_thenPersistsAndReleasesIt() {
        // given
        TransactionEntity newTransaction = transaction("TXN-5009", null, null, LocalDateTime.of(2026, 8, 6, 14, 30));
        RuleEntity rule = rule("{\"windowMinutes\": 10, \"maxTransactions\": 3}", "Velocity Rule", "Velocity description", SeverityLevel.HIGH);

        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("VELOCITY_CHECK")).thenReturn(Optional.of(rule));
        when(transactionEntityRepository.findByTxnId("TXN-5009")).thenReturn(Optional.empty());
        when(transactionDecisionEntityRepo.findAllowedDebitTransactionsForAccountInWindow(
                eq(newTransaction.getAccountId()), any(LocalDateTime.class), eq(newTransaction.getTxnTimestamp()),
                eq(DecisionType.ALLOW), eq("TXN-5009")))
                .thenReturn(List.of(transaction("TXN-OLD-5", MonitorState.RELEASED, FinalDecision.ALLOW, LocalDateTime.of(2026, 8, 6, 14, 25))));
        when(transactionEntityRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        boolean triggered = service.evaluateTransaction(newTransaction);

        // then
        assertFalse(triggered);
        verify(transactionEntityRepository).findByTxnId("TXN-5009");
        verify(transactionEntityRepository, times(2)).save(newTransaction);
        assertEquals(MonitorState.RELEASED, newTransaction.getMonitorState());
        assertEquals(FinalDecision.ALLOW, newTransaction.getFinalDecision());
        assertEquals("Velocity check passed: 2 transaction(s) within 10 minutes, threshold is 3", newTransaction.getDecisionReason());
        assertNotNull(newTransaction.getUpdatedAt());
        assertNotNull(newTransaction.getDecidedAt());
        verifyNoInteractions(alertEntityRepository, alertTransactionEntityRepository, modelMapper);
    }

    @Test
    void evaluateTransaction_whenTransactionAlreadyHeldAndVelocityDoesNotTrigger_thenDoesNotOverrideOrSave() {
        // given
        TransactionEntity heldTransaction = transaction("TXN-5010", MonitorState.HELD, FinalDecision.PENDING, LocalDateTime.of(2026, 8, 6, 14, 30));
        RuleEntity rule = rule("{\"windowMinutes\": 10, \"maxTransactions\": 3}", "Velocity Rule", "Velocity description", SeverityLevel.HIGH);

        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("VELOCITY_CHECK")).thenReturn(Optional.of(rule));
        when(transactionEntityRepository.findByTxnId("TXN-5010")).thenReturn(Optional.of(heldTransaction));
        when(transactionDecisionEntityRepo.findAllowedDebitTransactionsForAccountInWindow(
                eq(heldTransaction.getAccountId()), any(LocalDateTime.class), eq(heldTransaction.getTxnTimestamp()),
                eq(DecisionType.ALLOW), eq("TXN-5010")))
                .thenReturn(List.of(transaction("TXN-OLD-6", MonitorState.RELEASED, FinalDecision.ALLOW, LocalDateTime.of(2026, 8, 6, 14, 25))));

        // when
        boolean triggered = service.evaluateTransaction(heldTransaction);

        // then
        assertFalse(triggered);
        assertEquals(MonitorState.HELD, heldTransaction.getMonitorState());
        assertEquals(FinalDecision.PENDING, heldTransaction.getFinalDecision());
        verify(transactionEntityRepository).findByTxnId("TXN-5010");
        verify(transactionEntityRepository, never()).save(any(TransactionEntity.class));
        verifyNoInteractions(alertEntityRepository, alertTransactionEntityRepository, modelMapper);
    }

    @Test
    void evaluateTransaction_whenTxnTimestampIsNull_thenUsesCurrentTimeAndStillReleases() {
        // given
        TransactionEntity existingTransaction = transaction("TXN-5011", MonitorState.RECEIVED, FinalDecision.PENDING, null);
        RuleEntity rule = rule("{\"windowMinutes\": 15, \"maxTransactions\": 3}", "Velocity Rule", "Velocity description", SeverityLevel.MEDIUM);

        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("VELOCITY_CHECK")).thenReturn(Optional.of(rule));
        when(transactionEntityRepository.findByTxnId("TXN-5011")).thenReturn(Optional.of(existingTransaction));
        when(transactionDecisionEntityRepo.findAllowedDebitTransactionsForAccountInWindow(
                eq(existingTransaction.getAccountId()), any(LocalDateTime.class), any(LocalDateTime.class),
                eq(DecisionType.ALLOW), eq("TXN-5011")))
                .thenReturn(List.of());
        when(transactionEntityRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        boolean triggered = service.evaluateTransaction(existingTransaction);

        // then
        assertFalse(triggered);
        assertEquals(MonitorState.RELEASED, existingTransaction.getMonitorState());
        assertEquals(FinalDecision.ALLOW, existingTransaction.getFinalDecision());
        assertEquals("Velocity check passed: 1 transaction(s) within 15 minutes, threshold is 3", existingTransaction.getDecisionReason());
        verify(transactionDecisionEntityRepo).findAllowedDebitTransactionsForAccountInWindow(
                eq(existingTransaction.getAccountId()), any(LocalDateTime.class), any(LocalDateTime.class),
                eq(DecisionType.ALLOW), eq("TXN-5011"));
    }

    @Test
    void evaluateTransaction_whenCalledWithDto_thenMapsDtoAndReturnsDelegatedResult() {
        // given
        TransactionRequestDTO request = TransactionRequestDTO.builder()
                .txnId("TXN-5012")
                .accountId("ACC-5012")
                .payeeId("PAY-5012")
                .amount(new BigDecimal("900.00"))
                .currency("USD")
                .txnType(TransactionType.DEBIT)
                .txnTimestamp(LocalDateTime.now())
                .build();

        TransactionEntity mappedTransaction = transaction("TXN-5012", MonitorState.RECEIVED, FinalDecision.PENDING, LocalDateTime.of(2026, 8, 6, 14, 30));
        RuleEntity rule = rule("{\"windowMinutes\": 10, \"maxTransactions\": 3}", "Velocity Rule", "Velocity description", SeverityLevel.LOW);

        when(modelMapper.map(request, TransactionEntity.class)).thenReturn(mappedTransaction);
        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("VELOCITY_CHECK")).thenReturn(Optional.of(rule));
        when(transactionEntityRepository.findByTxnId("TXN-5012")).thenReturn(Optional.of(mappedTransaction));
        when(transactionDecisionEntityRepo.findAllowedDebitTransactionsForAccountInWindow(
                eq(mappedTransaction.getAccountId()), any(LocalDateTime.class), eq(mappedTransaction.getTxnTimestamp()),
                eq(DecisionType.ALLOW), eq("TXN-5012")))
                .thenReturn(List.of(transaction("TXN-OLD-7", MonitorState.RELEASED, FinalDecision.ALLOW, LocalDateTime.of(2026, 8, 6, 14, 25))));
        when(transactionEntityRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Optional<Boolean> result = service.evaluateTransaction(request);

        // then
        assertTrue(result.isPresent());
        assertFalse(result.get());
        verify(modelMapper).map(request, TransactionEntity.class);
        verify(transactionEntityRepository).findByTxnId("TXN-5012");
        verify(transactionEntityRepository).save(mappedTransaction);
        verifyNoInteractions(alertEntityRepository, alertTransactionEntityRepository);
    }

    private RuleEntity rule(String configJson, String name, String description, SeverityLevel severity) {
        RuleEntity rule = new RuleEntity();
        rule.setRuleCode("VELOCITY_CHECK");
        rule.setConfigJson(configJson);
        rule.setName(name);
        rule.setDescription(description);
        rule.setSeverityDefault(severity);
        rule.setIsDeleted(false);
        rule.setIsActive(true);
        return rule;
    }

    private TransactionEntity transaction(String txnId, MonitorState monitorState, FinalDecision finalDecision, LocalDateTime txnTimestamp) {
        TransactionEntity transaction = new TransactionEntity();
        transaction.setTxnId(txnId);
        transaction.setAccountId("ACC-" + txnId);
        transaction.setPayeeId("PAY-" + txnId);
        transaction.setAmount(new BigDecimal("750.00"));
        transaction.setCurrency("USD");
        transaction.setTxnType(TransactionType.DEBIT);
        transaction.setTxnTimestamp(txnTimestamp);
        transaction.setMonitorState(monitorState);
        transaction.setFinalDecision(finalDecision);
        return transaction;
    }
}

