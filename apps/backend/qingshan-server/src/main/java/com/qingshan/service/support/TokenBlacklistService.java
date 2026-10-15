package com.qingshan.service.support;

import com.qingshan.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * JWT 黑名单：退出登录后把 token 的 jti 写入 Redis，保留到 token 自然过期。
 */
@Component
@Slf4j
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "auth:token:blacklist:";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 拉黑 token；已过期或无法解析时直接忽略
     */
    public void revoke(String token, String secretKey) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        try {
            Claims claims = JwtUtil.parseJWT(secretKey, token);
            String jti = claims.getId();
            Date expiration = claims.getExpiration();
            if (!StringUtils.hasText(jti) || expiration == null) {
                return;
            }
            long ttl = expiration.getTime() - System.currentTimeMillis();
            if (ttl > 0) {
                stringRedisTemplate.opsForValue()
                        .set(KEY_PREFIX + jti, "1", ttl, TimeUnit.MILLISECONDS);
            }
        } catch (ExpiredJwtException e) {
            // token 已过期，无需拉黑
        } catch (Exception e) {
            log.warn("注销 token 失败", e);
        }
    }

    /**
     * 是否已拉黑。无 jti 的旧 token 一律拒绝，强制重新登录。
     */
    public boolean isRevoked(String jti) {
        if (!StringUtils.hasText(jti)) {
            return true;
        }
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(KEY_PREFIX + jti));
        } catch (Exception e) {
            // Redis 不可用时按未拉黑处理，避免影响正常使用；
            // 改密、禁用员工仍会通过 tokenVersion 失效
            log.warn("检查 token 黑名单失败，按未拉黑处理", e);
            return false;
        }
    }
}
