package me.dhiren9939.mint.filter;

import io.lettuce.core.api.StatefulRedisConnection;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import me.dhiren9939.mint.common.RedisConnectionProvider;
import me.dhiren9939.mint.common.RedisRateLimiter;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private RedisConnectionProvider redisConnectionProvider;

    @Mock
    private RedisRateLimiter rateLimiter;

    @Mock
    private StatefulRedisConnection<String, byte[]> connection;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private FilterChain filterChain;

    private SimpleMeterRegistry meterRegistry;

    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        rateLimitFilter = new RateLimitFilter(redisConnectionProvider, objectMapper, meterRegistry, rateLimiter);
        ReflectionTestUtils.setField(rateLimitFilter, "profile", "dev");
        for (String field : new String[]{"globalGetCapacity", "globalPostCapacity", "ipGetCapacity",
                "ipPostCapacity", "userGetCapacity", "userPostCapacity"}) {
            ReflectionTestUtils.setField(rateLimitFilter, field, 10);
        }
    }

    @Test
    @DisplayName("fails open when Redis is unavailable")
    void redisUnavailable_allowsRequest() throws Exception {
        when(redisConnectionProvider.getConnection()).thenReturn(Optional.empty());
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

    @Test
    @DisplayName("passes the request on when the limiter allows it")
    void limiterAllows_passesRequest() throws Exception {
        when(redisConnectionProvider.getConnection()).thenReturn(Optional.of(connection));
        when(rateLimiter.check(eq(connection), any(), any(), any())).thenReturn(null);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/file/abc123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(1.0, meterRegistry.get("mint.ratelimit.decisions")
                .tag("result", "allowed").tag("limit", "none").counter().count());
    }

    @Test
    @DisplayName("answers 429 naming the bucket that rejected")
    void limiterRejects_answers429() throws Exception {
        when(redisConnectionProvider.getConnection()).thenReturn(Optional.of(connection));
        when(rateLimiter.check(eq(connection), any(), any(), any())).thenReturn("IP");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/file/abc123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitFilter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertEquals(429, response.getStatus());
        assertTrue(response.getContentAsString().contains("IP rate limit exceeded."));
        assertEquals(1.0, meterRegistry.get("mint.ratelimit.decisions")
                .tag("result", "rejected").tag("limit", "IP").counter().count());
    }

    @Test
    @DisplayName("fails open when the limiter throws")
    void limiterThrows_allowsRequest() throws Exception {
        when(redisConnectionProvider.getConnection()).thenReturn(Optional.of(connection));
        when(rateLimiter.check(eq(connection), any(), any(), any())).thenThrow(new RuntimeException("timeout"));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/file");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(1.0, meterRegistry.get("mint.ratelimit.decisions")
                .tag("result", "fail_open").tag("limit", "none").counter().count());
    }
}
