package com.FraudBusters.TransactionMonitoring.Services;

import com.FraudBusters.TransactionMonitoring.exceptions.ResourceNotFoundException;
import com.FraudBusters.TransactionMonitoring.models.RuleEntity;
import com.FraudBusters.TransactionMonitoring.models.dto.RuleListItemDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.RuleResponseDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.RuleUpdateRequestDTO;
import com.FraudBusters.TransactionMonitoring.models.dto.RulesListResponseDTO;
import com.FraudBusters.TransactionMonitoring.models.enums.InlineMode;
import com.FraudBusters.TransactionMonitoring.models.enums.SeverityLevel;
import com.FraudBusters.TransactionMonitoring.repository.RuleEntityRepository;
import com.FraudBusters.TransactionMonitoring.services.RuleService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleServiceTest {

    @Mock
    private RuleEntityRepository ruleEntityRepository;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RuleService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "configurePassword", "secret-123");
    }

    @Test
    void getAllActiveRules_whenRulesExist_thenMapsAllFieldsToResponseDto() {
        // given
        RuleEntity rule = ruleEntity(1L, "VELOCITY_CHECK", "Velocity Rule", "Velocity description",
                "VELOCITY", SeverityLevel.HIGH, InlineMode.INLINE, true, "{\"windowMinutes\":5}");
        when(ruleEntityRepository.findByIsActiveTrueAndIsDeletedFalse()).thenReturn(List.of(rule));

        // when
        List<RuleResponseDTO> response = service.getAllActiveRules();

        // then
        assertEquals(1, response.size());
        RuleResponseDTO dto = response.get(0);
        assertEquals(1L, dto.getId());
        assertEquals("VELOCITY_CHECK", dto.getRuleCode());
        assertEquals("Velocity Rule", dto.getName());
        assertEquals("Velocity description", dto.getDescription());
        assertEquals("VELOCITY", dto.getRuleType());
        assertEquals(SeverityLevel.HIGH, dto.getSeverityDefault());
        assertEquals(InlineMode.INLINE, dto.getInlineMode());
        assertTrue(dto.getIsActive());
    }

    @Test
    void getRulesPageData_whenRulesMixedActiveInactiveAndNullActive_thenBuildsCorrectCountsAndClasses() {
        // given
        RuleEntity activeCritical = ruleEntity(2L, "AMOUNT_THRESHOLD", "Amount Rule", "Amount description",
                "AMOUNT", SeverityLevel.CRITICAL, InlineMode.INLINE, true, "{\"thresholdAmount\":2500}");
        RuleEntity inactiveMedium = ruleEntity(3L, "DAILY_LIMIT", "Daily Rule", "Daily description",
                "LIMIT", SeverityLevel.MEDIUM, InlineMode.POST_AUTH, false, "{\"dailyLimitAmount\":10000}");
        RuleEntity nullActiveNullSeverity = ruleEntity(4L, "NEW_PAYEE", "Payee Rule", "Payee description",
                "PAYEE", null, InlineMode.INLINE, null, "{}");

        when(ruleEntityRepository.findByIsDeletedFalse()).thenReturn(List.of(activeCritical, inactiveMedium, nullActiveNullSeverity));

        // when
        RulesListResponseDTO response = service.getRulesPageData();

        // then
        assertEquals(3, response.getTotalRules());
        assertEquals(1, response.getActiveRules());
        assertEquals(2, response.getInactiveRules());
        assertEquals(3, response.getRules().size());

        RuleListItemDTO first = response.getRules().get(0);
        assertEquals("AMOUNT_THRESHOLD", first.getRuleCode());
        assertEquals("CRITICAL", first.getSeverity());
        assertEquals("sev-high", first.getSeverityClass());
        assertTrue(first.getIsActive());

        RuleListItemDTO second = response.getRules().get(1);
        assertEquals("MEDIUM", second.getSeverity());
        assertEquals("sev-medium", second.getSeverityClass());
        assertFalse(second.getIsActive());

        RuleListItemDTO third = response.getRules().get(2);
        assertEquals("LOW", third.getSeverity());
        assertEquals("sev-low", third.getSeverityClass());
        assertFalse(third.getIsActive());
    }

    @Test
    void getRuleByCode_whenRuleNotFound_thenThrowsResourceNotFoundException() {
        // given
        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("UNKNOWN")).thenReturn(Optional.empty());

        // when
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.getRuleByCode("UNKNOWN")
        );

        // then
        assertEquals("Rule not found with code: UNKNOWN", exception.getMessage());
    }

    @Test
    void updateRuleForConfigure_whenOperatorPasswordMissing_thenThrowsValidationError() {
        // given
        RuleUpdateRequestDTO request = RuleUpdateRequestDTO.builder()
                .description("desc")
                .configJson("{}")
                .severityDefault(SeverityLevel.HIGH)
                .isActive(true)
                .build();

        // when
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateRuleForConfigure("AMOUNT_THRESHOLD", request, "  ")
        );

        // then
        assertEquals("Operator password is required.", exception.getMessage());
        verifyNoInteractions(ruleEntityRepository, objectMapper);
    }

    @Test
    void updateRuleForConfigure_whenOperatorPasswordInvalid_thenThrowsValidationError() {
        // given
        RuleUpdateRequestDTO request = RuleUpdateRequestDTO.builder()
                .description("desc")
                .configJson("{}")
                .severityDefault(SeverityLevel.HIGH)
                .isActive(true)
                .build();

        // when
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateRuleForConfigure("AMOUNT_THRESHOLD", request, "wrong-pass")
        );

        // then
        assertEquals("Invalid operator password.", exception.getMessage());
        verifyNoInteractions(ruleEntityRepository, objectMapper);
    }

    @Test
    void updateRuleForConfigure_whenRequestIsNull_thenThrowsValidationError() {
        // given

        // when
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateRuleForConfigure("AMOUNT_THRESHOLD", null, "secret-123")
        );

        // then
        assertEquals("Rule update payload is required.", exception.getMessage());
        verifyNoInteractions(ruleEntityRepository, objectMapper);
    }

    @Test
    void updateRuleForConfigure_whenConfigJsonBlank_thenThrowsValidationError() {
        // given
        RuleUpdateRequestDTO request = RuleUpdateRequestDTO.builder()
                .description("desc")
                .configJson("   ")
                .severityDefault(SeverityLevel.HIGH)
                .isActive(true)
                .build();

        // when
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateRuleForConfigure("AMOUNT_THRESHOLD", request, "secret-123")
        );

        // then
        assertEquals("Rule configJson is required.", exception.getMessage());
        verifyNoInteractions(ruleEntityRepository);
    }

    @Test
    void updateRuleForConfigure_whenConfigJsonInvalid_thenThrowsValidationError() throws Exception {
        // given
        RuleUpdateRequestDTO request = RuleUpdateRequestDTO.builder()
                .description("desc")
                .configJson("{not-json}")
                .severityDefault(SeverityLevel.HIGH)
                .isActive(true)
                .build();
        when(objectMapper.readTree("{not-json}")).thenThrow(new RuntimeException("bad json"));

        // when
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateRuleForConfigure("AMOUNT_THRESHOLD", request, "secret-123")
        );

        // then
        assertEquals("configJson must be valid JSON.", exception.getMessage());
        verify(objectMapper).readTree("{not-json}");
        verifyNoInteractions(ruleEntityRepository);
    }

    @Test
    void updateRuleForConfigure_whenRuleNotFound_thenThrowsResourceNotFoundException() throws Exception {
        // given
        RuleUpdateRequestDTO request = RuleUpdateRequestDTO.builder()
                .description("desc")
                .configJson("{\"thresholdAmount\":2500}")
                .severityDefault(SeverityLevel.HIGH)
                .isActive(true)
                .build();
        when(objectMapper.readTree("{\"thresholdAmount\":2500}")).thenReturn(anyJsonNode());
        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("AMOUNT_THRESHOLD")).thenReturn(Optional.empty());

        // when
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.updateRuleForConfigure("AMOUNT_THRESHOLD", request, "secret-123")
        );

        // then
        assertEquals("Rule not found with code: AMOUNT_THRESHOLD", exception.getMessage());
        verify(ruleEntityRepository, never()).save(any(RuleEntity.class));
    }

    @Test
    void updateRuleForConfigure_whenValidPayload_thenNormalizesAndSavesAndReturnsMappedListItem() throws Exception {
        // given
        RuleEntity existing = ruleEntity(5L, "DAILY_LIMIT", "Daily Rule", "old desc",
                "LIMIT", SeverityLevel.LOW, InlineMode.INLINE, true, "{\"dailyLimitAmount\":10000}");

        RuleUpdateRequestDTO request = RuleUpdateRequestDTO.builder()
                .description("  Updated description  ")
                .configJson("  {\"dailyLimitAmount\":12000}  ")
                .severityDefault(SeverityLevel.CRITICAL)
                .isActive(false)
                .build();

        when(objectMapper.readTree("{\"dailyLimitAmount\":12000}")).thenReturn(anyJsonNode());
        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("DAILY_LIMIT")).thenReturn(Optional.of(existing));
        when(ruleEntityRepository.save(any(RuleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        RuleListItemDTO response = service.updateRuleForConfigure("DAILY_LIMIT", request, "secret-123");

        // then
        ArgumentCaptor<RuleEntity> entityCaptor = ArgumentCaptor.forClass(RuleEntity.class);
        verify(ruleEntityRepository).save(entityCaptor.capture());

        RuleEntity saved = entityCaptor.getValue();
        assertEquals("Updated description", saved.getDescription());
        assertEquals("{\"dailyLimitAmount\":12000}", saved.getConfigJson());
        assertEquals(SeverityLevel.CRITICAL, saved.getSeverityDefault());
        assertFalse(saved.getIsActive());

        assertNotNull(response);
        assertEquals("DAILY_LIMIT", response.getRuleCode());
        assertEquals("CRITICAL", response.getSeverity());
        assertEquals("sev-high", response.getSeverityClass());
        assertFalse(response.getIsActive());
        assertEquals("{\"dailyLimitAmount\":12000}", response.getParameter());
    }

    @Test
    void updateRuleForConfigure_whenDescriptionBlank_thenNormalizesDescriptionToNull() throws Exception {
        // given
        RuleEntity existing = ruleEntity(6L, "NEW_PAYEE", "Payee Rule", "old",
                "PAYEE", SeverityLevel.MEDIUM, InlineMode.INLINE, true, "{}");

        RuleUpdateRequestDTO request = RuleUpdateRequestDTO.builder()
                .description("   ")
                .configJson("{\"key\":\"value\"}")
                .severityDefault(SeverityLevel.MEDIUM)
                .isActive(true)
                .build();

        when(objectMapper.readTree("{\"key\":\"value\"}")).thenReturn(anyJsonNode());
        when(ruleEntityRepository.findByRuleCodeAndIsDeletedFalse("NEW_PAYEE")).thenReturn(Optional.of(existing));
        when(ruleEntityRepository.save(any(RuleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        RuleListItemDTO response = service.updateRuleForConfigure("NEW_PAYEE", request, "secret-123");

        // then
        assertNull(existing.getDescription());
        assertEquals("MEDIUM", response.getSeverity());
        assertEquals("sev-medium", response.getSeverityClass());
    }

    private RuleEntity ruleEntity(Long id,
                                  String code,
                                  String name,
                                  String description,
                                  String ruleType,
                                  SeverityLevel severity,
                                  InlineMode inlineMode,
                                  Boolean active,
                                  String configJson) {
        RuleEntity rule = new RuleEntity();
        rule.setId(id);
        rule.setRuleCode(code);
        rule.setName(name);
        rule.setDescription(description);
        rule.setRuleType(ruleType);
        rule.setSeverityDefault(severity);
        rule.setInlineMode(inlineMode);
        rule.setIsActive(active);
        rule.setConfigJson(configJson);
        rule.setIsDeleted(false);
        return rule;
    }

    private JsonNode anyJsonNode() {
        return new ObjectMapper().createObjectNode();
    }
}
