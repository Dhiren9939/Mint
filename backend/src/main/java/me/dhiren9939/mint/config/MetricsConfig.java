package me.dhiren9939.mint.config;

import io.micrometer.cloudwatch2.CloudWatchConfig;
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Publishes a curated set of app metrics to CloudWatch under the "Mint" namespace. Only
 * active when {@code mint.metrics.cloudwatch.enabled=true} (prod), so dev/test never pay
 * for or attempt a CloudWatch connection.
 * <p>
 * Everything not on {@link #METRICS_ALLOWLIST_FILTER} is dropped before it reaches
 * CloudWatch, keeping cost and cardinality bounded to what's actually dashboarded/alarmed on.
 */
@Configuration
@ConditionalOnProperty(name = "mint.metrics.cloudwatch.enabled", havingValue = "true")
public class MetricsConfig {

    private static final Set<String> ALLOWLIST_EXACT = Set.of(
            "http.server.requests",
            "tomcat.threads.busy",
            "tomcat.threads.config.max",
            "jvm.gc.pause",
            "jvm.threads.live",
            "process.cpu.usage",
            "system.cpu.usage"
    );

    private static final List<String> ALLOWLIST_PREFIXES = List.of(
            "tomcat.connections.",
            "hikaricp.connections.",
            "mint."
    );

    private static final List<String> PERCENTILE_EXACT = List.of("http.server.requests");
    private static final String PERCENTILE_PREFIX = "mint.";

    /**
     * Denies every meter except the allowlisted ones (by exact name or prefix), plus
     * jvm.memory.used/max restricted to the heap area. Extracted as a static method so it can
     * be unit tested directly against a {@link io.micrometer.core.instrument.simple.SimpleMeterRegistry}.
     */
    static MeterFilter allowlistFilter() {
        return MeterFilter.denyUnless(MetricsConfig::isAllowed);
    }

    private static boolean isAllowed(Meter.Id id) {
        String name = id.getName();

        if (ALLOWLIST_EXACT.contains(name)) {
            return true;
        }
        for (String prefix : ALLOWLIST_PREFIXES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        if (name.equals("jvm.memory.used") || name.equals("jvm.memory.max")) {
            return "heap".equals(id.getTag("area"));
        }
        return false;
    }

    static MeterFilter percentileFilter() {
        return new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                String name = id.getName();
                if (PERCENTILE_EXACT.contains(name) || name.startsWith(PERCENTILE_PREFIX)) {
                    return DistributionStatisticConfig.builder()
                            .percentiles(0.5, 0.95, 0.99)
                            .build()
                            .merge(config);
                }
                return config;
            }
        };
    }

    @Value("${aws.bucket.region:ap-south-1}")
    private String region;

    @Value("${mint.env:unknown}")
    private String env;

    @Bean(destroyMethod = "close")
    public CloudWatchAsyncClient cloudWatchAsyncClient() {
        return CloudWatchAsyncClient.builder()
                .region(Region.of(region))
                .build();
    }

    @Bean
    public CloudWatchConfig cloudWatchConfig() {
        Map<String, String> props = Map.of(
                "cloudwatch.namespace", "Mint",
                "cloudwatch.step", Duration.ofMinutes(1).toString()
        );
        return props::get;
    }

    @Bean(destroyMethod = "close")
    public CloudWatchMeterRegistry cloudWatchMeterRegistry(CloudWatchConfig cloudWatchConfig,
                                                            CloudWatchAsyncClient cloudWatchAsyncClient) {
        CloudWatchMeterRegistry registry = new CloudWatchMeterRegistry(cloudWatchConfig, Clock.SYSTEM, cloudWatchAsyncClient);
        registry.config()
                .commonTags("env", env)
                .meterFilter(allowlistFilter())
                .meterFilter(percentileFilter());
        return registry;
    }
}
