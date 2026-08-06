package com.FraudBusters.TransactionMonitoring.services;

import com.FraudBusters.TransactionMonitoring.models.dto.CentralRuleEvaluationResponseDTO;
import com.FraudBusters.TransactionMonitoring.services.Impl.CentralRuleEngineServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class TransactionEvaluationScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionEvaluationScheduler.class);
    private final AtomicBoolean evaluationInProgress = new AtomicBoolean(false);

    @Value("${fraud.evaluation.pending.enabled:true}")
    private boolean evaluationEnabled;

    private final CentralRuleEngineServiceImpl centralRuleEngineService;

    public TransactionEvaluationScheduler(CentralRuleEngineServiceImpl centralRuleEngineService) {
        this.centralRuleEngineService = centralRuleEngineService;
    }

    @Scheduled(
            fixedDelayString = "${fraud.evaluation.pending.fixed-delay-ms:5000}",
            initialDelayString = "${fraud.evaluation.pending.initial-delay-ms:5000}")
    public void evaluatePendingTransactions() {
        if (!evaluationEnabled) {
            return;
        }

        if (!evaluationInProgress.compareAndSet(false, true)) {
            LOGGER.debug("Skipping scheduled pending evaluation because previous run is still in progress.");
            return;
        }

        try {
            List<CentralRuleEvaluationResponseDTO> responses = centralRuleEngineService.evaluatePendingTransactionsBatch();
            if (!responses.isEmpty()) {
                LOGGER.info("Scheduled evaluation processed {} pending transaction(s).", responses.size());
            }
        } catch (Exception ex) {
            LOGGER.error("Scheduled pending transaction evaluation failed", ex);
        } finally {
            evaluationInProgress.set(false);
        }
    }
}


