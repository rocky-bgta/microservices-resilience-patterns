package dev.rocky.resilience;

import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/** Retry is applied through Spring AOP when invoked from another bean. */
@Service
public class RetryingDownstreamService {
    private final RestClient client;

    public RetryingDownstreamService(RestClient.Builder builder,
            @Value("${demo.downstream.url:http://localhost:8090}") String baseUrl) {
        this.client = builder.baseUrl(baseUrl).build();
    }

    @Retry(name = "downstreamRetry")
    public String fetchHealth() {
        return client.get().uri("/health").retrieve().body(String.class);
    }
}
