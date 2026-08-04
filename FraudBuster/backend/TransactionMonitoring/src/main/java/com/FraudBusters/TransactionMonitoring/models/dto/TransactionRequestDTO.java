package com.FraudBusters.TransactionMonitoring.models.dto;

import com.FraudBusters.TransactionMonitoring.models.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Incoming payload when a new transaction is submitted to the monitoring layer.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequestDTO {

    @NotBlank
    @Size(max = 64)
    private String txnId;

    @NotBlank
    @Size(max = 64)
    private String accountId;

    @Size(max = 120)
    private String customerFullName;

    @Size(max = 150)
    private String customerEmail;

    @Size(max = 20)
    private String customerPhone;

    @NotBlank
    @Size(max = 64)
    private String payeeId;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    /** ISO-4217 currency code, e.g. "USD" */
    @NotBlank
    @Size(min = 3, max = 3)
    private String currency;

    @NotNull
    private TransactionType txnType;

    @NotNull
    private LocalDateTime txnTimestamp;
}

