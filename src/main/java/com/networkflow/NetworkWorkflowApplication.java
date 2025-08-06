package com.networkflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main Spring Boot application for Network Device Configuration Workflow Builder
 */
@SpringBootApplication
@EnableAsync
public class NetworkWorkflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(NetworkWorkflowApplication.class, args);
    }
}