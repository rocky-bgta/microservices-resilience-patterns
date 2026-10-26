package dev.rocky.resilience;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
public class DownstreamController {
    private final RestClient client;

    public DownstreamController(RestClient.Builder builder,
                                @Value("${demo.downstream.url:http://localhost:8090}") String baseUrl) {
        this.client = builder.baseUrl(baseUrl).build();
    }

    @GetMapping("/proxy")
    @CircuitBreaker(name = "downstream", fallbackMethod = "fallback")
    public String proxy() {
        return client.get().uri("/health").retrieve().body(String.class);
    }

    public String fallback(Throwable failure) {
        return "downstream temporarily unavailable";
    }
}
