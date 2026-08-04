package com.FraudBusters.TransactionMonitoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TransactionMonitoringApplication {

	public static void main(String[] args) {
		System.out.println("Starting Transaction Monitoring Application...");
		SpringApplication.run(TransactionMonitoringApplication.class, args);
        System.out.println("After Starting Transaction Monitoring Application...");
	}

}
