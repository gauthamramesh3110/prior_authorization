package com.lifeforce.payer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PayerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayerApplication.class, args);
    }

}
