package com.FraudBusters.TransactionMonitoring.Controllers;

import com.FraudBusters.TransactionMonitoring.controllers.RuleController;
import com.FraudBusters.TransactionMonitoring.exceptions.GlobalApiExceptionHandler;
import com.FraudBusters.TransactionMonitoring.exceptions.ResourceNotFoundException;
import com.FraudBusters.TransactionMonitoring.models.dto.RuleListItemDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.RuleResponseDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.RulesListResponseDTO;
import com.FraudBusters.TransactionMonitoring.models.enums.InlineMode;
import com.FraudBusters.TransactionMonitoring.models.enums.SeverityLevel;
import com.FraudBusters.TransactionMonitoring.services.RuleService;
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

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link RuleController}.
 *
 * Uses @WebMvcTest to load only the web layer (no DB, no full Spring context).
 * RuleService is mocked via @MockitoBean.
 * All tests follow the Given / When / Then structure.
 *
 * Endpoints covered:
 *   GET  /api/rules            → getAllRules()
 *   GET  /api/rules/{ruleCode} → getRuleByCode()
 *   PUT  /api/rules/{ruleCode} → updateRule()
 */
@WebMvcTest(RuleController.class)
@Import(GlobalApiExceptionHandler.class)
class RuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RuleService ruleService;

    // ─────────────────────────────────────────────────────────────────────────
    //  Helper – builds sample DTOs
    // ─────────────────────────────────────────────────────────────────────────

    private RuleListItemDTO buildListItemDTO(String ruleCode, String severity, boolean isActive) {
        return RuleListItemDTO.builder()
                .ruleCode(ruleCode)
                .name("Rule: " + ruleCode)
                .description("Description for " + ruleCode)
                .parameter("{\"threshold\": 10000}")
                .severity(severity)
                .severityClass("sev-" + severity.toLowerCase())
                .isActive(isActive)
                .build();
    }

    private RuleResponseDTO buildRuleResponseDTO(String ruleCode) {
        return RuleResponseDTO.builder()
                .id(1L)
                .ruleCode(ruleCode)
                .name("Amount Threshold Rule")
                .description("Trigger alert when a single transaction exceeds the threshold.")
                .ruleType("AMOUNT")
                .severityDefault(SeverityLevel.HIGH)
                .inlineMode(InlineMode.INLINE)
                .isActive(true)
                .build();
    }

    private RulesListResponseDTO buildRulesListDTO(List<RuleListItemDTO> items) {
        int total = items.size();
        long active = items.stream().filter(r -> Boolean.TRUE.equals(r.getIsActive())).count();
        return RulesListResponseDTO.builder()
                .totalRules(total)
                .activeRules((int) active)
                .inactiveRules((int) (total - active))
                .rules(items)
                .build();
    }

    /** Valid PUT request body as JSON string */
    private static final String VALID_UPDATE_BODY = """
            {
              "description": "Updated rule description",
              "configJson": "{\\"threshold\\": 15000}",
              "severityDefault": "HIGH",
              "isActive": true
            }
            """;

    // ═════════════════════════════════════════════════════════════════════════
    //  GET /api/rules
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET /api/rules – returns rules page data with counts and list")
    void getAllRules_returnsRulesListResponse() throws Exception {

        // ── Given ─────────────────────────────────────────────────────────────
        RuleListItemDTO rule1 = buildListItemDTO("AMOUNT_THRESHOLD", "HIGH", true);
        RuleListItemDTO rule2 = buildListItemDTO("VELOCITY_CHECK",   "MEDIUM", true);
        RuleListItemDTO rule3 = buildListItemDTO("NEW_PAYEE_CHECK",  "LOW", false);
        RulesListResponseDTO response = buildRulesListDTO(List.of(rule1, rule2, rule3));

        when(ruleService.getRulesPageData()).thenReturn(response);

        // ── When ──────────────────────────────────────────────────────────────
        mockMvc.perform(get("/api/rules"))

        // ── Then ──────────────────────────────────────────────────────────────
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalRules",   is(3)))
                .andExpect(jsonPath("$.activeRules",  is(2)))
                .andExpect(jsonPath("$.inactiveRules",is(1)))
                .andExpect(jsonPath("$.rules",        hasSize(3)))
                .andExpect(jsonPath("$.rules[0].ruleCode",  is("AMOUNT_THRESHOLD")))
                .andExpect(jsonPath("$.rules[0].severity",  is("HIGH")))
                .andExpect(jsonPath("$.rules[0].isActive",  is(true)))
                .andExpect(jsonPath("$.rules[2].ruleCode",  is("NEW_PAYEE_CHECK")))
                .andExpect(jsonPath("$.rules[2].isActive",  is(false)));

        verify(ruleService, times(1)).getRulesPageData();
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/rules – returns empty rules list with zero counts when no rules exist")
    void getAllRules_noRules_returnsEmptyListWithZeroCounts() throws Exception {

        // ── Given ─────────────────────────────────────────────────────────────
        RulesListResponseDTO response = buildRulesListDTO(Collections.emptyList());
        when(ruleService.getRulesPageData()).thenReturn(response);

        // ── When ──────────────────────────────────────────────────────────────
        mockMvc.perform(get("/api/rules"))

        // ── Then ──────────────────────────────────────────────────────────────
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRules",   is(0)))
                .andExpect(jsonPath("$.activeRules",  is(0)))
                .andExpect(jsonPath("$.inactiveRules",is(0)))
                .andExpect(jsonPath("$.rules",        hasSize(0)));

        verify(ruleService, times(1)).getRulesPageData();
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/rules – returns 500 on unexpected service exception")
    void getAllRules_serviceThrowsUnexpectedException_returns500() throws Exception {

        // ── Given ─────────────────────────────────────────────────────────────
        when(ruleService.getRulesPageData())
                .thenThrow(new RuntimeException("DB connection lost"));

        // ── When ──────────────────────────────────────────────────────────────
        mockMvc.perform(get("/api/rules"))

        // ── Then ──────────────────────────────────────────────────────────────
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status",  is(500)))
                .andExpect(jsonPath("$.error",   is("Internal Server Error")))
                .andExpect(jsonPath("$.message", is("Unexpected server error.")))
                .andExpect(jsonPath("$.path",    is("/api/rules")));

        verify(ruleService, times(1)).getRulesPageData();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  GET /api/rules/{ruleCode}
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET /api/rules/{ruleCode} – returns rule detail when found")
    void getRuleByCode_found_returnsRuleResponseDTO() throws Exception {

        // ── Given ─────────────────────────────────────────────────────────────
        RuleResponseDTO dto = buildRuleResponseDTO("AMOUNT_THRESHOLD");
        when(ruleService.getRuleByCode("AMOUNT_THRESHOLD")).thenReturn(dto);

        // ── When ──────────────────────────────────────────────────────────────
        mockMvc.perform(get("/api/rules/{ruleCode}", "AMOUNT_THRESHOLD"))

        // ── Then ──────────────────────────────────────────────────────────────
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id",              is(1)))
                .andExpect(jsonPath("$.ruleCode",        is("AMOUNT_THRESHOLD")))
                .andExpect(jsonPath("$.name",            is("Amount Threshold Rule")))
                .andExpect(jsonPath("$.ruleType",        is("AMOUNT")))
                .andExpect(jsonPath("$.severityDefault", is("HIGH")))
                .andExpect(jsonPath("$.inlineMode",      is("INLINE")))
                .andExpect(jsonPath("$.isActive",        is(true)));

        verify(ruleService, times(1)).getRuleByCode("AMOUNT_THRESHOLD");
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/rules/{ruleCode} – returns 404 with ApiError when rule not found")
    void getRuleByCode_notFound_returns404WithApiError() throws Exception {

        // ── Given ─────────────────────────────────────────────────────────────
        String missingCode = "NONEXISTENT_RULE";
        when(ruleService.getRuleByCode(missingCode))
                .thenThrow(new ResourceNotFoundException(
                        "Rule not found with code: " + missingCode));

        // ── When ──────────────────────────────────────────────────────────────
        mockMvc.perform(get("/api/rules/{ruleCode}", missingCode))

        // ── Then ──────────────────────────────────────────────────────────────
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status",  is(404)))
                .andExpect(jsonPath("$.error",   is("Not Found")))
                .andExpect(jsonPath("$.message", containsString("NONEXISTENT_RULE")))
                .andExpect(jsonPath("$.path",    is("/api/rules/" + missingCode)));

        verify(ruleService, times(1)).getRuleByCode(missingCode);
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/rules/{ruleCode} – returns 500 on unexpected service exception")
    void getRuleByCode_serviceThrowsUnexpectedException_returns500() throws Exception {

        // ── Given ─────────────────────────────────────────────────────────────
        when(ruleService.getRuleByCode("VELOCITY_CHECK"))
                .thenThrow(new RuntimeException("Unexpected mapping failure"));

        // ── When ──────────────────────────────────────────────────────────────
        mockMvc.perform(get("/api/rules/VELOCITY_CHECK"))

        // ── Then ──────────────────────────────────────────────────────────────
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status",  is(500)))
                .andExpect(jsonPath("$.error",   is("Internal Server Error")))
                .andExpect(jsonPath("$.message", is("Unexpected server error.")));

        verify(ruleService, times(1)).getRuleByCode("VELOCITY_CHECK");
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  PUT /api/rules/{ruleCode}
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("PUT /api/rules/{ruleCode} – valid request with correct password returns updated rule")
    void updateRule_validRequestWithCorrectPassword_returnsUpdatedRule() throws Exception {

        // ── Given ─────────────────────────────────────────────────────────────
        RuleListItemDTO updated = buildListItemDTO("AMOUNT_THRESHOLD", "HIGH", true);
        when(ruleService.updateRuleForConfigure(eq("AMOUNT_THRESHOLD"), any(), eq("12345")))
                .thenReturn(updated);

        // ── When ──────────────────────────────────────────────────────────────
        mockMvc.perform(put("/api/rules/{ruleCode}", "AMOUNT_THRESHOLD")
                        .header("X-Operator-Password", "12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_BODY))

        // ── Then ──────────────────────────────────────────────────────────────
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ruleCode",  is("AMOUNT_THRESHOLD")))
                .andExpect(jsonPath("$.severity",  is("HIGH")))
                .andExpect(jsonPath("$.isActive",  is(true)));

        verify(ruleService, times(1))
                .updateRuleForConfigure(eq("AMOUNT_THRESHOLD"), any(), eq("12345"));
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/rules/{ruleCode} – wrong operator password returns 400 Bad Request (no body)")
    void updateRule_wrongPassword_returns400() throws Exception {

        // ── Given ─────────────────────────────────────────────────────────────
        when(ruleService.updateRuleForConfigure(eq("AMOUNT_THRESHOLD"), any(), eq("wrongPass")))
                .thenThrow(new IllegalArgumentException("Invalid operator password."));

        // ── When ──────────────────────────────────────────────────────────────
        mockMvc.perform(put("/api/rules/{ruleCode}", "AMOUNT_THRESHOLD")
                        .header("X-Operator-Password", "wrongPass")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_BODY))

        // ── Then ──────────────────────────────────────────────────────────────
                .andExpect(status().isBadRequest());
        // Note: controller catches IllegalArgumentException and returns 400 with no body

        verify(ruleService, times(1))
                .updateRuleForConfigure(eq("AMOUNT_THRESHOLD"), any(), eq("wrongPass"));
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/rules/{ruleCode} – missing X-Operator-Password header is passed as null, wrong password returns 400")
    void updateRule_missingPasswordHeader_returns400() throws Exception {

        // ── Given ─────────────────────────────────────────────────────────────
        // Header is optional (required = false); service receives null → throws IllegalArgumentException
        when(ruleService.updateRuleForConfigure(eq("AMOUNT_THRESHOLD"), any(), isNull()))
                .thenThrow(new IllegalArgumentException("Operator password is required."));

        // ── When ──────────────────────────────────────────────────────────────
        mockMvc.perform(put("/api/rules/{ruleCode}", "AMOUNT_THRESHOLD")
                        // deliberately no X-Operator-Password header
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_BODY))

        // ── Then ──────────────────────────────────────────────────────────────
                .andExpect(status().isBadRequest());

        verify(ruleService, times(1))
                .updateRuleForConfigure(eq("AMOUNT_THRESHOLD"), any(), isNull());
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/rules/{ruleCode} – rule not found returns 404 with ApiError")
    void updateRule_ruleNotFound_returns404WithApiError() throws Exception {

        // ── Given ─────────────────────────────────────────────────────────────
        when(ruleService.updateRuleForConfigure(eq("UNKNOWN_RULE"), any(), eq("12345")))
                .thenThrow(new ResourceNotFoundException("Rule not found with code: UNKNOWN_RULE"));

        // ── When ──────────────────────────────────────────────────────────────
        mockMvc.perform(put("/api/rules/{ruleCode}", "UNKNOWN_RULE")
                        .header("X-Operator-Password", "12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_BODY))

        // ── Then ──────────────────────────────────────────────────────────────
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status",  is(404)))
                .andExpect(jsonPath("$.error",   is("Not Found")))
                .andExpect(jsonPath("$.message", containsString("UNKNOWN_RULE")))
                .andExpect(jsonPath("$.path",    is("/api/rules/UNKNOWN_RULE")));

        verify(ruleService, times(1))
                .updateRuleForConfigure(eq("UNKNOWN_RULE"), any(), eq("12345"));
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/rules/{ruleCode} – missing request body returns 400 (malformed JSON)")
    void updateRule_missingRequestBody_returns400() throws Exception {

        // ── Given ─────────────────────────────────────────────────────────────
        // No body sent → Spring throws HttpMessageNotReadableException → GlobalApiExceptionHandler returns 400

        // ── When ──────────────────────────────────────────────────────────────
        mockMvc.perform(put("/api/rules/{ruleCode}", "AMOUNT_THRESHOLD")
                        .header("X-Operator-Password", "12345")
                        .contentType(MediaType.APPLICATION_JSON))
                // No .content(...) → empty body

        // ── Then ──────────────────────────────────────────────────────────────
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status",  is(400)))
                .andExpect(jsonPath("$.message", is("Malformed request body.")));

        verifyNoInteractions(ruleService);
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/rules/{ruleCode} – blank configJson (@NotBlank) fails validation and returns 400")
    void updateRule_blankConfigJson_failsValidation_returns400() throws Exception {

        // ── Given ─────────────────────────────────────────────────────────────
        String invalidBody = """
                {
                  "description": "Some description",
                  "configJson": "",
                  "severityDefault": "HIGH",
                  "isActive": true
                }
                """;

        // ── When ──────────────────────────────────────────────────────────────
        mockMvc.perform(put("/api/rules/{ruleCode}", "AMOUNT_THRESHOLD")
                        .header("X-Operator-Password", "12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))

        // ── Then ──────────────────────────────────────────────────────────────
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status",  is(400)))
                .andExpect(jsonPath("$.message", containsString("configJson")));

        verifyNoInteractions(ruleService);
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/rules/{ruleCode} – null severityDefault (@NotNull) fails validation and returns 400")
    void updateRule_nullSeverityDefault_failsValidation_returns400() throws Exception {

        // ── Given ─────────────────────────────────────────────────────────────
        String invalidBody = """
                {
                  "description": "Some description",
                  "configJson": "{\\"threshold\\": 1000}",
                  "isActive": true
                }
                """;
        // severityDefault is missing → null → @NotNull fails

        // ── When ──────────────────────────────────────────────────────────────
        mockMvc.perform(put("/api/rules/{ruleCode}", "AMOUNT_THRESHOLD")
                        .header("X-Operator-Password", "12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))

        // ── Then ──────────────────────────────────────────────────────────────
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status",  is(400)))
                .andExpect(jsonPath("$.message", containsString("severityDefault")));

        verifyNoInteractions(ruleService);
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/rules/{ruleCode} – null isActive (@NotNull) fails validation and returns 400")
    void updateRule_nullIsActive_failsValidation_returns400() throws Exception {

        // ── Given ─────────────────────────────────────────────────────────────
        String invalidBody = """
                {
                  "description": "Some description",
                  "configJson": "{\\"threshold\\": 1000}",
                  "severityDefault": "MEDIUM"
                }
                """;
        // isActive is missing → null → @NotNull fails

        // ── When ──────────────────────────────────────────────────────────────
        mockMvc.perform(put("/api/rules/{ruleCode}", "AMOUNT_THRESHOLD")
                        .header("X-Operator-Password", "12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))

        // ── Then ──────────────────────────────────────────────────────────────
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status",  is(400)))
                .andExpect(jsonPath("$.message", containsString("isActive")));

        verifyNoInteractions(ruleService);
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/rules/{ruleCode} – description exceeds @Size(max=500) fails validation and returns 400")
    void updateRule_descriptionTooLong_failsValidation_returns400() throws Exception {

        // ── Given ─────────────────────────────────────────────────────────────
        String tooLongDescription = "A".repeat(501);
        String invalidBody = String.format("""
                {
                  "description": "%s",
                  "configJson": "{\\"threshold\\": 1000}",
                  "severityDefault": "LOW",
                  "isActive": false
                }
                """, tooLongDescription);

        // ── When ──────────────────────────────────────────────────────────────
        mockMvc.perform(put("/api/rules/{ruleCode}", "AMOUNT_THRESHOLD")
                        .header("X-Operator-Password", "12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))

        // ── Then ──────────────────────────────────────────────────────────────
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status",  is(400)))
                .andExpect(jsonPath("$.message", containsString("description")));

        verifyNoInteractions(ruleService);
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/rules/{ruleCode} – unexpected service exception returns 500")
    void updateRule_serviceThrowsUnexpectedException_returns500() throws Exception {

        // ── Given ─────────────────────────────────────────────────────────────
        when(ruleService.updateRuleForConfigure(eq("AMOUNT_THRESHOLD"), any(), eq("12345")))
                .thenThrow(new RuntimeException("Unexpected DB error"));

        // ── When ──────────────────────────────────────────────────────────────
        mockMvc.perform(put("/api/rules/{ruleCode}", "AMOUNT_THRESHOLD")
                        .header("X-Operator-Password", "12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_BODY))

        // ── Then ──────────────────────────────────────────────────────────────
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status",  is(500)))
                .andExpect(jsonPath("$.error",   is("Internal Server Error")))
                .andExpect(jsonPath("$.message", is("Unexpected server error.")));

        verify(ruleService, times(1))
                .updateRuleForConfigure(eq("AMOUNT_THRESHOLD"), any(), eq("12345"));
    }
}

