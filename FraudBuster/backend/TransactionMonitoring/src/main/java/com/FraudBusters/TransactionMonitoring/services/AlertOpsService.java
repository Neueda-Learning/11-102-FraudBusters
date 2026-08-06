package com.FraudBusters.TransactionMonitoring.services;


import com.FraudBusters.TransactionMonitoring.models.AlertEntity;

import java.util.List;

public interface AlertOpsService {

    List<AlertEntity> getAllAlerts();

    List<AlertEntity> getActiveAlerts();

    AlertEntity getAlertById(Long alertId);


}
