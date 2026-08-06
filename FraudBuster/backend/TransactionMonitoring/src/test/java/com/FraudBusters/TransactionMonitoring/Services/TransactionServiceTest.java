package com.FraudBusters.TransactionMonitoring.Services;

import com.FraudBusters.TransactionMonitoring.exceptions.ResourceNotFoundException;
import com.FraudBusters.TransactionMonitoring.models.TransactionEntity;
import com.FraudBusters.TransactionMonitoring.models.dto.TransactionResponseDTO;
import com.FraudBusters.TransactionMonitoring.models.enums.FinalDecision;
import com.FraudBusters.TransactionMonitoring.models.enums.MonitorState;
import com.FraudBusters.TransactionMonitoring.models.enums.TransactionType;
import com.FraudBusters.TransactionMonitoring.repository.TransactionEntityRepository;
import com.FraudBusters.TransactionMonitoring.services.TransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionEntityRepository transactionEntityRepository;

    @InjectMocks
    private TransactionService service;

    @Test
    void getAllTransactions_whenNoTransactionsExist_thenReturnsEmptyList() {
        // given
        when(transactionEntityRepository.findAll()).thenReturn(List.of());

        // when
        List<TransactionResponseDTO> response = service.getAllTransactions();

        // then
        assertTrue(response.isEmpty());
        verify(transactionEntityRepository).findAll();
    }

    @Test
    void getAllTransactions_whenTransactionsExist_thenMapsAllFieldsCorrectly() {
        // given
        TransactionEntity entity = fullTransactionEntity("TXN-10001");
        when(transactionEntityRepository.findAll()).thenReturn(List.of(entity));

        // when
        List<TransactionResponseDTO> response = service.getAllTransactions();

        // then
        assertEquals(1, response.size());
        assertMappedTransaction(entity, response.get(0));
        verify(transactionEntityRepository).findAll();
    }

    @Test
    void getTransactionByTxnId_whenTransactionExists_thenReturnsMappedDto() {
        // given
        TransactionEntity entity = fullTransactionEntity("TXN-10002");
        when(transactionEntityRepository.findByTxnId("TXN-10002")).thenReturn(Optional.of(entity));

        // when
        TransactionResponseDTO response = service.getTransactionByTxnId("TXN-10002");

        // then
        assertMappedTransaction(entity, response);
        verify(transactionEntityRepository).findByTxnId("TXN-10002");
    }

    @Test
    void getTransactionByTxnId_whenTransactionMissing_thenThrowsResourceNotFoundException() {
        // given
        when(transactionEntityRepository.findByTxnId("MISSING-TXN")).thenReturn(Optional.empty());

        // when
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.getTransactionByTxnId("MISSING-TXN")
        );

        // then
        assertEquals("Transaction not found with txnId: MISSING-TXN", exception.getMessage());
        verify(transactionEntityRepository).findByTxnId("MISSING-TXN");
    }

    @Test
    void getTransactionByTxnId_whenTxnIdIsNull_thenThrowsResourceNotFoundExceptionWithNullInMessage() {
        // given
        when(transactionEntityRepository.findByTxnId(null)).thenReturn(Optional.empty());

        // when
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.getTransactionByTxnId(null)
        );

        // then
        assertEquals("Transaction not found with txnId: null", exception.getMessage());
        verify(transactionEntityRepository).findByTxnId(null);
    }

    private TransactionEntity fullTransactionEntity(String txnId) {
        TransactionEntity entity = new TransactionEntity();
        entity.setId(99L);
        entity.setTxnId(txnId);
        entity.setAccountId("ACC-123");
        entity.setCustomerFullName("John Doe");
        entity.setCustomerEmail("john@example.com");
        entity.setCustomerPhone("1234567890");
        entity.setPayeeId("PAY-001");
        entity.setAmount(new BigDecimal("1500.75"));
        entity.setCurrency("USD");
        entity.setTxnType(TransactionType.DEBIT);
        entity.setTxnTimestamp(LocalDateTime.of(2026, 8, 6, 10, 0));
        entity.setMonitorState(MonitorState.HELD);
        entity.setHoldStartedAt(LocalDateTime.of(2026, 8, 6, 10, 1));
        entity.setHoldExpiresAt(LocalDateTime.of(2026, 8, 6, 12, 0));
        entity.setFinalDecision(FinalDecision.PENDING);
        entity.setDecisionReason("Under review");
        entity.setDecidedAt(LocalDateTime.of(2026, 8, 6, 10, 2));
        entity.setCreatedAt(LocalDateTime.of(2026, 8, 6, 9, 59));
        entity.setUpdatedAt(LocalDateTime.of(2026, 8, 6, 10, 3));
        return entity;
    }

    private void assertMappedTransaction(TransactionEntity entity, TransactionResponseDTO dto) {
        assertEquals(entity.getId(), dto.getId());
        assertEquals(entity.getTxnId(), dto.getTxnId());
        assertEquals(entity.getAccountId(), dto.getAccountId());
        assertEquals(entity.getCustomerFullName(), dto.getCustomerFullName());
        assertEquals(entity.getCustomerEmail(), dto.getCustomerEmail());
        assertEquals(entity.getCustomerPhone(), dto.getCustomerPhone());
        assertEquals(entity.getPayeeId(), dto.getPayeeId());
        assertEquals(entity.getAmount(), dto.getAmount());
        assertEquals(entity.getCurrency(), dto.getCurrency());
        assertEquals(entity.getTxnType(), dto.getTxnType());
        assertEquals(entity.getTxnTimestamp(), dto.getTxnTimestamp());
        assertEquals(entity.getMonitorState(), dto.getMonitorState());
        assertEquals(entity.getHoldStartedAt(), dto.getHoldStartedAt());
        assertEquals(entity.getHoldExpiresAt(), dto.getHoldExpiresAt());
        assertEquals(entity.getFinalDecision(), dto.getFinalDecision());
        assertEquals(entity.getDecisionReason(), dto.getDecisionReason());
        assertEquals(entity.getDecidedAt(), dto.getDecidedAt());
        assertEquals(entity.getCreatedAt(), dto.getCreatedAt());
        assertEquals(entity.getUpdatedAt(), dto.getUpdatedAt());
    }
}

