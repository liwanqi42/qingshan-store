package com.qingshan.service.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 店铺营业状态：统一读写 Redis，缓存缺失时按打烊处理
 */
@Component
@Slf4j
public class ShopStatusService {

    public static final String KEY = "SHOP_STATUS";

    /** 打烊中 */
    public static final int STATUS_CLOSED = 0;
    /** 营业中 */
    public static final int STATUS_OPEN = 1;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取营业状态，未设置时按打烊处理
     */
    public int getStatus() {
        Object value = redisTemplate.opsForValue().get(KEY);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException e) {
                log.warn("店铺营业状态值非法，按打烊处理：{}", value);
            }
        }
        return STATUS_CLOSED;
    }

    /**
     * 店铺是否营业中
     */
    public boolean isOpen() {
        return getStatus() == STATUS_OPEN;
    }

    /**
     * 设置营业状态
     */
    public void setStatus(int status) {
        redisTemplate.opsForValue().set(KEY, status);
    }
}
