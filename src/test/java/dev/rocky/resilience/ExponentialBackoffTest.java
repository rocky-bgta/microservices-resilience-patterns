package dev.rocky.resilience;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class ExponentialBackoffTest {
 @Test void fullJitterStaysWithinExponentialCap(){for(int attempt=0;attempt<12;attempt++){long upper=Math.min(1000,100L<<Math.min(attempt,10));for(int i=0;i<100;i++){long value=ExponentialBackoff.delay(attempt,Duration.ofMillis(100),Duration.ofMillis(1000)).toMillis();assertTrue(value>=0 && value<=upper);}}}
 @Test void zeroBaseProducesZero(){assertEquals(Duration.ZERO,ExponentialBackoff.delay(5,Duration.ZERO,Duration.ofSeconds(1)));}
 @Test void rejectsNegativeAttempt(){assertThrows(IllegalArgumentException.class,()->ExponentialBackoff.delay(-1,Duration.ofMillis(10),Duration.ofSeconds(1)));}
}
