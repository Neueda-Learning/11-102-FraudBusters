package com.FraudBusters.TransactionMonitoring.models;

import com.FraudBusters.TransactionMonitoring.models.enums.InlineMode;
import com.FraudBusters.TransactionMonitoring.models.enums.SeverityLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_code", nullable = false, unique = true, length = 64)
    private String ruleCode;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "rule_type", nullable = false, length = 50)
    private String ruleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity_default", nullable = false, length = 20)
    private SeverityLevel severityDefault;

    @Enumerated(EnumType.STRING)
    @Column(name = "inline_mode", nullable = false, length = 20)
    private InlineMode inlineMode;

    @Column(name = "config_json", nullable = false, columnDefinition = "json")
    private String configJson;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}

