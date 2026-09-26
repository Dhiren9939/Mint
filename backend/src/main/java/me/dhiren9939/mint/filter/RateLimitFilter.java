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
import me.dhiren9939.mint.config.RedisProxyManagerProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
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

    private final RedisProxyManagerProvider proxyManagerProvider;
    private final ObjectMapper objectMapper;

    @Override
    public void doFilterInternal(HttpServletRequest request,
                                 HttpServletResponse response,
                                 FilterChain filterChain) throws ServletException, IOException {

        Optional<ProxyManager<String>> proxyManager = proxyManagerProvider.get();
        if (proxyManager.isEmpty()) {
            // Fail open, no rate limiting at all while the cache is unavailable
            filterChain.doFilter(request, response);
            return;
        }

        String rejectedBy;
        try {
            rejectedBy = consumeLimits(proxyManager.get(), request, response);
        } catch (RuntimeException e) {
            // Fail open, a cache outage must not take the API down
            log.warn("Rate limiter unavailable, allowing request: {}", e.toString());
            filterChain.doFilter(request, response);
            return;
        }

        if (rejectedBy != null) {
            rejectRequest(response, rejectedBy);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * @return the name of the limit that rejected the request, or null if it is allowed
     */
    private String consumeLimits(ProxyManager<String> proxyManager,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        String method = request.getMethod().toUpperCase();
        boolean isGet = method.equals("GET");

        String ip = request.getRemoteAddr();
        String userId = getOrSetCookie(request, response);

        String globalKey = keyBuilder("GLOBAL", method, "");
        String ipKey = keyBuilder("IP", method, ip);
        String userKey = keyBuilder("USER", method, userId);

        Bucket globalBucket = isGet ? getBucket(proxyManager, globalKey, globalGetCapacity, Duration.ofDays(30)) :
                getBucket(proxyManager, globalKey, globalPostCapacity, Duration.ofDays(30));

        Bucket ipBucket = isGet ? getBucket(proxyManager, ipKey, ipGetCapacity, Duration.ofMinutes(1)) :
                getBucket(proxyManager, ipKey, ipPostCapacity, Duration.ofMinutes(1));

        Bucket userBucket = isGet ? getBucket(proxyManager, userKey, userGetCapacity, Duration.ofDays(1)) :
                getBucket(proxyManager, userKey, userPostCapacity, Duration.ofDays(1));

        if (!globalBucket.tryConsume(1)) {
            return "Global";
        }

        if (!ipBucket.tryConsume(1)) {
            globalBucket.addTokens(1);
            return "IP";
        }

        if (!userBucket.tryConsume(1)) {
            globalBucket.addTokens(1);
            ipBucket.addTokens(1);
            return "User";
        }

        return null;
    }

    private String keyBuilder(String type, String method, String id) {
        return type + "_" + method + ":" + id;
    }

    private Bucket getBucket(ProxyManager<String> proxyManager, String key, int capacity, Duration duration) {
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
