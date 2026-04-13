package com.anju;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AnjuBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnjuBackendApplication.class, args);
    }
}
