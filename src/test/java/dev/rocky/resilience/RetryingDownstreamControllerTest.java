package dev.rocky.resilience;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RetryingDownstreamControllerTest {
    @Test
    void delegatesToRetryingService() {
        RetryingDownstreamService service = mock(RetryingDownstreamService.class);
        when(service.fetchHealth()).thenReturn("ok");
        assertEquals("ok", new RetryingDownstreamController(service).proxyWithRetry());
        verify(service).fetchHealth();
    }
}
