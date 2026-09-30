package me.dhiren9939.mint.filter;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import me.dhiren9939.mint.common.RedisConnectionProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private RedisConnectionProvider redisConnectionProvider;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private FilterChain filterChain;

    private SimpleMeterRegistry meterRegistry;

    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        rateLimitFilter = new RateLimitFilter(redisConnectionProvider, objectMapper, meterRegistry);
        ReflectionTestUtils.setField(rateLimitFilter, "profile", "dev");
    }

    @Test
    @DisplayName("fails open when Redis is unavailable")
    void redisUnavailable_allowsRequest() throws Exception {
        when(redisConnectionProvider.getProxyManager()).thenReturn(Optional.empty());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/file/abc123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
        assertEquals(1.0, meterRegistry.get("mint.ratelimit.decisions")
                .tag("result", "fail_open")
                .tag("limit", "none")
                .counter()
                .count());
        assertEquals(1, meterRegistry.get("mint.ratelimit.duration").timer().count());
    }
}
