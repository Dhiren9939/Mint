package me.dhiren9939.mint.config;

import me.dhiren9939.mint.common.RedisConnectionProvider;
import me.dhiren9939.mint.filter.RateLimitFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Servlet filters, in order: forwarded headers (Spring Boot, highest precedence),
 * CORS (dev only, so 429s still carry CORS headers), then rate limiting on the API.
 */
@Configuration
public class WebFilterConfig {

    @Bean
    @Profile("!prod")
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration(@Value("${allowed.cors.origin:}") String allowedOrigin) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of(allowedOrigin));
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);

        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(new CorsFilter(source));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }

    @Bean
    public RateLimitFilter rateLimitFilter(RedisConnectionProvider redisConnectionProvider,
                                           ObjectMapper objectMapper) {
        return new RateLimitFilter(redisConnectionProvider, objectMapper);
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter rateLimitFilter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(rateLimitFilter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
        return registration;
    }
}
