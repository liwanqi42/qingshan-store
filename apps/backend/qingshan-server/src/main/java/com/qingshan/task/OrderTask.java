package com.qingshan.task;

import com.qingshan.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单定时任务
 */
@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderService orderService;

    /**
     * 每分钟处理一次超时未支付的订单
     */
    @Scheduled(fixedDelay = 60 * 1000)
    public void cancelTimeoutOrders() {
        try {
            orderService.cancelTimeoutOrders();
        } catch (Exception e) {
            log.error("自动取消超时订单失败", e);
        }
    }
}
