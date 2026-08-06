package com.FraudBusters.TransactionMonitoring.Controllers;

import com.FraudBusters.TransactionMonitoring.controllers.AlertReadController;
import com.FraudBusters.TransactionMonitoring.exceptions.GlobalApiExceptionHandler;
import com.FraudBusters.TransactionMonitoring.exceptions.ResourceNotFoundException;
import com.FraudBusters.TransactionMonitoring.models.dto.AlertHistoryListItemDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.AlertHistoryListResponseDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.AlertListItemDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.AlertListResponseDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.AlertTimelineItemDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.AlertTimelineResponseDTO;
import com.FraudBusters.TransactionMonitoring.models.enums.AlertStatus;
import com.FraudBusters.TransactionMonitoring.models.enums.SeverityLevel;
import com.FraudBusters.TransactionMonitoring.services.AlertReadService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link AlertReadController}.
 *
 * Uses @WebMvcTest to load only the web layer (controller + validation + exception advice).
 * AlertReadService is mocked via @MockitoBean.
 * All tests follow Given / When / Then sections.
 */
@WebMvcTest(AlertReadController.class)
@Import(GlobalApiExceptionHandler.class)
class AlertReadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AlertReadService alertReadService;

    private AlertListItemDTO buildAlertListItem(String alertCode, SeverityLevel severity, AlertStatus status) {
        return AlertListItemDTO.builder()
                .alertCode(alertCode)
                .severity(severity)
                .severityClass("sev-" + severity.name().toLowerCase())
                .ruleName("Amount Threshold Rule")
                .status(status)
                .statusClass("st-open")
                .accountId("ACC-1001")
                .amount(new BigDecimal("14000.50"))
                .payeeId("PAYEE-22")
                .createdAt(LocalDateTime.of(2026, 8, 6, 10, 15))
                .customerFullName("Jane Doe")
                .customerEmail("jane@example.com")
                .customerPhone("5550101")
                .relatedTxnId("TXN-1001")
                .relatedTxnAmount(new BigDecimal("14000.50"))
                .relatedTxnTime(LocalDateTime.of(2026, 8, 6, 10, 14))
                .build();
    }

    private AlertHistoryListItemDTO buildHistoryItem(String alertCode, AlertStatus finalStatus) {
        return AlertHistoryListItemDTO.builder()
                .alertCode(alertCode)
                .severity(SeverityLevel.HIGH)
                .ruleName("Velocity Check Rule")
                .openedAt(LocalDateTime.of(2026, 8, 5, 9, 0))
                .closedAt(LocalDateTime.of(2026, 8, 6, 9, 30))
                .finalStatus(finalStatus)
                .notes("Reviewed and finalized")
                .build();
    }

    private AlertTimelineItemDTO buildTimelineItem(AlertStatus oldStatus, AlertStatus newStatus, String actor) {
        return AlertTimelineItemDTO.builder()
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedBy(actor)
                .changeReason("Status updated by operator")
                .changedAt(LocalDateTime.of(2026, 8, 6, 11, 0))
                .build();
    }

    // -------------------------------------------------------------------------
    // GET /api/alerts
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/alerts - returns paged alerts list with counts")
    void getAlertsList_success_returnsPagedResponse() throws Exception {

        // Given
        AlertListResponseDTO response = AlertListResponseDTO.builder()
                .page(0)
                .size(10)
                .totalPages(2)
                .totalElements(11)
                .hasNext(true)
                .hasPrevious(false)
                .totalAlerts(30)
                .openAlerts(12)
                .acknowledgedAlerts(8)
                .investigatingAlerts(4)
                .alerts(List.of(
                        buildAlertListItem("ALT-001", SeverityLevel.HIGH, AlertStatus.OPEN),
                        buildAlertListItem("ALT-002", SeverityLevel.MEDIUM, AlertStatus.ACKNOWLEDGED)))
                .build();

        when(alertReadService.getAlertsList(0, 10, AlertStatus.OPEN)).thenReturn(response);

        // When
        mockMvc.perform(get("/api/alerts")
                        .param("page", "0")
                        .param("size", "10")
                        .param("status", "OPEN"))

                // Then
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(10)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.totalElements", is(11)))
                .andExpect(jsonPath("$.totalAlerts", is(30)))
                .andExpect(jsonPath("$.openAlerts", is(12)))
                .andExpect(jsonPath("$.alerts", hasSize(2)))
                .andExpect(jsonPath("$.alerts[0].alertCode", is("ALT-001")))
                .andExpect(jsonPath("$.alerts[0].severity", is("HIGH")))
                .andExpect(jsonPath("$.alerts[1].status", is("ACKNOWLEDGED")));

        verify(alertReadService, times(1)).getAlertsList(0, 10, AlertStatus.OPEN);
    }

    @Test
    @DisplayName("GET /api/alerts - no query params uses defaults page=0,size=10 and null status")
    void getAlertsList_noParams_usesDefaults() throws Exception {

        // Given
        AlertListResponseDTO response = AlertListResponseDTO.builder()
                .page(0)
                .size(10)
                .totalPages(0)
                .totalElements(0)
                .hasNext(false)
                .hasPrevious(false)
                .totalAlerts(0)
                .openAlerts(0)
                .acknowledgedAlerts(0)
                .investigatingAlerts(0)
                .alerts(Collections.emptyList())
                .build();

        when(alertReadService.getAlertsList(0, 10, null)).thenReturn(response);

        // When
        mockMvc.perform(get("/api/alerts"))

                // Then
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(10)))
                .andExpect(jsonPath("$.alerts", hasSize(0)));

        verify(alertReadService, times(1)).getAlertsList(eq(0), eq(10), isNull());
    }

    @Test
    @DisplayName("GET /api/alerts - invalid status param returns error and service is not called")
    void getAlertsList_invalidStatusParam_returnsError() throws Exception {

        // Given
        // Invalid enum value for AlertStatus.

        // When
        mockMvc.perform(get("/api/alerts").param("status", "NOT_A_STATUS"))

                // Then
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.message", is("Unexpected server error.")))
                .andExpect(jsonPath("$.path", is("/api/alerts")));

        verifyNoInteractions(alertReadService);
    }

    // -------------------------------------------------------------------------
    // GET /api/alerts/history
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/alerts/history - returns terminal alert history list")
    void getTerminalAlertHistory_success_returnsHistoryResponse() throws Exception {

        // Given
        AlertHistoryListResponseDTO response = AlertHistoryListResponseDTO.builder()
                .total(2)
                .history(List.of(
                        buildHistoryItem("ALT-HIS-001", AlertStatus.CLOSED),
                        buildHistoryItem("ALT-HIS-002", AlertStatus.DISMISSED)))
                .build();

        when(alertReadService.getTerminalAlertHistory()).thenReturn(response);

        // When
        mockMvc.perform(get("/api/alerts/history"))

                // Then
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.total", is(2)))
                .andExpect(jsonPath("$.history", hasSize(2)))
                .andExpect(jsonPath("$.history[0].alertCode", is("ALT-HIS-001")))
                .andExpect(jsonPath("$.history[1].finalStatus", is("DISMISSED")));

        verify(alertReadService, times(1)).getTerminalAlertHistory();
    }

    @Test
    @DisplayName("GET /api/alerts/history - returns empty history list when no terminal alerts exist")
    void getTerminalAlertHistory_empty_returnsEmptyList() throws Exception {

        // Given
        AlertHistoryListResponseDTO response = AlertHistoryListResponseDTO.builder()
                .total(0)
                .history(Collections.emptyList())
                .build();

        when(alertReadService.getTerminalAlertHistory()).thenReturn(response);

        // When
        mockMvc.perform(get("/api/alerts/history"))

                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(0)))
                .andExpect(jsonPath("$.history", hasSize(0)));

        verify(alertReadService, times(1)).getTerminalAlertHistory();
    }

    // -------------------------------------------------------------------------
    // GET /api/alerts/{alertCode}/history
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/alerts/{alertCode}/history - returns timeline for existing alert")
    void getAlertStatusHistory_success_returnsTimeline() throws Exception {

        // Given
        AlertTimelineResponseDTO response = AlertTimelineResponseDTO.builder()
                .alertCode("ALT-TL-001")
                .totalTransitions(2)
                .statusHistory(List.of(
                        buildTimelineItem(null, AlertStatus.OPEN, "SYSTEM"),
                        buildTimelineItem(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED, "operator-1")))
                .build();

        when(alertReadService.getAlertTimeline("ALT-TL-001")).thenReturn(response);

        // When
        mockMvc.perform(get("/api/alerts/ALT-TL-001/history"))

                // Then
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.alertCode", is("ALT-TL-001")))
                .andExpect(jsonPath("$.totalTransitions", is(2)))
                .andExpect(jsonPath("$.statusHistory", hasSize(2)))
                .andExpect(jsonPath("$.statusHistory[0].newStatus", is("OPEN")))
                .andExpect(jsonPath("$.statusHistory[1].oldStatus", is("OPEN")))
                .andExpect(jsonPath("$.statusHistory[1].newStatus", is("ACKNOWLEDGED")));

        verify(alertReadService, times(1)).getAlertTimeline("ALT-TL-001");
    }

    @Test
    @DisplayName("GET /api/alerts/{alertCode}/history - returns empty timeline when no transitions exist")
    void getAlertStatusHistory_emptyTimeline_returnsEmptyList() throws Exception {

        // Given
        AlertTimelineResponseDTO response = AlertTimelineResponseDTO.builder()
                .alertCode("ALT-TL-EMPTY")
                .totalTransitions(0)
                .statusHistory(Collections.emptyList())
                .build();

        when(alertReadService.getAlertTimeline("ALT-TL-EMPTY")).thenReturn(response);

        // When
        mockMvc.perform(get("/api/alerts/ALT-TL-EMPTY/history"))

                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertCode", is("ALT-TL-EMPTY")))
                .andExpect(jsonPath("$.totalTransitions", is(0)))
                .andExpect(jsonPath("$.statusHistory", hasSize(0)));

        verify(alertReadService, times(1)).getAlertTimeline("ALT-TL-EMPTY");
    }

    @Test
    @DisplayName("GET /api/alerts/{alertCode}/history - returns 404 when alert is not found")
    void getAlertStatusHistory_notFound_returns404() throws Exception {

        // Given
        when(alertReadService.getAlertTimeline("ALT-404"))
                .thenThrow(new ResourceNotFoundException("Alert not found with alertCode: ALT-404"));

        // When
        mockMvc.perform(get("/api/alerts/ALT-404/history"))

                // Then
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is("Alert not found with alertCode: ALT-404")))
                .andExpect(jsonPath("$.path", is("/api/alerts/ALT-404/history")));

        verify(alertReadService, times(1)).getAlertTimeline("ALT-404");
    }

    @Test
    @DisplayName("GET /api/alerts/history - unexpected service exception returns 500")
    void getTerminalAlertHistory_serviceThrowsUnexpectedException_returns500() throws Exception {

        // Given
        when(alertReadService.getTerminalAlertHistory()).thenThrow(new RuntimeException("Unexpected DB outage"));

        // When
        mockMvc.perform(get("/api/alerts/history"))

                // Then
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.error", is("Internal Server Error")))
                .andExpect(jsonPath("$.message", is("Unexpected server error.")))
                .andExpect(jsonPath("$.path", is("/api/alerts/history")));

        verify(alertReadService, times(1)).getTerminalAlertHistory();
    }

    @Test
    @DisplayName("GET /api/alerts/{alertCode}/history - unexpected service exception returns 500")
    void getAlertStatusHistory_serviceThrowsUnexpectedException_returns500() throws Exception {

        // Given
        when(alertReadService.getAlertTimeline("ALT-ERR")).thenThrow(new RuntimeException("Unexpected mapper exception"));

        // When
        mockMvc.perform(get("/api/alerts/ALT-ERR/history"))

                // Then
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.error", is("Internal Server Error")))
                .andExpect(jsonPath("$.message", is("Unexpected server error.")))
                .andExpect(jsonPath("$.path", is("/api/alerts/ALT-ERR/history")));

        verify(alertReadService, times(1)).getAlertTimeline("ALT-ERR");
    }

    @Test
    @DisplayName("GET /api/alerts - unexpected service exception returns 500")
    void getAlertsList_serviceThrowsUnexpectedException_returns500() throws Exception {

        // Given
        when(alertReadService.getAlertsList(0, 10, AlertStatus.OPEN)).thenThrow(new RuntimeException("Unexpected query failure"));

        // When
        mockMvc.perform(get("/api/alerts")
                        .param("page", "0")
                        .param("size", "10")
                        .param("status", "OPEN"))

                // Then
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.message", is("Unexpected server error.")))
                .andExpect(jsonPath("$.path", is("/api/alerts")))
                .andExpect(jsonPath("$.error", containsString("Internal Server Error")));

        verify(alertReadService, times(1)).getAlertsList(0, 10, AlertStatus.OPEN);
    }
}

