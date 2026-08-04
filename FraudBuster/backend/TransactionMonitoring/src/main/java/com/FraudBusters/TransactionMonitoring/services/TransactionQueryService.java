package com.FraudBusters.TransactionMonitoring.services;

import com.FraudBusters.TransactionMonitoring.dto.TransactionItemDto;
import com.FraudBusters.TransactionMonitoring.dto.TransactionsPageDto;
import com.FraudBusters.TransactionMonitoring.models.AlertTransactionEntity;
import com.FraudBusters.TransactionMonitoring.models.TransactionEntity;
import com.FraudBusters.TransactionMonitoring.models.enums.SeverityLevel;
import com.FraudBusters.TransactionMonitoring.repository.AlertTransactionRepository;
import com.FraudBusters.TransactionMonitoring.repository.TransactionRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TransactionQueryService {

    private final TransactionRepository transactionRepository;
    private final AlertTransactionRepository alertTransactionRepository;

    public TransactionQueryService(TransactionRepository transactionRepository,
                                   AlertTransactionRepository alertTransactionRepository) {
        this.transactionRepository = transactionRepository;
        this.alertTransactionRepository = alertTransactionRepository;
    }

    public TransactionsPageDto getTransactionsPage() {
        List<TransactionEntity> txns = transactionRepository.findTop200ByOrderByTxnTimestampDesc();

        List<Long> txnIds = txns.stream().map(TransactionEntity::getId).toList();
        List<AlertTransactionEntity> links = txnIds.isEmpty()
                ? List.of()
                : alertTransactionRepository.findByTransactionIdsWithAlertAndRule(txnIds);

        Map<Long, AlertTransactionEntity> firstAlertByTxn = new HashMap<>();
        for (AlertTransactionEntity link : links) {
            Long txnId = link.getTransaction().getId();
            firstAlertByTxn.putIfAbsent(txnId, link);
        }

        List<TransactionItemDto> rows = txns.stream()
                .map(txn -> toRow(txn, firstAlertByTxn.get(txn.getId())))
                .toList();

        long total = transactionRepository.countAllTransactions();
        BigDecimal volume = transactionRepository.sumAllAmounts();
        long alertedTxns = alertTransactionRepository.countDistinctTransactionsWithAlerts();

        return new TransactionsPageDto(total, formatMoney(volume), alertedTxns, rows);
    }

    private TransactionItemDto toRow(TransactionEntity txn, AlertTransactionEntity link) {
        boolean hasAlert = link != null;
        String alertSeverity = "";
        String alertLabel = "";

        if (hasAlert) {
            SeverityLevel sev = link.getAlert().getSeverity();
            alertSeverity = sev.name();
            String ruleName = link.getAlert().getRule() != null ? link.getAlert().getRule().getName() : "Alert";
            alertLabel = toSentenceCase(sev.name()) + " Alert" + (ruleName.isBlank() ? "" : " (" + ruleName + ")");
        }

        return new TransactionItemDto(
                txn.getTxnId(),
                txn.getAccountId(),
                txn.getPayeeId(),
                formatMoney(txn.getAmount()),
                txn.getTxnType().name(),
                txn.getTxnTimestamp(),
                hasAlert,
                alertSeverity,
                alertLabel
        );
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) {
            return "$0";
        }
        return "$" + value.stripTrailingZeros().toPlainString();
    }

    private String toSentenceCase(String value) {
        String lower = value.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}


