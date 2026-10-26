package dev.rocky.resilience;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/** Full-jitter backoff. Retry only operations known to be safe or idempotent. */
public final class ExponentialBackoff {
    private ExponentialBackoff() {}

    public static Duration delay(int attempt, Duration base, Duration cap) {
        if (attempt < 0 || base.isNegative() || cap.isNegative()) throw new IllegalArgumentException();
        long max = Math.min(cap.toMillis(), saturatingMultiply(base.toMillis(), attempt));
        return Duration.ofMillis(max == 0 ? 0 : ThreadLocalRandom.current().nextLong(max + 1));
    }

    private static long saturatingMultiply(long value, int attempt) {
        if (attempt >= 62 || value > (Long.MAX_VALUE >> attempt)) return Long.MAX_VALUE;
        return value << attempt;
    }
}
