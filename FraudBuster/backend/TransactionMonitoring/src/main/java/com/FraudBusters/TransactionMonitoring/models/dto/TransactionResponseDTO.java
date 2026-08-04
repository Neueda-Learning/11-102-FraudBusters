package com.FraudBusters.TransactionMonitoring.models.dto;

import com.FraudBusters.TransactionMonitoring.models.enums.FinalDecision;
import com.FraudBusters.TransactionMonitoring.models.enums.MonitorState;
import com.FraudBusters.TransactionMonitoring.models.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response DTO returned to the frontend for the Transactions list / detail screen.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponseDTO {

    private Long id;
    private String txnId;
    private String accountId;
    private String customerFullName;
    private String customerEmail;
    private String customerPhone;
    private String payeeId;
    private BigDecimal amount;
    private String currency;
    private TransactionType txnType;
    private LocalDateTime txnTimestamp;
    private MonitorState monitorState;
    private LocalDateTime holdStartedAt;
    private LocalDateTime holdExpiresAt;
    private FinalDecision finalDecision;
    private String decisionReason;
    private LocalDateTime decidedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

