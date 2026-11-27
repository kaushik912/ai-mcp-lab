package com.aicommerce.carrier;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/** Real Streamable-HTTP handshake: initialize, then tools/list must expose the 3 carrier tools. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class McpServerSmokeTest {

    @LocalServerPort int port;

    @Test
    void streamableHttpExposesCarrierTools() throws Exception {
        HttpClient http = HttpClient.newHttpClient();
        String url = "http://localhost:" + port + "/mcp";
        String init = """
                {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18",
                "capabilities":{},"clientInfo":{"name":"smoke","version":"1"}}}""";
        HttpResponse<String> r1 = http.send(post(url, init, null), HttpResponse.BodyHandlers.ofString());
        assertThat(r1.statusCode()).isEqualTo(200);
        String session = r1.headers().firstValue("Mcp-Session-Id").orElse(null);

        http.send(post(url, "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}", session),
                HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> r2 = http.send(
                post(url, "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}", session),
                HttpResponse.BodyHandlers.ofString());
        assertThat(r2.body()).contains("trackShipment", "generateReturnShippingLabel", "findNearbyDropoffPoints");

        HttpResponse<String> r3 = http.send(post(url, """
                {"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"trackShipment",
                "arguments":{"trackingId":"TRK-BLR-77412"}}}""", session), HttpResponse.BodyHandlers.ofString());
        assertThat(r3.body()).contains("Whitefield").contains("eather");
    }

    private HttpRequest post(String url, String body, String session) {
        var b = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (session != null) b.header("Mcp-Session-Id", session);
        return b.build();
    }
}
