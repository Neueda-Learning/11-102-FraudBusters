package com.FraudBusters.TransactionMonitoring.Controllers;

import com.FraudBusters.TransactionMonitoring.controllers.AlertLifecycleController;
import com.FraudBusters.TransactionMonitoring.exceptions.GlobalApiExceptionHandler;
import com.FraudBusters.TransactionMonitoring.exceptions.InvalidLifecycleActionException;
import com.FraudBusters.TransactionMonitoring.exceptions.ResourceNotFoundException;
import com.FraudBusters.TransactionMonitoring.services.AlertLifecycleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link AlertLifecycleController}.
 *
 * Uses @WebMvcTest to load only the MVC layer (controller + validation + exception advice).
 * AlertLifecycleService is mocked via @MockitoBean.
 * All tests follow Given / When / Then style for readability.
 */
@WebMvcTest(AlertLifecycleController.class)
@Import(GlobalApiExceptionHandler.class)
class AlertLifecycleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AlertLifecycleService alertLifecycleService;

    private static final String BASE_URL = "/api/alerts";

    private static final String VALID_BODY = """
            {
              "reason": "Confirmed by analyst review",
              "decidedBy": "operator-42"
            }
            """;

    // -------------------------------------------------------------------------
    // POST /api/alerts/{alertCode}/acknowledge
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/alerts/{alertCode}/acknowledge - uses trimmed request values when provided")
    void acknowledgeAlert_withProvidedValues_usesTrimmedValues() throws Exception {

        // Given
        String bodyWithSpaces = """
                {
                  "reason": "   Manual acknowledgement after phone verification   ",
                  "decidedBy": "   operator-9   "
                }
                """;

        // When
        mockMvc.perform(post(BASE_URL + "/ALT-1001/acknowledge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithSpaces))

                // Then
                .andExpect(status().isOk())
                .andExpect(content().string("Alert acknowledged successfully."));

        verify(alertLifecycleService, times(1))
                .acknowledgeAlert("ALT-1001", "Manual acknowledgement after phone verification", "operator-9");
    }

    @Test
    @DisplayName("POST /api/alerts/{alertCode}/acknowledge - missing body falls back to default reason and actor")
    void acknowledgeAlert_missingBody_usesDefaults() throws Exception {

        // Given
        // Request body is omitted intentionally.

        // When
        mockMvc.perform(post(BASE_URL + "/ALT-1002/acknowledge"))

                // Then
                .andExpect(status().isOk())
                .andExpect(content().string("Alert acknowledged successfully."));

        verify(alertLifecycleService, times(1))
                .acknowledgeAlert("ALT-1002", "Operator acknowledged the alert.", "operator-1");
    }

    @Test
    @DisplayName("POST /api/alerts/{alertCode}/acknowledge - validation failure on reason length returns 400")
    void acknowledgeAlert_reasonTooLong_returns400() throws Exception {

        // Given
        String tooLongReason = "R".repeat(501);
        String invalidBody = String.format("""
                {
                  "reason": "%s",
                  "decidedBy": "operator-2"
                }
                """, tooLongReason);

        // When
        mockMvc.perform(post(BASE_URL + "/ALT-1003/acknowledge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))

                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("reason")))
                .andExpect(jsonPath("$.path", is("/api/alerts/ALT-1003/acknowledge")));

        verifyNoInteractions(alertLifecycleService);
    }

    @Test
    @DisplayName("POST /api/alerts/{alertCode}/acknowledge - not found from service maps to 404")
    void acknowledgeAlert_serviceNotFound_returns404() throws Exception {

        // Given
        doThrow(new ResourceNotFoundException("Alert not found: ALT-404"))
                .when(alertLifecycleService)
                .acknowledgeAlert(anyString(), anyString(), anyString());

        // When
        mockMvc.perform(post(BASE_URL + "/ALT-404/acknowledge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))

                // Then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is("Alert not found: ALT-404")))
                .andExpect(jsonPath("$.path", is("/api/alerts/ALT-404/acknowledge")));

        verify(alertLifecycleService, times(1))
                .acknowledgeAlert("ALT-404", "Confirmed by analyst review", "operator-42");
    }

    // -------------------------------------------------------------------------
    // POST /api/alerts/{alertCode}/investigate
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/alerts/{alertCode}/investigate - blank fields fall back to endpoint defaults")
    void investigateAlert_blankFields_usesDefaults() throws Exception {

        // Given
        String blankBody = """
                {
                  "reason": "   ",
                  "decidedBy": "   "
                }
                """;

        // When
        mockMvc.perform(post(BASE_URL + "/ALT-2001/investigate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blankBody))

                // Then
                .andExpect(status().isOk())
                .andExpect(content().string("Alert moved to investigating successfully."));

        verify(alertLifecycleService, times(1))
                .investigateAlert("ALT-2001", "Operator started investigation.", "operator-1");
    }

    @Test
    @DisplayName("POST /api/alerts/{alertCode}/investigate - decidedBy longer than 100 fails validation")
    void investigateAlert_decidedByTooLong_returns400() throws Exception {

        // Given
        String tooLongDecidedBy = "O".repeat(101);
        String invalidBody = String.format("""
                {
                  "reason": "Investigating transaction pattern",
                  "decidedBy": "%s"
                }
                """, tooLongDecidedBy);

        // When
        mockMvc.perform(post(BASE_URL + "/ALT-2002/investigate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))

                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("decidedBy")))
                .andExpect(jsonPath("$.path", is("/api/alerts/ALT-2002/investigate")));

        verifyNoInteractions(alertLifecycleService);
    }

    // -------------------------------------------------------------------------
    // POST /api/alerts/{alertCode}/close
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/alerts/{alertCode}/close - valid reason and blank decidedBy closes with default actor")
    void closeAlert_validReason_blankActor_usesDefaultActor() throws Exception {

        // Given
        String body = """
                {
                  "reason": "Fraud confirmed after review",
                  "decidedBy": "   "
                }
                """;

        // When
        mockMvc.perform(post(BASE_URL + "/ALT-3001/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))

                // Then
                .andExpect(status().isOk())
                .andExpect(content().string("Alert closed successfully."));

        verify(alertLifecycleService, times(1))
                .closeAlert("ALT-3001", "Fraud confirmed after review", "operator-1");
    }

    @Test
    @DisplayName("POST /api/alerts/{alertCode}/close - missing request body returns 400 because reason is mandatory")
    void closeAlert_missingBody_returns400() throws Exception {

        // Given
        // No body so required reason cannot be resolved.

        // When
        mockMvc.perform(post(BASE_URL + "/ALT-3002/close"))

                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Reason is required for close action.")))
                .andExpect(jsonPath("$.path", is("/api/alerts/ALT-3002/close")));

        verifyNoInteractions(alertLifecycleService);
    }

    @Test
    @DisplayName("POST /api/alerts/{alertCode}/close - blank reason returns 400 because reason is mandatory")
    void closeAlert_blankReason_returns400() throws Exception {

        // Given
        String bodyWithBlankReason = """
                {
                  "reason": "   ",
                  "decidedBy": "operator-77"
                }
                """;

        // When
        mockMvc.perform(post(BASE_URL + "/ALT-3003/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithBlankReason))

                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Reason is required for close action.")));

        verify(alertLifecycleService, never()).closeAlert(anyString(), anyString(), anyString());
    }

    // -------------------------------------------------------------------------
    // POST /api/alerts/{alertCode}/dismiss
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/alerts/{alertCode}/dismiss - valid request dismisses alert successfully")
    void dismissAlert_validRequest_returnsSuccess() throws Exception {

        // Given
        String body = """
                {
                  "reason": "Customer confirmed transaction is legitimate",
                  "decidedBy": "operator-18"
                }
                """;

        // When
        mockMvc.perform(post(BASE_URL + "/ALT-4001/dismiss")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))

                // Then
                .andExpect(status().isOk())
                .andExpect(content().string("Alert dismissed successfully."));

        verify(alertLifecycleService, times(1))
                .dismissAlert("ALT-4001", "Customer confirmed transaction is legitimate", "operator-18");
    }

    @Test
    @DisplayName("POST /api/alerts/{alertCode}/dismiss - missing reason returns 400")
    void dismissAlert_missingReason_returns400() throws Exception {

        // Given
        String bodyWithoutReason = """
                {
                  "decidedBy": "operator-21"
                }
                """;

        // When
        mockMvc.perform(post(BASE_URL + "/ALT-4002/dismiss")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithoutReason))

                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Reason is required for dismiss action.")))
                .andExpect(jsonPath("$.path", is("/api/alerts/ALT-4002/dismiss")));

        verify(alertLifecycleService, never()).dismissAlert(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("POST /api/alerts/{alertCode}/dismiss - service invalid lifecycle error maps to 400")
    void dismissAlert_serviceInvalidAction_returns400() throws Exception {

        // Given
        doThrow(new InvalidLifecycleActionException("Alert cannot be dismissed from CLOSED state"))
                .when(alertLifecycleService)
                .dismissAlert(anyString(), anyString(), anyString());

        // When
        mockMvc.perform(post(BASE_URL + "/ALT-4003/dismiss")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))

                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Alert cannot be dismissed from CLOSED state")))
                .andExpect(jsonPath("$.path", is("/api/alerts/ALT-4003/dismiss")));

        verify(alertLifecycleService, times(1))
                .dismissAlert("ALT-4003", "Confirmed by analyst review", "operator-42");
    }

    @Test
    @DisplayName("POST /api/alerts/{alertCode}/dismiss - unexpected service exception maps to 500")
    void dismissAlert_serviceThrowsUnexpectedException_returns500() throws Exception {

        // Given
        doThrow(new RuntimeException("Unexpected persistence error"))
                .when(alertLifecycleService)
                .dismissAlert(anyString(), anyString(), anyString());

        // When
        mockMvc.perform(post(BASE_URL + "/ALT-4004/dismiss")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))

                // Then
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.error", is("Internal Server Error")))
                .andExpect(jsonPath("$.message", is("Unexpected server error.")))
                .andExpect(jsonPath("$.path", is("/api/alerts/ALT-4004/dismiss")));

        verify(alertLifecycleService, times(1))
                .dismissAlert("ALT-4004", "Confirmed by analyst review", "operator-42");
    }

    @Test
    @DisplayName("POST /api/alerts/{alertCode}/close - malformed JSON returns 400 and service is not called")
    void closeAlert_malformedJson_returns400() throws Exception {

        // Given
        String malformedJson = "{\"reason\":\"Fraud\",\"decidedBy\":";

        // When
        mockMvc.perform(post(BASE_URL + "/ALT-3004/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))

                // Then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Malformed request body.")))
                .andExpect(jsonPath("$.path", is("/api/alerts/ALT-3004/close")));

        verifyNoInteractions(alertLifecycleService);
    }
}

