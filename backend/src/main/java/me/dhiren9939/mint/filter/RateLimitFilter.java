package me.dhiren9939.mint.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.dhiren9939.mint.common.ApiError;
import me.dhiren9939.mint.common.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {

    // Per Month
    @Value("${mint.cap.global.get:5000}")
    private Integer globalGetCapacity;
    @Value("${mint.cap.global.post:1000}")
    private Integer globalPostCapacity;

    // Per Minute
    @Value("${mint.cap.ip.get:20}")
    private Integer ipGetCapacity;
    @Value("${mint.cap.ip.post:5}")
    private Integer ipPostCapacity;

    // Per Day
    @Value("${mint.cap.user.get:20}")
    private Integer userGetCapacity;
    @Value("${mint.cap.user.post:10}")
    private Integer userPostCapacity;

    @Value("${spring.profiles.active:dev}")
    private String profile;

    private final ProxyManager<String> proxyManager;
    private final ObjectMapper objectMapper;
    private final CorsConfigurationSource corsConfigurationSource;

    @Override
    public void doFilterInternal(HttpServletRequest request,
                                 HttpServletResponse response,
                                 FilterChain filterChain) throws ServletException, IOException {

        // Add Access-Control headers according to CORS config
        CorsConfiguration corsConfig = corsConfigurationSource.getCorsConfiguration(request);
        if (corsConfig != null) {
            String origin = request.getHeader("Origin");
            if (origin != null && corsConfig.checkOrigin(origin) != null) {
                response.setHeader("Access-Control-Allow-Headers", "Content-Type");
                response.setHeader("Access-Control-Allow-Origin", origin);
                response.setHeader("Access-Control-Allow-Credentials",
                        String.valueOf(Boolean.TRUE.equals(corsConfig.getAllowCredentials())));
            }
        }

        String method = request.getMethod().toUpperCase();
        boolean isGet = method.equals("GET");

        String ip = request.getRemoteAddr();
        String userId = getOrSetCookie(request, response);

        String globalKey = keyBuilder("GLOBAL", method, "");
        String ipKey = keyBuilder("IP", method, ip);
        String userKey = keyBuilder("USER", method, userId);

        Bucket globalBucket = isGet ? getBucket(globalKey, globalGetCapacity, Duration.ofDays(30)) :
                getBucket(globalKey, globalPostCapacity, Duration.ofDays(30));

        Bucket ipBucket = isGet ? getBucket(ipKey, ipGetCapacity, Duration.ofMinutes(1)) :
                getBucket(ipKey, ipPostCapacity, Duration.ofMinutes(1));

        Bucket userBucket = isGet ? getBucket(userKey, userGetCapacity, Duration.ofDays(1)) :
                getBucket(userKey, userPostCapacity, Duration.ofDays(1));

        if (!globalBucket.tryConsume(1)) {
            rejectRequest(response, "Global");
            return;
        }

        if (!ipBucket.tryConsume(1)) {
            globalBucket.addTokens(1);
            rejectRequest(response, "IP");
            return;
        }

        if (!userBucket.tryConsume(1)) {
            globalBucket.addTokens(1);
            ipBucket.addTokens(1);
            rejectRequest(response, "User");
            return;
        }

        doFilter(request, response, filterChain);
    }

    private String keyBuilder(String type, String method, String id) {
        return type + "_" + method + ":" + id;
    }

    private Bucket getBucket(String key, int capacity, Duration duration) {
        BucketConfiguration configuration = BucketConfiguration
                .builder()
                .addLimit(Bandwidth
                        .builder()
                        .capacity(capacity)
                        .refillGreedy(capacity, duration)
                        .build())
                .build();

        return proxyManager.builder().build(key, () -> configuration);
    }

    private void rejectRequest(HttpServletResponse response, String limitedBy) throws IOException {
        ApiError<Object> error = ApiError.of(429, "RATE_LIMIT_EXCEEDED", limitedBy + " rate limit exceeded.");
        ApiResponse<?> apiResponse = ApiResponse.fail(error);
        String json = objectMapper.writeValueAsString(apiResponse);

        response.setStatus(429);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        response.setContentLength(bytes.length);

        response.getOutputStream().write(bytes);
        response.getOutputStream().flush();
    }

    private String getOrSetCookie(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            Optional<Cookie> userCookie = Arrays.stream(cookies)
                    .filter(c -> c.getName().equals("MINT_ID"))
                    .findFirst();
            if (userCookie.isPresent()) {
                return userCookie.get().getValue();
            }
        }

        String userId = UUID.randomUUID().toString();
        Cookie mintCookie = new Cookie("MINT_ID", userId);
        mintCookie.setHttpOnly(true);
        mintCookie.setSecure(profile.equals("prod"));
        mintCookie.setAttribute("SameSite", profile.equals("prod") ? "Strict" : "Lax");
        mintCookie.setPath("/");
        mintCookie.setMaxAge(60 * 60 * 24 * 30);
        response.addCookie(mintCookie);
        return userId;
    }
}
