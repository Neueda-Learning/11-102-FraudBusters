package com.FraudBusters.TransactionMonitoring.Controllers;

import com.FraudBusters.TransactionMonitoring.controllers.DashboardController;
import com.FraudBusters.TransactionMonitoring.exceptions.GlobalApiExceptionHandler;
import com.FraudBusters.TransactionMonitoring.models.dto.DashboardRecentAlertItemDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.DashboardSummaryResponseDTO;
import com.FraudBusters.TransactionMonitoring.models.enums.AlertStatus;
import com.FraudBusters.TransactionMonitoring.models.enums.SeverityLevel;
import com.FraudBusters.TransactionMonitoring.services.DashboardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link DashboardController}.
 *
 * Uses @WebMvcTest to load only the web layer.
 * DashboardService is mocked with @MockitoBean.
 * All tests follow Given / When / Then sections.
 */
@WebMvcTest(DashboardController.class)
@Import(GlobalApiExceptionHandler.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    private static final String BASE_URL = "/api/dashboard";

    // -------------------------------------------------------------------------
    // GET /api/dashboard/summary
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/dashboard/summary - returns summary cards payload")
    void getSummary_success_returnsSummaryPayload() throws Exception {

        // Given
        DashboardSummaryResponseDTO summary = DashboardSummaryResponseDTO.builder()
                .openAlerts(12L)
                .acknowledgedAlerts(3L)
                .transactionsToday(148L)
                .closedToday(7L)
                .build();

        when(dashboardService.getDashboardSummary()).thenReturn(summary);

        // When
        mockMvc.perform(get(BASE_URL + "/summary"))

                // Then
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.openAlerts", is(12)))
                .andExpect(jsonPath("$.acknowledgedAlerts", is(3)))
                .andExpect(jsonPath("$.transactionsToday", is(148)))
                .andExpect(jsonPath("$.closedToday", is(7)));

        verify(dashboardService, times(1)).getDashboardSummary();
    }

    @Test
    @DisplayName("GET /api/dashboard/summary - returns zero-value summary when no data exists")
    void getSummary_zeroState_returnsZeros() throws Exception {

        // Given
        DashboardSummaryResponseDTO summary = DashboardSummaryResponseDTO.builder()
                .openAlerts(0L)
                .acknowledgedAlerts(0L)
                .transactionsToday(0L)
                .closedToday(0L)
                .build();

        when(dashboardService.getDashboardSummary()).thenReturn(summary);

        // When
        mockMvc.perform(get(BASE_URL + "/summary"))

                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openAlerts", is(0)))
                .andExpect(jsonPath("$.acknowledgedAlerts", is(0)))
                .andExpect(jsonPath("$.transactionsToday", is(0)))
                .andExpect(jsonPath("$.closedToday", is(0)));

        verify(dashboardService, times(1)).getDashboardSummary();
    }

    @Test
    @DisplayName("GET /api/dashboard/summary - returns 500 when service throws unexpected exception")
    void getSummary_serviceThrowsUnexpectedException_returns500() throws Exception {

        // Given
        when(dashboardService.getDashboardSummary()).thenThrow(new RuntimeException("Unexpected DB failure"));

        // When
        mockMvc.perform(get(BASE_URL + "/summary"))

                // Then
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.error", is("Internal Server Error")))
                .andExpect(jsonPath("$.message", is("Unexpected server error.")))
                .andExpect(jsonPath("$.path", is("/api/dashboard/summary")));

        verify(dashboardService, times(1)).getDashboardSummary();
    }

    // -------------------------------------------------------------------------
    // GET /api/dashboard/recent-alerts
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/dashboard/recent-alerts - returns recent open alerts list")
    void getRecentOpenAlerts_success_returnsRecentAlerts() throws Exception {

        // Given
        DashboardRecentAlertItemDTO first = DashboardRecentAlertItemDTO.builder()
                .alertCode("ALT-2026-001")
                .severity(SeverityLevel.CRITICAL)
                .ruleName("Amount Threshold Rule")
                .accountId("ACC-001")
                .createdAt(LocalDateTime.of(2026, 8, 6, 9, 30, 0))
                .status(AlertStatus.OPEN)
                .build();

        DashboardRecentAlertItemDTO second = DashboardRecentAlertItemDTO.builder()
                .alertCode("ALT-2026-002")
                .severity(SeverityLevel.HIGH)
                .ruleName("Velocity Check Rule")
                .accountId("ACC-002")
                .createdAt(LocalDateTime.of(2026, 8, 6, 9, 10, 0))
                .status(AlertStatus.OPEN)
                .build();

        when(dashboardService.getRecentOpenAlerts()).thenReturn(List.of(first, second));

        // When
        mockMvc.perform(get(BASE_URL + "/recent-alerts"))

                // Then
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].alertCode", is("ALT-2026-001")))
                .andExpect(jsonPath("$[0].severity", is("CRITICAL")))
                .andExpect(jsonPath("$[0].ruleName", is("Amount Threshold Rule")))
                .andExpect(jsonPath("$[0].accountId", is("ACC-001")))
                .andExpect(jsonPath("$[0].status", is("OPEN")))
                .andExpect(jsonPath("$[1].alertCode", is("ALT-2026-002")))
                .andExpect(jsonPath("$[1].severity", is("HIGH")))
                .andExpect(jsonPath("$[1].status", is("OPEN")));

        verify(dashboardService, times(1)).getRecentOpenAlerts();
    }

    @Test
    @DisplayName("GET /api/dashboard/recent-alerts - returns empty list when no open alerts exist")
    void getRecentOpenAlerts_emptyList_returnsEmptyArray() throws Exception {

        // Given
        when(dashboardService.getRecentOpenAlerts()).thenReturn(Collections.emptyList());

        // When
        mockMvc.perform(get(BASE_URL + "/recent-alerts"))

                // Then
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)))
                .andExpect(content().json("[]"));

        verify(dashboardService, times(1)).getRecentOpenAlerts();
    }

    @Test
    @DisplayName("GET /api/dashboard/recent-alerts - returns 500 when service throws unexpected exception")
    void getRecentOpenAlerts_serviceThrowsUnexpectedException_returns500() throws Exception {

        // Given
        when(dashboardService.getRecentOpenAlerts()).thenThrow(new RuntimeException("Unexpected mapping error"));

        // When
        mockMvc.perform(get(BASE_URL + "/recent-alerts"))

                // Then
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.error", is("Internal Server Error")))
                .andExpect(jsonPath("$.message", is("Unexpected server error.")))
                .andExpect(jsonPath("$.path", is("/api/dashboard/recent-alerts")));

        verify(dashboardService, times(1)).getRecentOpenAlerts();
    }
}

