package com.example.project01.integration;

import com.example.project01.cache.CacheNames;
import com.example.project01.common.OutboxStatusEnum;
import com.example.project01.entity.OrderEventRecord;
import com.example.project01.entity.OrderNotification;
import com.example.project01.entity.OutboxMessage;
import com.example.project01.entity.Product;
import com.example.project01.mapper.ProductMapper;
import com.example.project01.service.OrderEventRecordService;
import com.example.project01.service.OrderNotificationService;
import com.example.project01.service.OrderService;
import com.example.project01.service.OutboxMessageService;
import com.example.project01.service.ProductService;
import com.example.project01.task.OrderCreatedEventListener;
import com.example.project01.task.OrderOutboxPublisher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("testcontainers")
class RealMiddlewareIntegrationTest {

    private static final int REDIS_PORT = 6379;

    @Container
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName("E-Commerce_Platform")
                    .withUsername("test")
                    .withPassword("test")
                    .withStartupTimeout(Duration.ofMinutes(3));

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7.4-alpine")
                    .withExposedPorts(REDIS_PORT);

    @Container
    static final RabbitMQContainer RABBITMQ =
            new RabbitMQContainer("rabbitmq:3.13-management")
                    .withStartupTimeout(Duration.ofMinutes(3));

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);

        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(REDIS_PORT));
        registry.add("spring.data.redis.username", () -> "");
        registry.add("spring.data.redis.password", () -> "");

        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", () -> RABBITMQ.getMappedPort(5672));
        registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private OutboxMessageService outboxMessageService;

    @Autowired
    private OrderOutboxPublisher outboxPublisher;

    @Autowired
    private OrderCreatedEventListener orderCreatedEventListener;

    @Autowired
    private OrderEventRecordService orderEventRecordService;

    @Autowired
    private OrderNotificationService notificationService;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void realRedisCacheAndMysqlCheckoutFlowAreUsed() throws Exception {
        String token = login("BUYER", "buyer01", "123456");
        int stockBefore = productMapper.selectById(1L).getStock();

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk());

        Cache cache = cacheManager.getCache(CacheNames.PRODUCT_DETAIL);
        assertNotNull(cache);
        assertNotNull(cache.get("1"));

        addToCart(token, 1, 1);
        long orderId = createOrder(token);
        assertTrue(orderId > 0);
        assertEquals(stockBefore - 1, productMapper.selectById(1L).getStock());
    }

    @Test
    void realMysqlShopSearchReturnsOnlyMatchingSellerProducts() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/products")
                        .param("keyword", "Tech Store")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode page = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        assertEquals(3, page.path("total").asInt());
        for (JsonNode product : page.path("records")) {
            assertEquals(1L, product.path("sellerId").asLong());
            assertEquals("Tech Store", product.path("sellerName").asText());
        }
    }

    @Test
    void realRabbitMqPublishesOutboxAndCreatesNotifications() throws Exception {
        String token = login("BUYER", "buyer01", "123456");
        addToCart(token, 1, 1);
        String createTraceId = "create-" + UUID.randomUUID();
        String paidTraceId = "paid-" + UUID.randomUUID();
        long orderId = createOrder(token, createTraceId);

        mockMvc.perform(put("/api/orders/" + orderId + "/pay")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Trace-Id", paidTraceId))
                .andExpect(status().isOk());

        outboxPublisher.publishPending();

        awaitCondition(() -> notificationService.lambdaQuery()
                .eq(OrderNotification::getOrderId, orderId)
                .count() >= 3);

        List<OutboxMessage> outboxMessages = outboxMessageService.lambdaQuery()
                .eq(OutboxMessage::getOrderId, orderId)
                .list();
        assertEquals(2, outboxMessages.size());
        assertTrue(outboxMessages.stream()
                .allMatch(message -> message.getStatus() == OutboxStatusEnum.SENT.getCode()));
        assertEquals(createTraceId, outboxMessages.stream()
                .filter(message -> "ORDER_CREATED".equals(message.getEventType()))
                .findFirst().orElseThrow().getTraceId());
        assertEquals(paidTraceId, outboxMessages.stream()
                .filter(message -> "ORDER_PAID".equals(message.getEventType()))
                .findFirst().orElseThrow().getTraceId());

        List<OrderEventRecord> eventRecords = orderEventRecordService.lambdaQuery()
                .eq(OrderEventRecord::getOrderId, orderId)
                .list();
        assertEquals(2, eventRecords.size());
        assertEquals(createTraceId, eventRecords.stream()
                .filter(record -> "ORDER_CREATED".equals(record.getEventType()))
                .findFirst().orElseThrow().getTraceId());
        assertEquals(paidTraceId, eventRecords.stream()
                .filter(record -> "ORDER_PAID".equals(record.getEventType()))
                .findFirst().orElseThrow().getTraceId());

        List<OrderNotification> notifications = notificationService.lambdaQuery()
                .eq(OrderNotification::getOrderId, orderId)
                .list();
        assertEquals(3, notifications.size());
        assertFalse(notifications.stream()
                .anyMatch(notification -> notification.getContent().isBlank()));

        OutboxMessage duplicate = outboxMessages.stream()
                .filter(message -> "ORDER_PAID".equals(message.getEventType()))
                .findFirst()
                .orElseThrow();
        orderCreatedEventListener.onOrderCreated(duplicate);
        orderCreatedEventListener.onOrderCreated(duplicate);

        assertEquals(2, orderEventRecordService.lambdaQuery()
                .eq(OrderEventRecord::getOrderId, orderId)
                .count());
        assertEquals(3, notificationService.lambdaQuery()
                .eq(OrderNotification::getOrderId, orderId)
                .count());
        Counter duplicateCounter = meterRegistry.find("ecommerce.events.consumed")
                .tag("event_type", "ORDER_PAID")
                .tag("outcome", "duplicate")
                .counter();
        assertNotNull(duplicateCounter);
        assertTrue(duplicateCounter.count() >= 1);
    }

    @Test
    void concurrentStockDecreaseAllowsOnlyOneSuccess() throws Exception {
        Product original = productMapper.selectById(1L);
        int originalStock = original.getStock();
        Product update = new Product();
        update.setId(1L);
        update.setStock(1);
        productMapper.updateById(update);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Callable<Boolean> decrease = () -> {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                try {
                    productService.decreaseStock(1L, 1);
                    return true;
                } catch (Exception ignored) {
                    return false;
                }
            };
            Future<Boolean> first = executor.submit(decrease);
            Future<Boolean> second = executor.submit(decrease);
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            int success = (first.get(10, TimeUnit.SECONDS) ? 1 : 0)
                    + (second.get(10, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, success);
            assertEquals(0, productMapper.selectById(1L).getStock());
        } finally {
            executor.shutdownNow();
            update.setStock(originalStock);
            productMapper.updateById(update);
        }
    }

    @Test
    void concurrentShipOrderAllowsOnlyOneTransition() throws Exception {
        String buyerToken = login("BUYER", "buyer01", "123456");
        addToCart(buyerToken, 1, 1);
        long orderId = createOrder(buyerToken);
        mockMvc.perform(put("/api/orders/" + orderId + "/pay")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Callable<Boolean> ship = () -> {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                try {
                    orderService.shipOrder(1L, orderId);
                    return true;
                } catch (Exception ignored) {
                    return false;
                }
            };
            Future<Boolean> first = executor.submit(ship);
            Future<Boolean> second = executor.submit(ship);
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            int success = (first.get(10, TimeUnit.SECONDS) ? 1 : 0)
                    + (second.get(10, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, success);
            assertEquals(2, orderService.getById(orderId).getStatus());
        } finally {
            executor.shutdownNow();
        }
    }

    private String login(String role, String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"" + role + "\",\"username\":\"" + username
                                + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(200, body.path("code").asInt());
        return body.path("data").path("token").asText();
    }

    private void addToCart(String token, int productId, int num) throws Exception {
        mockMvc.perform(post("/api/carts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"num\":" + num + "}"))
                .andExpect(status().isOk());
    }

    private long createOrder(String token) throws Exception {
        return createOrder(token, null);
    }

    private long createOrder(String token, String traceId) throws Exception {
        var request = post("/api/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotencyKey\":\"" + UUID.randomUUID()
                        + "\",\"receiverName\":\"Tom\",\"receiverPhone\":\"13800000000\","
                        + "\"receiverAddress\":\"Beijing\"}");
        if (traceId != null) {
            request.header("X-Trace-Id", traceId);
        }
        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(200, body.path("code").asInt());
        JsonNode payload = body.path("data");
        return (payload.isArray() ? payload.get(0) : payload).path("id").asLong();
    }

    private void awaitCondition(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(200);
        }
        fail("condition was not satisfied before timeout");
    }
}
