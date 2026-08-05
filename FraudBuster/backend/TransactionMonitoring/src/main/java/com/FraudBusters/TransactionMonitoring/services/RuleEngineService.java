package com.FraudBusters.TransactionMonitoring.services;

import com.FraudBusters.TransactionMonitoring.models.TransactionEntity;


public interface RuleEngineService {

    boolean evaluateTransaction(TransactionEntity transaction);

}
