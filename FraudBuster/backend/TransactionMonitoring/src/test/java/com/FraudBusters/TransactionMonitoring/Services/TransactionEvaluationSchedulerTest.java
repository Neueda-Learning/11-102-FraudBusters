package com.FraudBusters.TransactionMonitoring.services;

import com.FraudBusters.TransactionMonitoring.models.dto.CentralRuleEvaluationResponseDTO;
import com.FraudBusters.TransactionMonitoring.services.Impl.CentralRuleEngineServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionEvaluationSchedulerTest {

    @Mock
    private CentralRuleEngineServiceImpl centralRuleEngineService;

    @InjectMocks
    private TransactionEvaluationScheduler scheduler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "evaluationEnabled", true);
    }

    @Test
    void evaluatePendingTransactions_whenEvaluationDisabled_thenReturnsEarlyWithoutServiceCall() {
        // given
        ReflectionTestUtils.setField(scheduler, "evaluationEnabled", false);

        // when
        scheduler.evaluatePendingTransactions();

        // then
        verifyNoInteractions(centralRuleEngineService);
        assertFalse(isEvaluationInProgress());
    }

    @Test
    void evaluatePendingTransactions_whenPreviousRunInProgress_thenSkipsCurrentRun() {
        // given
        setEvaluationInProgress(true);

        // when
        scheduler.evaluatePendingTransactions();

        // then
        verifyNoInteractions(centralRuleEngineService);
        assertTrue(isEvaluationInProgress());
    }

    @Test
    void evaluatePendingTransactions_whenBatchReturnsEmpty_thenCallsServiceAndReleasesLock() {
        // given
        when(centralRuleEngineService.evaluatePendingTransactionsBatch()).thenReturn(List.of());

        // when
        scheduler.evaluatePendingTransactions();

        // then
        verify(centralRuleEngineService).evaluatePendingTransactionsBatch();
        assertFalse(isEvaluationInProgress());
    }

    @Test
    void evaluatePendingTransactions_whenBatchReturnsItems_thenCallsServiceAndReleasesLock() {
        // given
        CentralRuleEvaluationResponseDTO dto = CentralRuleEvaluationResponseDTO.builder()
                .txnId("TXN-9001")
                .anyRuleTriggered(false)
                .build();
        when(centralRuleEngineService.evaluatePendingTransactionsBatch()).thenReturn(List.of(dto));

        // when
        scheduler.evaluatePendingTransactions();

        // then
        verify(centralRuleEngineService).evaluatePendingTransactionsBatch();
        assertFalse(isEvaluationInProgress());
    }

    @Test
    void evaluatePendingTransactions_whenServiceThrows_thenSwallowsExceptionAndReleasesLock() {
        // given
        when(centralRuleEngineService.evaluatePendingTransactionsBatch())
                .thenThrow(new RuntimeException("simulated failure"));

        // when
        assertDoesNotThrow(() -> scheduler.evaluatePendingTransactions());

        // then
        verify(centralRuleEngineService).evaluatePendingTransactionsBatch();
        assertFalse(isEvaluationInProgress());
    }

    @Test
    void evaluatePendingTransactions_whenServiceReturnsNull_thenHandlesNpeAndReleasesLock() {
        // given
        when(centralRuleEngineService.evaluatePendingTransactionsBatch()).thenReturn(null);

        // when
        assertDoesNotThrow(() -> scheduler.evaluatePendingTransactions());

        // then
        verify(centralRuleEngineService).evaluatePendingTransactionsBatch();
        assertFalse(isEvaluationInProgress());
    }

    @Test
    void evaluatePendingTransactions_whenRunCompletes_thenNextRunCanProceed() {
        // given
        when(centralRuleEngineService.evaluatePendingTransactionsBatch()).thenReturn(List.of(), List.of());

        // when
        scheduler.evaluatePendingTransactions();
        scheduler.evaluatePendingTransactions();

        // then
        verify(centralRuleEngineService, times(2)).evaluatePendingTransactionsBatch();
        assertFalse(isEvaluationInProgress());
    }

    private boolean isEvaluationInProgress() {
        AtomicBoolean inProgress = (AtomicBoolean) ReflectionTestUtils.getField(scheduler, "evaluationInProgress");
        return inProgress != null && inProgress.get();
    }

    private void setEvaluationInProgress(boolean value) {
        AtomicBoolean inProgress = (AtomicBoolean) ReflectionTestUtils.getField(scheduler, "evaluationInProgress");
        if (inProgress != null) {
            inProgress.set(value);
        }
    }
}

