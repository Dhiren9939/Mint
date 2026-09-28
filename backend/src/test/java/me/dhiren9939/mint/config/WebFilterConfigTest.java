package me.dhiren9939.mint.config;

import me.dhiren9939.mint.filter.RateLimitFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.web.filter.CorsFilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class WebFilterConfigTest {

    private final WebFilterConfig config = new WebFilterConfig();

    @Test
    @DisplayName("rate limiter only applies to the API, so health checks are never limited")
    void rateLimiter_onlyCoversApi() {
        FilterRegistrationBean<RateLimitFilter> registration =
                config.rateLimitFilterRegistration(mock(RateLimitFilter.class));

        assertEquals(1, registration.getUrlPatterns().size());
        assertTrue(registration.getUrlPatterns().contains("/api/*"));
    }

    @Test
    @DisplayName("CORS runs before the rate limiter so 429 responses carry CORS headers")
    void cors_runsBeforeRateLimiter() {
        FilterRegistrationBean<CorsFilter> cors = config.corsFilterRegistration("http://localhost:5173");
        FilterRegistrationBean<RateLimitFilter> rateLimit =
                config.rateLimitFilterRegistration(mock(RateLimitFilter.class));

        assertTrue(cors.getOrder() < rateLimit.getOrder());
    }
}
