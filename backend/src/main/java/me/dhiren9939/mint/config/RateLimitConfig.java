package me.dhiren9939.mint.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.time.Duration;


@Configuration
public class RateLimitConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.ssl.enabled:false}")
    private boolean ssl;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${mint.redis.command-timeout-ms:500}")
    private long commandTimeoutMs;

    private RedisURI redisUri() {
        RedisURI.Builder uri = RedisURI.builder()
                .withHost(host)
                .withPort(port)
                .withSsl(ssl)
                .withTimeout(Duration.ofMillis(commandTimeoutMs));

        if (StringUtils.hasText(password)) {
            uri.withPassword(password.toCharArray());
        }
        return uri.build();
    }

    @Bean(destroyMethod = "shutdown")
    public RedisClient redisClient() {
        RedisClient client = RedisClient.create(redisUri());
        client.setOptions(ClientOptions.builder()
                .autoReconnect(true)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .socketOptions(SocketOptions.builder()
                        .connectTimeout(Duration.ofSeconds(2))
                        .keepAlive(SocketOptions.KeepAliveOptions.builder()
                                .enable()
                                .idle(Duration.ofSeconds(30))
                                .interval(Duration.ofSeconds(10))
                                .count(3)
                                .build())
                        .build())
                .timeoutOptions(TimeoutOptions.enabled(Duration.ofMillis(commandTimeoutMs)))
                .build());
        return client;
    }

    @Bean(destroyMethod = "close")
    public RedisProxyManagerProvider redisProxyManagerProvider(RedisClient redisClient) {
        return new RedisProxyManagerProvider(redisClient, redisUri());
    }
}
