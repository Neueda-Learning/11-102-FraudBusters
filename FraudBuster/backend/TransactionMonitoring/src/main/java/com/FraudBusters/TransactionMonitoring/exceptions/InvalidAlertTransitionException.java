package com.FraudBusters.TransactionMonitoring.exceptions;

/**
 * Raised when an alert lifecycle transition is not allowed from current state.
 */
public class InvalidAlertTransitionException extends RuntimeException {

    public InvalidAlertTransitionException(String message) {
        super(message);
    }
}

