package com.FraudBusters.TransactionMonitoring.exceptions;

/**
 * Raised when lifecycle action input is invalid (e.g. missing required reason).
 */
public class InvalidLifecycleActionException extends RuntimeException {

    public InvalidLifecycleActionException(String message) {
        super(message);
    }
}

