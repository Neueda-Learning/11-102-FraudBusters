package com.FraudBusters.TransactionMonitoring.services.Impl;

import com.FraudBusters.TransactionMonitoring.models.AlertEntity;
import com.FraudBusters.TransactionMonitoring.repository.AlertEntityRepository;
import com.FraudBusters.TransactionMonitoring.services.AlertOpsService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlertOpsServiceImpl implements AlertOpsService {

    private AlertEntityRepository alertEntityRepository;


    @Override
    public List<AlertEntity> getAllAlerts() {
        return alertEntityRepository.findAll();
    }

    @Override
    public List<AlertEntity> getActiveAlerts() {
        return alertEntityRepository.findAlertsWhoseAlertStatusIsNotDismissedAndClosed();
    }

    @Override
    public AlertEntity getAlertById(Long alertId) {
        return alertEntityRepository.findById(alertId).orElse(null);
    }
}
