package com.watchstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // needed for PaymentReconciliationJob
public class WatchStoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(WatchStoreApplication.class, args);
    }
}
