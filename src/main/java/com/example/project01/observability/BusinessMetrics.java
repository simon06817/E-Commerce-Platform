package com.example.project01.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Low-cardinality business metrics for order, stock, cache and async delivery.
 */
@Component
public class BusinessMetrics {

    private final MeterRegistry registry;
    private final AtomicInteger outboxPending = new AtomicInteger();
    private final AtomicLong outboxOldestAgeSeconds = new AtomicLong();

    public BusinessMetrics(MeterRegistry registry) {
        this.registry = registry;
        Gauge.builder("ecommerce.outbox.pending", outboxPending, AtomicInteger::get)
                .description("Current number of pending outbox messages")
                .register(registry);
        Gauge.builder("ecommerce.outbox.oldest_pending_seconds",
                        outboxOldestAgeSeconds, AtomicLong::get)
                .description("Age in seconds of the oldest pending outbox message")
                .register(registry);
    }

    public void recordOrderCreated(int count) {
        if (count > 0) {
            counter("ecommerce.orders.checkout").increment(count);
        }
    }

    public void recordOrderCreateRejected(String reason) {
        counter("ecommerce.orders.create.rejected", "reason", reason).increment();
    }

    public void recordOrderTransition(String action, boolean success) {
        counter("ecommerce.order.transitions",
                "action", action,
                "outcome", outcome(success)).increment();
    }

    public void recordStockChange(String operation, boolean success) {
        counter("ecommerce.stock.changes",
                "operation", operation,
                "outcome", outcome(success)).increment();
    }

    public void recordCacheAccess(String cacheName, String result) {
        counter("ecommerce.cache.access",
                "cache", cacheName,
                "result", result).increment();
    }

    public void recordOutboxPublish(String eventType, boolean success) {
        counter("ecommerce.outbox.publish",
                "event_type", eventType,
                "outcome", outcome(success)).increment();
    }

    public void recordEventConsumed(String eventType, String outcome) {
        counter("ecommerce.events.consumed",
                "event_type", eventType,
                "outcome", outcome).increment();
    }

    public void updateOutboxBacklog(long pending, long oldestAgeSeconds) {
        outboxPending.set(Math.toIntExact(Math.min(Integer.MAX_VALUE, Math.max(0, pending))));
        outboxOldestAgeSeconds.set(Math.max(0, oldestAgeSeconds));
    }

    private Counter counter(String name, String... tags) {
        return Counter.builder(name).tags(tags).register(registry);
    }

    private String outcome(boolean success) {
        return success ? "success" : "failure";
    }
}
