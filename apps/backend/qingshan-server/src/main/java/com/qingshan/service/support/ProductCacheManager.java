package com.qingshan.service.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 商品缓存维护：用版本号实现整体失效，避免使用 KEYS 扫描。
 * 用户端商品列表/分页 key 都带上当前版本号，商品变更后递增版本号，
 * 旧 key 不再被读取，由 TTL 自然过期。
 */
@Component
@Slf4j
public class ProductCacheManager {

    private static final String CACHE_VERSION_KEY = "product:cache:version";
    private static final String INITIAL_VERSION = "1";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 当前缓存版本，缓存缺失时初始化为 1
     */
    public String currentVersion() {
        try {
            String version = stringRedisTemplate.opsForValue().get(CACHE_VERSION_KEY);
            if (version == null) {
                stringRedisTemplate.opsForValue().setIfAbsent(CACHE_VERSION_KEY, INITIAL_VERSION);
                version = stringRedisTemplate.opsForValue().get(CACHE_VERSION_KEY);
            }
            return version == null ? INITIAL_VERSION : version;
        } catch (Exception e) {
            // Redis 不可用时让调用方回源数据库，避免业务被缓存拖垮
            log.warn("读取商品缓存版本失败，本次跳过缓存", e);
            return "0";
        }
    }

    /**
     * 清理商品相关缓存（含分类商品列表与分页缓存）
     */
    public void clearProductCache() {
        try {
            stringRedisTemplate.opsForValue().increment(CACHE_VERSION_KEY);
        } catch (Exception e) {
            // 缓存清理失败不影响主业务，最多由缓存 TTL 兜底过期
            log.warn("清理商品缓存失败", e);
        }
    }
}
