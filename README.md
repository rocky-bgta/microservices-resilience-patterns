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


## Step-by-step local quickstart

**Environment:** JDK **17**, Maven **3.9+**, Git, a terminal and internet access for dependencies. No Docker, Kafka or database is required. The Spring Boot app listens on **8080** by default; an optional mock downstream HTTP service listens on **8090**.

1. Clone and verify prerequisites:
   ```bash
   git clone https://github.com/rocky-bgta/microservices-resilience-patterns.git
   cd microservices-resilience-patterns
   java -version
   mvn -version
   ```
2. Run automated unit and real-HTTP integration tests:
   ```bash
   mvn -B clean verify
   ```
   Expected: `BUILD SUCCESS`. The integration test creates its **own local HTTP server**, so it does not need a separately running downstream service.
3. Start Spring Boot in terminal A:
   ```bash
   mvn spring-boot:run
   ```
4. With **no downstream service running** on port 8090, use terminal B to test the circuit-breaker fallback:
   ```bash
   curl http://localhost:8080/proxy
   ```
   Expected response: `downstream temporarily unavailable`. Repeated failures contribute to the circuit-breaker state; the endpoint is intentionally designed to return a fallback.
5. To observe a successful downstream call, start a **mock downstream** in terminal C. Python 3 is **optional** and only needed for this manual demonstration. From any temporary empty folder create `mock_downstream.py`:
   ```python
   from http.server import BaseHTTPRequestHandler, HTTPServer
   class Handler(BaseHTTPRequestHandler):
       def do_GET(self):
           if self.path == "/health":
               self.send_response(200)
               self.end_headers()
               self.wfile.write(b"healthy")
           else:
               self.send_error(404)
   HTTPServer(("127.0.0.1", 8090), Handler).serve_forever()
   ```
   Start it with `python mock_downstream.py` (or `python3 mock_downstream.py`). Then run:
   ```bash
   curl http://localhost:8080/proxy-retry
   ```
   Expected: `healthy`. Also try `/proxy`; if its circuit breaker was opened by earlier failures, wait at least **10 seconds** for the half-open probe and try again.
6. To inspect the retry failure case, stop the Python mock server and request `/proxy-retry` again. It attempts up to **three** downstream requests with a **200 ms** fixed interval, then returns a Spring error response rather than the circuit-breaker fallback. The deterministic **503, 503, 200** retry success scenario is asserted in `DownstreamHttpIntegrationTest`.
7. View the health endpoint at `http://localhost:8080/actuator/health`. Stop both processes with Ctrl+C.

**Configuration:** The downstream defaults to `http://localhost:8090`. Override it with `mvn spring-boot:run -Dspring-boot.run.arguments=--demo.downstream.url=http://localhost:8091` if your mock uses another port. Connect/read timeouts are 2/3 seconds. If port 8080 is busy, change `server.port` through Spring Boot arguments. Windows PowerShell users can use `curl.exe` instead of the `curl` alias to display raw responses.
