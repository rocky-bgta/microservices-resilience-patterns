package dev.rocky.resilience;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DownstreamHttpIntegrationTest {
    private static final AtomicInteger calls = new AtomicInteger();
    private static final HttpServer downstream = startDownstream();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("demo.downstream.url", () -> "http://localhost:" + downstream.getAddress().getPort());
        registry.add("resilience4j.retry.instances.downstreamRetry.waitDuration", () -> "10ms");
    }

    private static HttpServer startDownstream() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/health", exchange -> {
                int n = calls.incrementAndGet();
                int status = n < 3 ? 503 : 200;
                byte[] body = (status == 200 ? "healthy" : "unavailable")
                    .getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(status, body.length);
                try (var out = exchange.getResponseBody()) { out.write(body); }
            });
            server.start();
            return server;
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Autowired TestRestTemplate client;
    @LocalServerPort int port;

    @Test
    void retriesRealHttp503AndEventuallySucceeds() {
        calls.set(0);
        String response = client.getForObject("http://localhost:" + port + "/proxy-retry", String.class);
        assertEquals("healthy", response);
        assertEquals(3, calls.get());
    }
}
