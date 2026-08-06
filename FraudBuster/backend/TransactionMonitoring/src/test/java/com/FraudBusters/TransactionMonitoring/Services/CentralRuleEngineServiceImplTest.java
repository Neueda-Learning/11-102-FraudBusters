package com.FraudBusters.TransactionMonitoring.Services;

import com.FraudBusters.TransactionMonitoring.models.RuleEntity;
import com.FraudBusters.TransactionMonitoring.models.TransactionDecisionEntity;
import com.FraudBusters.TransactionMonitoring.models.TransactionEntity;
import com.FraudBusters.TransactionMonitoring.models.dto.CentralRuleEvaluationResponseDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.TransactionRequestDTO;
import com.FraudBusters.TransactionMonitoring.models.enums.DecisionType;
import com.FraudBusters.TransactionMonitoring.models.enums.FinalDecision;
import com.FraudBusters.TransactionMonitoring.models.enums.MonitorState;
import com.FraudBusters.TransactionMonitoring.models.enums.SeverityLevel;
import com.FraudBusters.TransactionMonitoring.models.enums.TransactionType;
import com.FraudBusters.TransactionMonitoring.repository.RuleEntityRepository;
import com.FraudBusters.TransactionMonitoring.repository.TransactionDecisionEntityRepo;
import com.FraudBusters.TransactionMonitoring.repository.TransactionEntityRepository;
import com.FraudBusters.TransactionMonitoring.services.Impl.*;
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
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CentralRuleEngineServiceImplTest {

    @Mock
    private RuleEntityRepository ruleEntityRepository;
    @Mock
    private TransactionEntityRepository transactionEntityRepository;
    @Mock
    private TransactionDecisionEntityRepo transactionDecisionEntityRepo;
    @Mock
    private ModelMapper modelMapper;
    @Mock
    private AmountThresholdRuleEngineServiceImpl amountThresholdRuleEngineService;
    @Mock
    private DailyLimitRuleEngineServiceImpl dailyLimitRuleEngineService;
    @Mock
    private NewPayeeRuleEngineImpl newPayeeRuleEngine;
    @Mock
    private VelocityCheckRuleEngineImpl velocityCheckRuleEngine;

    @InjectMocks
    private CentralRuleEngineServiceImpl service;

    @Test
    void evaluateAgainstActiveRules_whenTransactionRequestDtoProvided_thenMapsAndDelegatesToEntityEvaluation() {
        // given
        TransactionRequestDTO request = TransactionRequestDTO.builder()
                .txnId("TXN-2001")
                .accountId("ACC-2001")
                .payeeId("PAY-2001")
                .amount(new BigDecimal("1500.00"))
                .currency("USD")
                .txnType(TransactionType.DEBIT)
                .txnTimestamp(LocalDateTime.now())
                .build();

        TransactionEntity mappedTransaction = transaction("TXN-2001", MonitorState.RECEIVED, FinalDecision.PENDING);
        RuleEntity amountRule = rule(2L, "AMOUNT_THRESHOLD");

        when(modelMapper.map(request, TransactionEntity.class)).thenReturn(mappedTransaction);
        when(transactionEntityRepository.findByTxnId("TXN-2001")).thenReturn(Optional.of(mappedTransaction), Optional.of(mappedTransaction));
        when(ruleEntityRepository.findByIsActiveTrueAndIsDeletedFalse()).thenReturn(List.of(amountRule));
        when(amountThresholdRuleEngineService.evaluateTransaction(mappedTransaction)).thenReturn(false);
        when(transactionEntityRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionDecisionEntityRepo.existsByTransactionTxnId("TXN-2001")).thenReturn(false);
        when(transactionDecisionEntityRepo.save(any(TransactionDecisionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        CentralRuleEvaluationResponseDTO response = service.evaluateAgainstActiveRules(request);

        // then
        assertEquals("TXN-2001", response.getTxnId());
        assertFalse(response.isAnyRuleTriggered());
        assertEquals(1, response.getEvaluatedRuleCount());
        assertEquals(Boolean.FALSE, response.getRuleResults().get("AMOUNT_THRESHOLD"));
        verify(modelMapper).map(request, TransactionEntity.class);
        verify(amountThresholdRuleEngineService).evaluateTransaction(mappedTransaction);
    }

    @Test
    void evaluateAgainstActiveRules_whenNewTransactionAndAnyRuleTriggers_thenPersistsAsReceivedAndFinallyHeld() {
        // given
        TransactionEntity incomingTransaction = transaction("TXN-2002", null, null);
        RuleEntity velocityRule = rule(3L, "VELOCITY_CHECK");
        RuleEntity amountRule = rule(1L, "AMOUNT_THRESHOLD");
        RuleEntity unknownRule = rule(5L, "UNKNOWN_RULE");

        when(transactionEntityRepository.findByTxnId("TXN-2002")).thenReturn(Optional.empty(), Optional.of(incomingTransaction));
        when(transactionEntityRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ruleEntityRepository.findByIsActiveTrueAndIsDeletedFalse()).thenReturn(List.of(velocityRule, unknownRule, amountRule));
        when(amountThresholdRuleEngineService.evaluateTransaction(incomingTransaction)).thenReturn(true);
        when(velocityCheckRuleEngine.evaluateTransaction(incomingTransaction)).thenReturn(false);

        // when
        CentralRuleEvaluationResponseDTO response = service.evaluateAgainstActiveRules(incomingTransaction);

        // then
        assertTrue(response.isAnyRuleTriggered());
        assertEquals(2, response.getEvaluatedRuleCount());
        assertEquals(Map.of("AMOUNT_THRESHOLD", true, "VELOCITY_CHECK", false), response.getRuleResults());
        assertEquals(List.of("UNKNOWN_RULE"), response.getSkippedRuleCodes());
        assertEquals(MonitorState.HELD, response.getMonitorState());
        assertEquals(FinalDecision.PENDING, response.getFinalDecision());
        assertEquals("Held by central rule engine: one or more rules triggered", response.getDecisionReason());

        verify(amountThresholdRuleEngineService).evaluateTransaction(incomingTransaction);
        verify(velocityCheckRuleEngine).evaluateTransaction(incomingTransaction);
        verifyNoInteractions(dailyLimitRuleEngineService, newPayeeRuleEngine, modelMapper);

        assertEquals(MonitorState.HELD, incomingTransaction.getMonitorState());
        assertEquals(FinalDecision.PENDING, incomingTransaction.getFinalDecision());
        assertNotNull(incomingTransaction.getUpdatedAt());
        assertNotNull(incomingTransaction.getDecidedAt());
        verify(transactionEntityRepository, times(2)).save(incomingTransaction);
        verify(transactionDecisionEntityRepo, never()).save(any(TransactionDecisionEntity.class));
    }

    @Test
    void evaluateAgainstActiveRules_whenNoRuleTriggersAndTransactionWasNotHeld_thenReleasesAndCreatesAllowDecisionOnce() {
        // given
        TransactionEntity existingTransaction = transaction("TXN-2003", MonitorState.RECEIVED, FinalDecision.PENDING);
        RuleEntity amountRule = rule(4L, "AMOUNT_THRESHOLD");
        RuleEntity dailyRule = rule(2L, "DAILY_LIMIT");
        RuleEntity payeeRule = rule(3L, "NEW_PAYEE");

        when(transactionEntityRepository.findByTxnId("TXN-2003")).thenReturn(Optional.of(existingTransaction), Optional.of(existingTransaction));
        when(ruleEntityRepository.findByIsActiveTrueAndIsDeletedFalse()).thenReturn(List.of(amountRule, dailyRule, payeeRule));
        when(dailyLimitRuleEngineService.evaluateTransaction(existingTransaction)).thenReturn(false);
        when(newPayeeRuleEngine.evaluateTransaction(existingTransaction)).thenReturn(false);
        when(amountThresholdRuleEngineService.evaluateTransaction(existingTransaction)).thenReturn(false);
        when(transactionEntityRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionDecisionEntityRepo.existsByTransactionTxnId("TXN-2003")).thenReturn(false);
        when(transactionDecisionEntityRepo.save(any(TransactionDecisionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        CentralRuleEvaluationResponseDTO response = service.evaluateAgainstActiveRules(existingTransaction);

        // then
        assertFalse(response.isAnyRuleTriggered());
        assertEquals(3, response.getEvaluatedRuleCount());
        assertEquals(List.of(), response.getSkippedRuleCodes());
        assertEquals(MonitorState.RELEASED, response.getMonitorState());
        assertEquals(FinalDecision.ALLOW, response.getFinalDecision());
        assertEquals("Passed all active rules in central rule engine", response.getDecisionReason());

        ArgumentCaptor<TransactionDecisionEntity> decisionCaptor = ArgumentCaptor.forClass(TransactionDecisionEntity.class);
        verify(transactionDecisionEntityRepo).save(decisionCaptor.capture());
        TransactionDecisionEntity savedDecision = decisionCaptor.getValue();
        assertSame(existingTransaction, savedDecision.getTransaction());
        assertEquals(DecisionType.ALLOW, savedDecision.getDecision());
        assertEquals("SYSTEM", savedDecision.getDecidedBy());
        assertEquals("No active rule triggered in central rule engine", savedDecision.getDecisionReason());

        assertEquals(MonitorState.RELEASED, existingTransaction.getMonitorState());
        assertEquals(FinalDecision.ALLOW, existingTransaction.getFinalDecision());
        assertNotNull(existingTransaction.getUpdatedAt());
        assertNotNull(existingTransaction.getDecidedAt());
        verify(transactionEntityRepository).save(existingTransaction);
    }

    @Test
    void evaluateAgainstActiveRules_whenAllowDecisionAlreadyExists_thenDoesNotCreateDuplicateAuditRecord() {
        // given
        TransactionEntity existingTransaction = transaction("TXN-2004", MonitorState.RECEIVED, FinalDecision.PENDING);
        RuleEntity amountRule = rule(1L, "AMOUNT_THRESHOLD");

        when(transactionEntityRepository.findByTxnId("TXN-2004")).thenReturn(Optional.of(existingTransaction), Optional.of(existingTransaction));
        when(ruleEntityRepository.findByIsActiveTrueAndIsDeletedFalse()).thenReturn(List.of(amountRule));
        when(amountThresholdRuleEngineService.evaluateTransaction(existingTransaction)).thenReturn(false);
        when(transactionEntityRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionDecisionEntityRepo.existsByTransactionTxnId("TXN-2004")).thenReturn(true);

        // when
        CentralRuleEvaluationResponseDTO response = service.evaluateAgainstActiveRules(existingTransaction);

        // then
        assertFalse(response.isAnyRuleTriggered());
        assertEquals(MonitorState.RELEASED, response.getMonitorState());
        assertEquals(FinalDecision.ALLOW, response.getFinalDecision());
        verify(transactionDecisionEntityRepo).existsByTransactionTxnId("TXN-2004");
        verify(transactionDecisionEntityRepo, never()).save(any(TransactionDecisionEntity.class));
    }

    @Test
    void evaluateAgainstActiveRules_whenTransactionWasHeldBeforeAndNoRuleTriggers_thenDoesNotOverrideStateOrCreateDecision() {
        // given
        TransactionEntity heldTransaction = transaction("TXN-2005", MonitorState.HELD, FinalDecision.PENDING);
        RuleEntity amountRule = rule(1L, "AMOUNT_THRESHOLD");
        RuleEntity dailyRule = rule(2L, "DAILY_LIMIT");

        when(transactionEntityRepository.findByTxnId("TXN-2005")).thenReturn(Optional.of(heldTransaction), Optional.of(heldTransaction));
        when(ruleEntityRepository.findByIsActiveTrueAndIsDeletedFalse()).thenReturn(List.of(amountRule, dailyRule));
        when(amountThresholdRuleEngineService.evaluateTransaction(heldTransaction)).thenReturn(false);
        when(dailyLimitRuleEngineService.evaluateTransaction(heldTransaction)).thenReturn(false);

        // when
        CentralRuleEvaluationResponseDTO response = service.evaluateAgainstActiveRules(heldTransaction);

        // then
        assertFalse(response.isAnyRuleTriggered());
        assertEquals(MonitorState.HELD, response.getMonitorState());
        assertEquals(FinalDecision.PENDING, response.getFinalDecision());
        assertEquals(2, response.getEvaluatedRuleCount());
        verify(transactionEntityRepository, never()).save(any(TransactionEntity.class));
        verifyNoInteractions(transactionDecisionEntityRepo);
    }

    @Test
    void evaluatePendingTransactionsBatch_whenPendingTransactionsExist_thenEvaluatesEachAndReturnsResponsesInOrder() {
        // given
        TransactionEntity txn1 = transaction("TXN-2006", MonitorState.RECEIVED, FinalDecision.PENDING);
        TransactionEntity txn2 = transaction("TXN-2007", MonitorState.RECEIVED, FinalDecision.PENDING);
        RuleEntity amountRule = rule(1L, "AMOUNT_THRESHOLD");

        when(transactionEntityRepository.findTop100ByMonitorStateAndFinalDecisionOrderByTxnTimestampAsc(
                MonitorState.RECEIVED, FinalDecision.PENDING)).thenReturn(List.of(txn1, txn2));

        when(transactionEntityRepository.findByTxnId("TXN-2006")).thenReturn(Optional.of(txn1), Optional.of(txn1));
        when(transactionEntityRepository.findByTxnId("TXN-2007")).thenReturn(Optional.of(txn2), Optional.of(txn2));
        when(ruleEntityRepository.findByIsActiveTrueAndIsDeletedFalse()).thenReturn(List.of(amountRule));
        when(amountThresholdRuleEngineService.evaluateTransaction(any(TransactionEntity.class))).thenReturn(false);
        when(transactionEntityRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionDecisionEntityRepo.existsByTransactionTxnId("TXN-2006")).thenReturn(false);
        when(transactionDecisionEntityRepo.existsByTransactionTxnId("TXN-2007")).thenReturn(false);
        when(transactionDecisionEntityRepo.save(any(TransactionDecisionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        List<CentralRuleEvaluationResponseDTO> responses = service.evaluatePendingTransactionsBatch();

        // then
        assertEquals(2, responses.size());
        assertEquals("TXN-2006", responses.get(0).getTxnId());
        assertEquals("TXN-2007", responses.get(1).getTxnId());
        assertEquals(MonitorState.RELEASED, responses.get(0).getMonitorState());
        assertEquals(MonitorState.RELEASED, responses.get(1).getMonitorState());
        verify(amountThresholdRuleEngineService).evaluateTransaction(txn1);
        verify(amountThresholdRuleEngineService).evaluateTransaction(txn2);
        verify(transactionDecisionEntityRepo, times(2)).save(any(TransactionDecisionEntity.class));
    }

    @Test
    void evaluatePendingTransactionsBatch_whenNoPendingTransactions_thenReturnsEmptyList() {
        // given
        when(transactionEntityRepository.findTop100ByMonitorStateAndFinalDecisionOrderByTxnTimestampAsc(
                MonitorState.RECEIVED, FinalDecision.PENDING)).thenReturn(List.of());

        // when
        List<CentralRuleEvaluationResponseDTO> responses = service.evaluatePendingTransactionsBatch();

        // then
        assertTrue(responses.isEmpty());
        verify(transactionEntityRepository).findTop100ByMonitorStateAndFinalDecisionOrderByTxnTimestampAsc(
                MonitorState.RECEIVED, FinalDecision.PENDING);
        verifyNoInteractions(ruleEntityRepository, transactionDecisionEntityRepo, modelMapper,
                amountThresholdRuleEngineService, dailyLimitRuleEngineService, newPayeeRuleEngine, velocityCheckRuleEngine);
    }

    private RuleEntity rule(Long id, String ruleCode) {
        RuleEntity rule = new RuleEntity();
        rule.setId(id);
        rule.setRuleCode(ruleCode);
        rule.setName(ruleCode + " Rule");
        rule.setDescription(ruleCode + " description");
        rule.setSeverityDefault(SeverityLevel.HIGH);
        rule.setIsActive(true);
        rule.setIsDeleted(false);
        return rule;
    }

    private TransactionEntity transaction(String txnId, MonitorState monitorState, FinalDecision finalDecision) {
        TransactionEntity transaction = new TransactionEntity();
        transaction.setTxnId(txnId);
        transaction.setAccountId("ACC-" + txnId);
        transaction.setPayeeId("PAY-" + txnId);
        transaction.setAmount(new BigDecimal("1000.00"));
        transaction.setCurrency("USD");
        transaction.setTxnType(TransactionType.DEBIT);
        transaction.setTxnTimestamp(LocalDateTime.of(2026, 8, 6, 11, 0));
        transaction.setMonitorState(monitorState);
        transaction.setFinalDecision(finalDecision);
        return transaction;
    }
}

