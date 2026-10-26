# Microservices Resilience Patterns

Java 17 / Spring Boot 3.3 example of resilient HTTP dependencies using Resilience4j, explicit timeouts, bounded retry and circuit-breaker fallback.

## Run

```bash
mvn -B clean verify
mvn spring-boot:run
```

By default the downstream service is at `http://localhost:8090`. Override with `DEMO_DOWNSTREAM_URL` after mapping the application property `demo.downstream.url`, or supply `--demo.downstream.url=http://localhost:8090` on the command line.

## Request flow

```mermaid
flowchart LR
  C[Caller] --> P[/proxy]
  P --> B[Resilience4j circuit breaker]
  B --> H[RestClient GET downstream /health]
  B -->|Failure or open circuit| F[Stable fallback]
  C --> R[/proxy-retry]
  R --> X[Resilience4j retry]
  X --> H
  X -->|Transient RestClientException| X
```

- `/proxy` demonstrates a circuit breaker with fallback response `downstream temporarily unavailable`.
- `/proxy-retry` demonstrates up to three attempts for `RestClientException`, with a 200 ms interval.
- HTTP connect timeout is 2 seconds; read timeout is 3 seconds.
- Circuit breaker: sliding window of 10 calls, minimum 5, 50% failure threshold, 10-second open wait and three half-open probes.
- `ExponentialBackoff` separately demonstrates capped exponential backoff with full jitter. The configured HTTP retry uses a fixed wait, **not** this helper.

## Tests

JUnit 5 tests cover retry delegation, circuit-breaker fallback, backoff bounds and validation. `DownstreamHttpIntegrationTest` starts a real local HTTP server that returns two 503 errors followed by a 200, and asserts the retry endpoint succeeds after exactly three requests. GitHub Actions runs `mvn -B verify`.

## Production considerations

Retry only idempotent operations or requests protected by idempotency keys. Add an overall deadline, retry budget, jitter and metrics for high-scale systems. A fallback must not conceal critical data loss. Protect secrets and configure TLS for real downstream services. The local HTTP test server is test-only.

## Interview prompts

Why can retries amplify overload? What is the difference between an open and half-open circuit breaker? When should a fallback return stale data versus an error?
