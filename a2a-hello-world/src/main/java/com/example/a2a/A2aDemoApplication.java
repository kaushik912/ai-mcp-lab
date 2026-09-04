package com.example.a2a;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@SpringBootApplication
public class A2aDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(A2aDemoApplication.class, args);
    }

    /** Used by the orchestrator to make outbound A2A calls to the agents. */
    @Bean
    RestClient restClient() {
        return RestClient.create();
    }
}