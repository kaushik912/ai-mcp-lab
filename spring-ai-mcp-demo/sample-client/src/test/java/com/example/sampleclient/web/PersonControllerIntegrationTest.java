package com.example.sampleclient.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Requires person-mcp-service (8060) and account-mcp-service (8040) already running,
 * and OPENROUTER_API_KEY set — see the root README "Running tests" section.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".+")
class PersonControllerIntegrationTest {

    @Autowired
    private RestTestClient restTestClient;

    @Test
    void findsPersonsByNationality() {
        restTestClient.get()
                .uri("/persons/nationality/Polish")
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(String.class)
                .value(body -> {
                    if (!body.contains("Anna") || !body.contains("Piotr")) {
                        throw new AssertionError(
                                "expected response to mention seeded Polish persons Anna and Piotr, was: " + body);
                    }
                });
    }

    @Test
    void countsPersonsByNationality() {
        restTestClient.get()
                .uri("/persons/count-by-nationality/Polish")
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(String.class)
                .value(body -> {
                    if (!body.contains("2")) {
                        throw new AssertionError(
                                "expected response to mention the seeded Polish person count (2), was: " + body);
                    }
                });
    }
}
