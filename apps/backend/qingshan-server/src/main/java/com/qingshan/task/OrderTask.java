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
 * 功能：定时扫描并自动取消超时未支付订单
 * 多实例方案：使用Redis分布式锁控制，保证集群下任务只执行一次，防止重复处理
 */
@Component
@Slf4j
public class OrderTask {

    /**
     * Redis分布式锁Key：订单超时取消任务锁
     */
    private static final String LOCK_KEY = "task:order:cancel-timeout:lock";

    /**
     * 锁过期时间：5分钟，防止任务卡死导致锁永久占用
     */
    private static final Duration LOCK_TTL = Duration.ofMinutes(5);

    /**
     * Lua解锁脚本：原子校验锁持有者标识，匹配成功才删除锁，防止误删其他实例的锁
     * KEYS[1]：锁key
     * ARGV[1]：当前实例的唯一标识token
     * 返回 1=解锁成功，0=锁不属于当前持有者，不删除
     */
    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 定时任务：每分钟执行一次（fixedDelay：上一次执行完成后，延迟60s再执行下一次）
     * 流程：尝试获取分布式锁 -> 获取成功则执行订单超时取消逻辑 -> finally释放锁
     */
    @Scheduled(fixedDelay = 60 * 1000)
    public void cancelTimeoutOrders() {
        // 生成当前线程/实例唯一标识，用于解锁时校验，避免释放别人的锁
        String token = UUID.randomUUID().toString();
        Boolean locked;
        try {
            // setIfAbsent：SETNX，原子加锁，同时设置过期时间
            locked = stringRedisTemplate.opsForValue().setIfAbsent(LOCK_KEY, token, LOCK_TTL);
        } catch (Exception e) {
            log.error("获取订单超时任务锁失败，跳过本次执行", e);
            return;
        }

        // 未抢到锁，其他实例正在执行，直接跳过本次任务
        if (!Boolean.TRUE.equals(locked)) {
            log.debug("订单超时任务已由其他实例执行，跳过");
            return;
        }

        try {
            // 执行业务：取消超时未支付订单
            orderService.cancelTimeoutOrders();
        } catch (Exception e) {
            log.error("自动取消超时订单失败", e);
        } finally {
            // 无论业务成功失败，都尝试释放锁
            releaseLock(token);
        }
    }

    /**
     * 释放分布式锁
     * @param token 加锁时的唯一标识，用于lua脚本校验
     */
    private void releaseLock(String token) {
        try {
            stringRedisTemplate.execute(RELEASE_LOCK_SCRIPT, Collections.singletonList(LOCK_KEY), token);
        } catch (Exception e) {
            log.warn("释放订单超时任务锁失败", e);
        }
    }
}
