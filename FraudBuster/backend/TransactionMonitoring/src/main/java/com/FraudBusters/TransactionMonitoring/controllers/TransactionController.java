package com.FraudBusters.TransactionMonitoring.controllers;

import com.FraudBusters.TransactionMonitoring.models.dto.TransactionResponseDTO;
import com.FraudBusters.TransactionMonitoring.services.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for the Transactions screen on the frontend.
 *
 * GET /api/transactions          → list all transactions
 * GET /api/transactions/{txnId}  → single transaction by business ID
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * Returns all transactions in the system.
     * Frontend uses this to populate the Transactions list table.
     *
     * Response example:
     * [
     *   {
     *     "id": 1,
     *     "txnId": "TXN-AMT-001",
     *     "accountId": "acct_amt_001",
     *     "amount": 12500.00,
     *     "currency": "USD",
     *     "txnType": "DEBIT",
     *     "monitorState": "RECEIVED",
     *     "finalDecision": "PENDING",
     *     ...
     *   }
     * ]
     */
    @GetMapping
    public ResponseEntity<List<TransactionResponseDTO>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    /**
     * Returns a single transaction by its business txnId.
     * Returns 404 if not found.
     *
     * @param txnId  e.g. "TXN-AMT-001"
     */
    @GetMapping("/{txnId}")
    public ResponseEntity<TransactionResponseDTO> getTransactionByTxnId(@PathVariable String txnId) {
        return ResponseEntity.ok(transactionService.getTransactionByTxnId(txnId));
    }
}

