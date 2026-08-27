package com.cashbackrewards.compras;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableRetry
public class ComprasServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ComprasServiceApplication.class, args);
    }
}
