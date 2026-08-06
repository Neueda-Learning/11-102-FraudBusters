package com.FraudBusters.TransactionMonitoring.Controllers;

import com.FraudBusters.TransactionMonitoring.controllers.TransactionController;
import com.FraudBusters.TransactionMonitoring.exceptions.GlobalApiExceptionHandler;
import com.FraudBusters.TransactionMonitoring.exceptions.ResourceNotFoundException;
import com.FraudBusters.TransactionMonitoring.models.dto.TransactionResponseDTO;
import com.FraudBusters.TransactionMonitoring.models.enums.FinalDecision;
import com.FraudBusters.TransactionMonitoring.models.enums.MonitorState;
import com.FraudBusters.TransactionMonitoring.models.enums.TransactionType;
import com.FraudBusters.TransactionMonitoring.services.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link TransactionController}.
 *
 * Uses @WebMvcTest to load only the web layer (no DB, no full Spring context).
 * TransactionService is mocked via @MockitoBean.
 * All tests follow the Given / When / Then structure.
 */
@WebMvcTest(TransactionController.class)
@Import(GlobalApiExceptionHandler.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    // ─────────────────────────────────────────────────────────────────────────
    //  Helper – builds a realistic TransactionResponseDTO stub
    // ─────────────────────────────────────────────────────────────────────────

    private TransactionResponseDTO buildSampleDTO(Long id,
                                                  String txnId,
                                                  TransactionType txnType,
                                                  MonitorState monitorState,
                                                  FinalDecision finalDecision) {
        return TransactionResponseDTO.builder()
                .id(id)
                .txnId(txnId)
                .accountId("acct_" + txnId.toLowerCase())
                .customerFullName("John Doe")
                .customerEmail("john.doe@example.com")
                .customerPhone("+1-555-0100")
                .payeeId("payee_001")
                .amount(new BigDecimal("12500.00"))
                .currency("USD")
                .txnType(txnType)
                .txnTimestamp(LocalDateTime.of(2026, 8, 1, 10, 30))
                .monitorState(monitorState)
                .holdStartedAt(null)
                .holdExpiresAt(null)
                .finalDecision(finalDecision)
                .decisionReason(null)
                .decidedAt(null)
                .createdAt(LocalDateTime.of(2026, 8, 1, 10, 30))
                .updatedAt(LocalDateTime.of(2026, 8, 1, 10, 30))
                .build();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  GET /api/transactions
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET /api/transactions – returns list of all transactions with HTTP 200")
    void getAllTransactions_returnsList() throws Exception {

        // ── Given ─────────────────────────────────────────────────────────────
        TransactionResponseDTO dto1 = buildSampleDTO(1L, "TXN-AMT-001",
                TransactionType.DEBIT, MonitorState.RECEIVED, FinalDecision.PENDING);
        TransactionResponseDTO dto2 = buildSampleDTO(2L, "TXN-AMT-002",
                TransactionType.CREDIT, MonitorState.HELD, FinalDecision.ALLOW);

        when(transactionService.getAllTransactions()).thenReturn(List.of(dto1, dto2));

        // ── When ──────────────────────────────────────────────────────────────
        mockMvc.perform(get("/api/transactions"))

        // ── Then ──────────────────────────────────────────────────────────────
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$", hasSize(2)))
                // First transaction assertions
                .andExpect(jsonPath("$[0].txnId",        is("TXN-AMT-001")))
                .andExpect(jsonPath("$[0].txnType",      is("DEBIT")))
                .andExpect(jsonPath("$[0].monitorState", is("RECEIVED")))
                .andExpect(jsonPath("$[0].finalDecision",is("PENDING")))
                .andExpect(jsonPath("$[0].currency",     is("USD")))
                .andExpect(jsonPath("$[0].amount",       is(12500.00)))
                // Second transaction assertions
                .andExpect(jsonPath("$[1].txnId",        is("TXN-AMT-002")))
                .andExpect(jsonPath("$[1].txnType",      is("CREDIT")))
                .andExpect(jsonPath("$[1].monitorState", is("HELD")))
                .andExpect(jsonPath("$[1].finalDecision",is("ALLOW")));

        verify(transactionService, times(1)).getAllTransactions();
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/transactions – returns empty JSON array when no transactions exist")
    void getAllTransactions_emptyList_returnsEmptyArray() throws Exception {

        // ── Given ─────────────────────────────────────────────────────────────
        when(transactionService.getAllTransactions()).thenReturn(Collections.emptyList());

        // ── When ──────────────────────────────────────────────────────────────
        mockMvc.perform(get("/api/transactions"))

        // ── Then ──────────────────────────────────────────────────────────────
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$", hasSize(0)))
                .andExpect(content().json("[]"));

        verify(transactionService, times(1)).getAllTransactions();
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/transactions – returns 500 on unexpected service exception")
    void getAllTransactions_serviceThrowsUnexpectedException_returns500() throws Exception {

        // ── Given ─────────────────────────────────────────────────────────────
        when(transactionService.getAllTransactions())
                .thenThrow(new RuntimeException("DB crashed"));

        // ── When ──────────────────────────────────────────────────────────────
        mockMvc.perform(get("/api/transactions"))

        // ── Then ──────────────────────────────────────────────────────────────
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status",  is(500)))
                .andExpect(jsonPath("$.error",   is("Internal Server Error")))
                .andExpect(jsonPath("$.message", is("Unexpected server error.")))
                .andExpect(jsonPath("$.path",    is("/api/transactions")));

        verify(transactionService, times(1)).getAllTransactions();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  GET /api/transactions/{txnId}
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET /api/transactions/{txnId} – returns single transaction when found")
    void getTransactionByTxnId_found_returnsDto() throws Exception {

        // ── Given ─────────────────────────────────────────────────────────────
        TransactionResponseDTO dto = buildSampleDTO(1L, "TXN-AMT-001",
                TransactionType.DEBIT, MonitorState.RECEIVED, FinalDecision.PENDING);

        when(transactionService.getTransactionByTxnId("TXN-AMT-001")).thenReturn(dto);

        // ── When ──────────────────────────────────────────────────────────────
        mockMvc.perform(get("/api/transactions/TXN-AMT-001"))

        // ── Then ──────────────────────────────────────────────────────────────
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.id",           is(1)))
                .andExpect(jsonPath("$.txnId",        is("TXN-AMT-001")))
                .andExpect(jsonPath("$.txnType",      is("DEBIT")))
                .andExpect(jsonPath("$.currency",     is("USD")))
                .andExpect(jsonPath("$.amount",       is(12500.00)))
                .andExpect(jsonPath("$.monitorState", is("RECEIVED")))
                .andExpect(jsonPath("$.finalDecision",is("PENDING")))
                .andExpect(jsonPath("$.customerFullName", is("John Doe")))
                .andExpect(jsonPath("$.customerEmail",    is("john.doe@example.com")));

        verify(transactionService, times(1)).getTransactionByTxnId("TXN-AMT-001");
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/transactions/{txnId} – returns 404 when txnId does not exist")
    void getTransactionByTxnId_notFound_returns404WithApiError() throws Exception {

        // ── Given ─────────────────────────────────────────────────────────────
        String missingTxnId = "UNKNOWN-TXN";
        when(transactionService.getTransactionByTxnId(missingTxnId))
                .thenThrow(new ResourceNotFoundException(
                        "Transaction not found with txnId: " + missingTxnId));

        // ── When ──────────────────────────────────────────────────────────────
        mockMvc.perform(get("/api/transactions/{txnId}", missingTxnId))

        // ── Then ──────────────────────────────────────────────────────────────
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status",  is(404)))
                .andExpect(jsonPath("$.error",   is("Not Found")))
                .andExpect(jsonPath("$.message", containsString("UNKNOWN-TXN")))
                .andExpect(jsonPath("$.path",    is("/api/transactions/" + missingTxnId)));

        verify(transactionService, times(1)).getTransactionByTxnId(missingTxnId);
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/transactions/{txnId} – returns 500 on unexpected service exception")
    void getTransactionByTxnId_serviceThrowsUnexpectedException_returns500() throws Exception {

        // ── Given ─────────────────────────────────────────────────────────────
        when(transactionService.getTransactionByTxnId("TXN-ERR-999"))
                .thenThrow(new RuntimeException("Unexpected mapping failure"));

        // ── When ──────────────────────────────────────────────────────────────
        mockMvc.perform(get("/api/transactions/TXN-ERR-999"))

        // ── Then ──────────────────────────────────────────────────────────────
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status",  is(500)))
                .andExpect(jsonPath("$.error",   is("Internal Server Error")))
                .andExpect(jsonPath("$.message", is("Unexpected server error.")))
                .andExpect(jsonPath("$.path",    is("/api/transactions/TXN-ERR-999")));

        verify(transactionService, times(1)).getTransactionByTxnId("TXN-ERR-999");
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/transactions/{txnId} – txnId with special characters is passed correctly to service")
    void getTransactionByTxnId_specialCharactersInTxnId_passedCorrectlyToService() throws Exception {

        // ── Given ─────────────────────────────────────────────────────────────
        String txnId = "TXN-SPEC-2026";
        TransactionResponseDTO dto = buildSampleDTO(99L, txnId,
                TransactionType.CREDIT, MonitorState.DECLINED, FinalDecision.DECLINE);

        when(transactionService.getTransactionByTxnId(txnId)).thenReturn(dto);

        // ── When ──────────────────────────────────────────────────────────────
        mockMvc.perform(get("/api/transactions/{txnId}", txnId))

        // ── Then ──────────────────────────────────────────────────────────────
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.txnId",        is(txnId)))
                .andExpect(jsonPath("$.txnType",      is("CREDIT")))
                .andExpect(jsonPath("$.monitorState", is("DECLINED")))
                .andExpect(jsonPath("$.finalDecision",is("DECLINE")));

        verify(transactionService, times(1)).getTransactionByTxnId(txnId);
    }
}

