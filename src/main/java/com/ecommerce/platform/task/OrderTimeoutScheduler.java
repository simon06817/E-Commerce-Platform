package com.ecommerce.platform.task;

import com.ecommerce.platform.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 订单超时定时任务。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutScheduler {

    private final OrderService orderService;

    @Value("${app.order.expire-minutes:30}")
    private int expireMinutes;

    @Value("${app.order.auto-complete-hours:24}")
    private int autoCompleteHours;

    /** 关闭超时未支付订单。 */
    @Scheduled(
            initialDelayString = "${app.order.close-initial-delay-ms:60000}",
            fixedDelayString = "${app.order.close-scan-interval-ms:60000}")
    public void closeExpiredOrders() {
        int closed = orderService.closeExpiredOrders(expireMinutes);
        if (closed > 0) {
            log.info("closed {} expired unpaid orders", closed);
        }
    }

    /** 自动完成已发货订单。 */
    @Scheduled(
            initialDelayString = "${app.order.auto-complete-initial-delay-ms:60000}",
            fixedDelayString = "${app.order.auto-complete-scan-interval-ms:60000}")
    public void autoCompleteShippedOrders() {
        int completed = orderService.autoCompleteShippedOrders(autoCompleteHours);
        if (completed > 0) {
            log.info("automatically completed {} delivered orders", completed);
        }
    }
}
