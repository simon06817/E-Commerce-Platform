package com.example.project01.task;

import com.example.project01.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutScheduler {

    private final OrderService orderService;

    @Value("${app.order.expire-minutes:30}")
    private int expireMinutes;

    @Scheduled(
            initialDelayString = "${app.order.close-initial-delay-ms:60000}",
            fixedDelayString = "${app.order.close-scan-interval-ms:60000}")
    public void closeExpiredOrders() {
        int closed = orderService.closeExpiredOrders(expireMinutes);
        if (closed > 0) {
            log.info("closed {} expired unpaid orders", closed);
        }
    }
}
