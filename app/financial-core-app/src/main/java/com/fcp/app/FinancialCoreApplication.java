package com.fcp.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = "com.fcp")
@EnableTransactionManagement
public class FinancialCoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(FinancialCoreApplication.class, args);
    }
}