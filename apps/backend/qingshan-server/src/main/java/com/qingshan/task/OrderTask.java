package com.qingshan.task;

import com.qingshan.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/**
 * 订单定时任务
 */
@Component
@Slf4j
public class OrderTask {

    private static final String LOCK_KEY = "task:order:cancel-timeout:lock";
    private static final Duration LOCK_TTL = Duration.ofMinutes(5);
    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 每分钟处理一次超时未支付的订单。
     * 多实例部署时通过 Redis 锁保证同一时刻只有一个实例执行。
     */
    @Scheduled(fixedDelay = 60 * 1000)
    public void cancelTimeoutOrders() {
        String token = UUID.randomUUID().toString();
        Boolean locked;
        try {
            locked = stringRedisTemplate.opsForValue().setIfAbsent(LOCK_KEY, token, LOCK_TTL);
        } catch (Exception e) {
            log.error("获取订单超时任务锁失败，跳过本次执行", e);
            return;
        }

        if (!Boolean.TRUE.equals(locked)) {
            log.debug("订单超时任务已由其他实例执行，跳过");
            return;
        }

        try {
            orderService.cancelTimeoutOrders();
        } catch (Exception e) {
            log.error("自动取消超时订单失败", e);
        } finally {
            releaseLock(token);
        }
    }

    private void releaseLock(String token) {
        try {
            stringRedisTemplate.execute(RELEASE_LOCK_SCRIPT, Collections.singletonList(LOCK_KEY), token);
        } catch (Exception e) {
            log.warn("释放订单超时任务锁失败", e);
        }
    }
}
