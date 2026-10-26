package dev.rocky.resilience;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;

class DownstreamControllerTest {
    @Test void fallbackProvidesStableResponse() {
        DownstreamController controller = new DownstreamController(RestClient.builder(), "http://localhost:1");
        assertEquals("downstream temporarily unavailable",
            controller.fallback(new IllegalStateException("unreachable")));
    }
}
