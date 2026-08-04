package com.FraudBusters.TransactionMonitoring.services;

import com.FraudBusters.TransactionMonitoring.models.RuleEntity;
import com.FraudBusters.TransactionMonitoring.models.TransactionEntity;
import org.springframework.stereotype.Service;

import java.util.List;


public interface RuleEngineService {

    public boolean evaluateTransactionUsingAmountThreshold(TransactionEntity transaction);

}
