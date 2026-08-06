package com.FraudBusters.TransactionMonitoring.Controllers;

import com.FraudBusters.TransactionMonitoring.controllers.RuleEngineServiceController;
import com.FraudBusters.TransactionMonitoring.exceptions.GlobalApiExceptionHandler;
import com.FraudBusters.TransactionMonitoring.models.dto.CentralRuleEvaluationResponseDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.TransactionRequestDTO;
import com.FraudBusters.TransactionMonitoring.services.Impl.AmountThresholdRuleEngineServiceImpl;
import com.FraudBusters.TransactionMonitoring.services.Impl.CentralRuleEngineServiceImpl;
import com.FraudBusters.TransactionMonitoring.services.Impl.DailyLimitRuleEngineServiceImpl;
import com.FraudBusters.TransactionMonitoring.services.Impl.NewPayeeRuleEngineImpl;
import com.FraudBusters.TransactionMonitoring.services.Impl.VelocityCheckRuleEngineImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link RuleEngineServiceController}.
 *
 * Uses @WebMvcTest to load only the MVC layer and mocks all rule engine services.
 * Tests are written in Given / When / Then format and focus on both happy paths and edge cases.
 */
@WebMvcTest(RuleEngineServiceController.class)
@Import(GlobalApiExceptionHandler.class)
class RuleEngineServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AmountThresholdRuleEngineServiceImpl amountThresholdRuleEngineService;

    @MockitoBean
    private DailyLimitRuleEngineServiceImpl dailyLimitRuleEngineService;

    @MockitoBean
    private NewPayeeRuleEngineImpl newPayeeRuleEngine;

    @MockitoBean
    private VelocityCheckRuleEngineImpl velocityCheckRuleEngine;

    @MockitoBean
    private CentralRuleEngineServiceImpl centralRuleEngineService;

    private static final String BASE_URL = "/api/rule-engine/evaluate";

    // Valid JSON body for TransactionRequestDTO
    private static final String VALID_REQUEST_BODY = """
            {
              "txnId": "TXN-1001",
              "accountId": "ACC-123",
              "customerFullName": "Jane Doe",
              "customerEmail": "jane@example.com",
              "customerPhone": "5550101",
              "payeeId": "PAYEE-22",
              "amount": 1500.75,
              "currency": "USD",
              "txnType": "DEBIT",
              "txnTimestamp": "2026-08-01T10:15:30"
            }
            """;

    @Test
    @DisplayName("POST /evaluate/amount-threshold - returns true when service evaluates successfully")
    void evaluateAmountThreshold_success_returnsTrue() throws Exception {

        // Given
        when(amountThresholdRuleEngineService.evaluateTransaction(any(TransactionRequestDTO.class)))
                .thenReturn(Optional.of(Boolean.TRUE));

        // When
        mockMvc.perform(post(BASE_URL + "/amount-threshold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))

                // Then
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string("true"));

        verify(amountThresholdRuleEngineService, times(1)).evaluateTransaction(any(TransactionRequestDTO.class));
    }

    @Test
    @DisplayName("POST /evaluate/amount-threshold - returns 404 when service returns Optional.empty")
    void evaluateAmountThreshold_emptyOptional_returns404() throws Exception {

        // Given
        when(amountThresholdRuleEngineService.evaluateTransaction(any(TransactionRequestDTO.class)))
                .thenReturn(Optional.empty());

        // When
        mockMvc.perform(post(BASE_URL + "/amount-threshold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))

                // Then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Amount threshold evaluation could not be completed")))
                .andExpect(jsonPath("$.path", is("/api/rule-engine/evaluate/amount-threshold")));

        verify(amountThresholdRuleEngineService, times(1)).evaluateTransaction(any(TransactionRequestDTO.class));
    }

    @Test
    @DisplayName("POST /evaluate/daily-limit - returns false when service evaluates successfully")
    void evaluateDailyLimit_success_returnsFalse() throws Exception {

        // Given
        when(dailyLimitRuleEngineService.evaluateTransaction(any(TransactionRequestDTO.class)))
                .thenReturn(Optional.of(Boolean.FALSE));

        // When
        mockMvc.perform(post(BASE_URL + "/daily-limit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))

                // Then
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        verify(dailyLimitRuleEngineService, times(1)).evaluateTransaction(any(TransactionRequestDTO.class));
    }

    @Test
    @DisplayName("POST /evaluate/daily-limit - returns 404 when service returns Optional.empty")
    void evaluateDailyLimit_emptyOptional_returns404() throws Exception {

        // Given
        when(dailyLimitRuleEngineService.evaluateTransaction(any(TransactionRequestDTO.class)))
                .thenReturn(Optional.empty());

        // When
        mockMvc.perform(post(BASE_URL + "/daily-limit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))

                // Then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Daily limit evaluation could not be completed")));

        verify(dailyLimitRuleEngineService, times(1)).evaluateTransaction(any(TransactionRequestDTO.class));
    }

    @Test
    @DisplayName("POST /evaluate/new-payee - returns true when service evaluates successfully")
    void evaluateNewPayee_success_returnsTrue() throws Exception {

        // Given
        when(newPayeeRuleEngine.evaluateTransaction(any(TransactionRequestDTO.class)))
                .thenReturn(Optional.of(Boolean.TRUE));

        // When
        mockMvc.perform(post(BASE_URL + "/new-payee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))

                // Then
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(newPayeeRuleEngine, times(1)).evaluateTransaction(any(TransactionRequestDTO.class));
    }

    @Test
    @DisplayName("POST /evaluate/new-payee - returns 404 when service returns Optional.empty")
    void evaluateNewPayee_emptyOptional_returns404() throws Exception {

        // Given
        when(newPayeeRuleEngine.evaluateTransaction(any(TransactionRequestDTO.class)))
                .thenReturn(Optional.empty());

        // When
        mockMvc.perform(post(BASE_URL + "/new-payee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))

                // Then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("New payee evaluation could not be completed")));

        verify(newPayeeRuleEngine, times(1)).evaluateTransaction(any(TransactionRequestDTO.class));
    }

    @Test
    @DisplayName("POST /evaluate/velocity-check - returns true when service evaluates successfully")
    void evaluateVelocityCheck_success_returnsTrue() throws Exception {

        // Given
        when(velocityCheckRuleEngine.evaluateTransaction(any(TransactionRequestDTO.class)))
                .thenReturn(Optional.of(Boolean.TRUE));

        // When
        mockMvc.perform(post(BASE_URL + "/velocity-check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))

                // Then
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(velocityCheckRuleEngine, times(1)).evaluateTransaction(any(TransactionRequestDTO.class));
    }

    @Test
    @DisplayName("POST /evaluate/velocity-check - returns 404 when service returns Optional.empty")
    void evaluateVelocityCheck_emptyOptional_returns404() throws Exception {

        // Given
        when(velocityCheckRuleEngine.evaluateTransaction(any(TransactionRequestDTO.class)))
                .thenReturn(Optional.empty());

        // When
        mockMvc.perform(post(BASE_URL + "/velocity-check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))

                // Then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Velocity evaluation could not be completed")));

        verify(velocityCheckRuleEngine, times(1)).evaluateTransaction(any(TransactionRequestDTO.class));
    }

    @Test
    @DisplayName("POST /evaluate/central - returns central evaluation payload when service returns non-null response")
    void evaluateCentral_success_returnsEvaluationResponse() throws Exception {

        // Given
        CentralRuleEvaluationResponseDTO response = CentralRuleEvaluationResponseDTO.builder()
                .txnId("TXN-1001")
                .anyRuleTriggered(true)
                .evaluatedRuleCount(3)
                .ruleResults(Map.of("AMOUNT_THRESHOLD", true, "DAILY_LIMIT", false))
                .skippedRuleCodes(List.of("UNKNOWN_RULE"))
                .monitorState(null)
                .finalDecision(null)
                .decisionReason("Held by central rule engine")
                .build();

        when(centralRuleEngineService.evaluateAgainstActiveRules(any(TransactionRequestDTO.class))).thenReturn(response);

        // When
        mockMvc.perform(post(BASE_URL + "/central")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))

                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.txnId", is("TXN-1001")))
                .andExpect(jsonPath("$.anyRuleTriggered", is(true)))
                .andExpect(jsonPath("$.evaluatedRuleCount", is(3)))
                .andExpect(jsonPath("$.ruleResults.AMOUNT_THRESHOLD", is(true)))
                .andExpect(jsonPath("$.ruleResults.DAILY_LIMIT", is(false)))
                .andExpect(jsonPath("$.skippedRuleCodes", hasSize(1)))
                .andExpect(jsonPath("$.decisionReason", is("Held by central rule engine")));

        verify(centralRuleEngineService, times(1)).evaluateAgainstActiveRules(any(TransactionRequestDTO.class));
    }

    @Test
    @DisplayName("POST /evaluate/central - returns 404 when service returns null")
    void evaluateCentral_nullResponse_returns404() throws Exception {

        // Given
        when(centralRuleEngineService.evaluateAgainstActiveRules(any(TransactionRequestDTO.class))).thenReturn(null);

        // When
        mockMvc.perform(post(BASE_URL + "/central")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))

                // Then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Central rule evaluation could not be completed")))
                .andExpect(jsonPath("$.path", is("/api/rule-engine/evaluate/central")));

        verify(centralRuleEngineService, times(1)).evaluateAgainstActiveRules(any(TransactionRequestDTO.class));
    }

    @Test
    @DisplayName("POST /evaluate/central/pending - returns evaluated batch list")
    void evaluatePendingCentral_success_returnsBatchList() throws Exception {

        // Given
        CentralRuleEvaluationResponseDTO first = CentralRuleEvaluationResponseDTO.builder()
                .txnId("TXN-2001")
                .anyRuleTriggered(false)
                .evaluatedRuleCount(4)
                .ruleResults(Map.of("AMOUNT_THRESHOLD", false))
                .skippedRuleCodes(Collections.emptyList())
                .decisionReason("Passed all rules")
                .build();

        CentralRuleEvaluationResponseDTO second = CentralRuleEvaluationResponseDTO.builder()
                .txnId("TXN-2002")
                .anyRuleTriggered(true)
                .evaluatedRuleCount(4)
                .ruleResults(Map.of("VELOCITY_CHECK", true))
                .skippedRuleCodes(Collections.emptyList())
                .decisionReason("Held")
                .build();

        when(centralRuleEngineService.evaluatePendingTransactionsBatch()).thenReturn(List.of(first, second));

        // When
        mockMvc.perform(post(BASE_URL + "/central/pending"))

                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].txnId", is("TXN-2001")))
                .andExpect(jsonPath("$[1].txnId", is("TXN-2002")))
                .andExpect(jsonPath("$[1].anyRuleTriggered", is(true)));

        verify(centralRuleEngineService, times(1)).evaluatePendingTransactionsBatch();
    }

    @Test
    @DisplayName("POST /evaluate/central/pending - returns empty array when no pending transactions")
    void evaluatePendingCentral_emptyBatch_returnsEmptyArray() throws Exception {

        // Given
        when(centralRuleEngineService.evaluatePendingTransactionsBatch()).thenReturn(Collections.emptyList());

        // When
        mockMvc.perform(post(BASE_URL + "/central/pending"))

                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)))
                .andExpect(content().json("[]"));

        verify(centralRuleEngineService, times(1)).evaluatePendingTransactionsBatch();
    }

    @Test
    @DisplayName("POST /evaluate/amount-threshold - invalid request body fails validation and service is not called")
    void evaluateAmountThreshold_invalidPayload_returns400() throws Exception {

        // Given
        String invalidBody = """
                {
                  "txnId": "",
                  "accountId": "ACC-123",
                  "payeeId": "PAYEE-22",
                  "amount": 1500.75,
                  "currency": "USD",
                  "txnType": "DEBIT",
                  "txnTimestamp": "2026-08-01T10:15:30"
                }
                """;
        // txnId is blank, so @NotBlank should fail before reaching the service.

        // When
        mockMvc.perform(post(BASE_URL + "/amount-threshold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))

                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Validation failed: {txnId=must not be blank}")));

        verify(amountThresholdRuleEngineService, never()).evaluateTransaction(any(TransactionRequestDTO.class));
        verifyNoInteractions(dailyLimitRuleEngineService, newPayeeRuleEngine, velocityCheckRuleEngine, centralRuleEngineService);
    }

    @Test
    @DisplayName("POST /evaluate/central - malformed JSON returns 400 and no service call")
    void evaluateCentral_malformedJson_returns400() throws Exception {

        // Given
        String malformedJson = "{\"txnId\":\"TXN-1001\",\"accountId\":";

        // When
        mockMvc.perform(post(BASE_URL + "/central")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))

                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Malformed request body.")))
                .andExpect(jsonPath("$.path", is("/api/rule-engine/evaluate/central")));

        verifyNoInteractions(centralRuleEngineService);
    }
}

