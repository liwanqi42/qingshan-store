package com.qingshan.service.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 商品缓存维护：用户端商品列表/分页缓存以 product_ 为前缀，
 * 商品变更、库存或销量变化后需要整体失效，避免用户端读到旧数据
 */
@Component
@Slf4j
public class ProductCacheManager {

    private static final String PRODUCT_CACHE_PATTERN = "product_*";

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 清理商品相关缓存（含分类商品列表与分页缓存）
     */
    public void clearProductCache() {
        try {
            Set keys = redisTemplate.keys(PRODUCT_CACHE_PATTERN);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            // 缓存清理失败不影响主业务，最多由缓存 TTL 兜底过期
            log.warn("清理商品缓存失败", e);
        }
    }
}
