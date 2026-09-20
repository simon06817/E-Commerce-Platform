package com.example.project01.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BusinessMetricsTest {

    @Test
    void recordsBusinessCountersAndBacklogGauges() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BusinessMetrics metrics = new BusinessMetrics(registry);

        metrics.recordOrderCreated(2);
        metrics.recordOrderCreateRejected("duplicate");
        metrics.recordStockChange("decrease", false);
        metrics.recordCacheAccess("productDetail", "hit");
        metrics.recordOutboxPublish("ORDER_PAID", true);
        metrics.recordEventConsumed("ORDER_PAID", "duplicate");
        metrics.updateOutboxBacklog(7, 42);

        assertEquals(2.0, registry.get("ecommerce.orders.checkout").counter().count());
        assertEquals(1.0, registry.get("ecommerce.orders.create.rejected")
                .tag("reason", "duplicate").counter().count());
        assertEquals(1.0, registry.get("ecommerce.stock.changes")
                .tag("operation", "decrease").tag("outcome", "failure").counter().count());
        assertEquals(1.0, registry.get("ecommerce.cache.access")
                .tag("cache", "productDetail").tag("result", "hit").counter().count());
        assertEquals(1.0, registry.get("ecommerce.outbox.publish")
                .tag("event_type", "ORDER_PAID").tag("outcome", "success").counter().count());
        assertEquals(1.0, registry.get("ecommerce.events.consumed")
                .tag("event_type", "ORDER_PAID").tag("outcome", "duplicate").counter().count());
        assertEquals(7.0, registry.get("ecommerce.outbox.pending").gauge().value());
        assertEquals(42.0, registry.get("ecommerce.outbox.oldest_pending_seconds")
                .gauge().value());
    }
}
