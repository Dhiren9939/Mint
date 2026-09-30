package me.dhiren9939.mint.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Exercises {@link MetricsConfig}'s allowlist filter against a plain
 * {@link SimpleMeterRegistry} (no CloudWatch/Spring context needed) so the filter's
 * logic can be checked in isolation.
 */
class MetricsConfigTest {

    private SimpleMeterRegistry registry() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        registry.config().meterFilter(MetricsConfig.allowlistFilter());
        return registry;
    }

    @Test
    @DisplayName("allows http.server.requests")
    void allowsHttpServerRequests() {
        SimpleMeterRegistry registry = registry();
        Timer.builder("http.server.requests").register(registry).record(() -> { });
        assertNotNull(registry.find("http.server.requests").timer());
    }

    @Test
    @DisplayName("allows tomcat.threads.busy and tomcat.connections.* by prefix")
    void allowsTomcatMetrics() {
        SimpleMeterRegistry registry = registry();
        Gauge.builder("tomcat.threads.busy", () -> 1).register(registry);
        Gauge.builder("tomcat.connections.current", () -> 1).register(registry);
        assertNotNull(registry.find("tomcat.threads.busy").gauge());
        assertNotNull(registry.find("tomcat.connections.current").gauge());
    }

    @Test
    @DisplayName("allows any mint.* meter by prefix")
    void allowsMintPrefixedMetrics() {
        SimpleMeterRegistry registry = registry();
        Counter.builder("mint.ratelimit.decisions").register(registry).increment();
        assertNotNull(registry.find("mint.ratelimit.decisions").counter());
    }

    @Test
    @DisplayName("allows jvm.memory.used only for the heap area, denies non-heap")
    void allowsJvmMemoryUsedHeapOnly() {
        SimpleMeterRegistry registry = registry();
        Gauge.builder("jvm.memory.used", () -> 1).tag("area", "heap").register(registry);
        Gauge.builder("jvm.memory.used", () -> 1).tag("area", "nonheap").register(registry);

        assertNotNull(registry.find("jvm.memory.used").tag("area", "heap").gauge());
        assertNull(registry.find("jvm.memory.used").tag("area", "nonheap").gauge());
    }

    @Test
    @DisplayName("denies meters not on the allowlist, e.g. jvm.memory.committed and arbitrary names")
    void deniesEverythingElse() {
        SimpleMeterRegistry registry = registry();
        Gauge.builder("jvm.memory.committed", () -> 1).register(registry);
        Counter.builder("random.other.meter").register(registry).increment();

        assertNull(registry.find("jvm.memory.committed").gauge());
        assertNull(registry.find("random.other.meter").counter());
    }
}
