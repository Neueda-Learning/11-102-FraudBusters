package com.FraudBusters.TransactionMonitoring.models;

import com.FraudBusters.TransactionMonitoring.models.enums.FinalDecision;
import com.FraudBusters.TransactionMonitoring.models.enums.MonitorState;
import com.FraudBusters.TransactionMonitoring.models.enums.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "txn_id", nullable = false, unique = true, length = 64)
    private String txnId;

    @Column(name = "account_id", nullable = false, length = 64)
    private String accountId;

    @Column(name = "customer_full_name", length = 120)
    private String customerFullName;

    @Column(name = "customer_email", length = 150)
    private String customerEmail;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Column(name = "payee_id", nullable = false, length = 64)
    private String payeeId;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "txn_type", nullable = false, length = 20)
    private TransactionType txnType;

    @Column(name = "txn_timestamp", nullable = false)
    private LocalDateTime txnTimestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "monitor_state", nullable = false, length = 20)
    private MonitorState monitorState;

    @Column(name = "hold_started_at")
    private LocalDateTime holdStartedAt;

    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "final_decision", nullable = false, length = 20)
    private FinalDecision finalDecision;

    @Column(name = "decision_reason", length = 120)
    private String decisionReason;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}

