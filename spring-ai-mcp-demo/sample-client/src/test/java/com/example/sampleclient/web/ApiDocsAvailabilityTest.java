package com.example.sampleclient.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Requires person-mcp-service (8060) and account-mcp-service (8040) already running
 * — the sample-client Spring context connects to both via SSE at startup regardless
 * of which endpoint is under test. Gated on OPENROUTER_API_KEY (not used directly
 * here) purely so `mvn test` self-skips in the same conditions as the other
 * sample-client integration tests — see the root README "Running tests" section.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".+")
class ApiDocsAvailabilityTest {

    @Autowired
    private RestTestClient restTestClient;

    @Test
    void openApiSpecIsReachable() {
        restTestClient.get()
                .uri("/v3/api-docs")
                .exchange()
                .expectStatus().is2xxSuccessful();
    }

    @Test
    void swaggerUiIsReachable() {
        restTestClient.get()
                .uri("/swagger-ui.html")
                .exchange()
                .expectStatus().is3xxRedirection();
    }
}
