package com.FraudBusters.TransactionMonitoring.services;

import com.FraudBusters.TransactionMonitoring.exceptions.ResourceNotFoundException;
import com.FraudBusters.TransactionMonitoring.models.TransactionEntity;
import com.FraudBusters.TransactionMonitoring.models.dto.TransactionResponseDTO;
import com.FraudBusters.TransactionMonitoring.repository.TransactionEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionEntityRepository transactionEntityRepository;

    /**
     * Returns all transactions stored in the system.
     * Used by GET /api/transactions
     */
    public List<TransactionResponseDTO> getAllTransactions() {

        return transactionEntityRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Returns a single transaction by its business txnId.
     * Throws ResourceNotFoundException (404) if not found.
     * Used by GET /api/transactions/{txnId}
     */
    public TransactionResponseDTO getTransactionByTxnId(String txnId) {
        TransactionEntity entity = transactionEntityRepository
                .findByTxnId(txnId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found with txnId: " + txnId));
        return toDTO(entity);
    }

    // ----------------------------------------------------------------
    //  Mapper
    // ----------------------------------------------------------------

    private TransactionResponseDTO toDTO(TransactionEntity entity) {
        return TransactionResponseDTO.builder()
                .id(entity.getId())
                .txnId(entity.getTxnId())
                .accountId(entity.getAccountId())
                .customerFullName(entity.getCustomerFullName())
                .customerEmail(entity.getCustomerEmail())
                .customerPhone(entity.getCustomerPhone())
                .payeeId(entity.getPayeeId())
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .txnType(entity.getTxnType())
                .txnTimestamp(entity.getTxnTimestamp())
                .monitorState(entity.getMonitorState())
                .holdStartedAt(entity.getHoldStartedAt())
                .holdExpiresAt(entity.getHoldExpiresAt())
                .finalDecision(entity.getFinalDecision())
                .decisionReason(entity.getDecisionReason())
                .decidedAt(entity.getDecidedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

