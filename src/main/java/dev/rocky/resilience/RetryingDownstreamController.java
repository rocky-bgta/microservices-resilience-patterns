package dev.rocky.resilience;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RetryingDownstreamController {
    private final RetryingDownstreamService service;

    public RetryingDownstreamController(RetryingDownstreamService service) {
        this.service = service;
    }

    @GetMapping("/proxy-retry")
    public String proxyWithRetry() {
        return service.fetchHealth();
    }
}
