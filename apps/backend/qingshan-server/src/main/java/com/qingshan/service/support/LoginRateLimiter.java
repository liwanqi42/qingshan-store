package com.qingshan.service.support;

import com.qingshan.exception.LoginFailedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * 登录失败限流：按账号和 IP 分别计数，超过阈值后锁定一段时间
 *
 * Redis 异常时选择放行（fail-open），避免缓存故障导致所有用户无法登录
 */
@Component
@Slf4j
public class LoginRateLimiter {

    private static final String KEY_PREFIX = "login:fail:";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${qingshan.security.login-max-failures:5}")
    private long maxFailures;

    @Value("${qingshan.security.login-ip-max-failures:20}")
    private long ipMaxFailures;

    @Value("${qingshan.security.login-lock-minutes:15}")
    private long lockMinutes;

    /**
     * 登录前检查账号或 IP 是否已被锁定
     */
    public void checkAllowed(String scene, String account, String ip) {
        if (isBlocked(accountKey(scene, account), maxFailures)
                || isBlocked(ipKey(scene, ip), ipMaxFailures)) {
            throw new LoginFailedException("操作过于频繁，请" + lockMinutes + "分钟后再试");
        }
    }

    /**
     * 记录一次失败
     */
    public void recordFailure(String scene, String account, String ip) {
        recordAttempt(scene, account, ip);
    }

    /**
     * 记录一次尝试，用于注册这类成功也需要计数的场景
     */
    public void recordAttempt(String scene, String account, String ip) {
        increase(accountKey(scene, account));
        increase(ipKey(scene, ip));
    }

    /**
     * 登录成功后清除该账号的失败计数，IP 计数保留，避免同一 IP 换账号继续撞库
     */
    public void recordSuccess(String scene, String account) {
        String key = accountKey(scene, account);
        if (key == null) {
            return;
        }
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("清理登录失败计数失败，key={}", key, e);
        }
    }

    private boolean isBlocked(String key, long threshold) {
        if (key == null) {
            return false;
        }
        try {
            String value = stringRedisTemplate.opsForValue().get(key);
            return value != null && Long.parseLong(value) >= threshold;
        } catch (Exception e) {
            log.warn("登录限流检查失败，key={}", key, e);
            return false;
        }
    }

    private void increase(String key) {
        if (key == null) {
            return;
        }
        try {
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                stringRedisTemplate.expire(key, Duration.ofMinutes(lockMinutes));
            }
        } catch (Exception e) {
            log.warn("登录失败计数失败，key={}", key, e);
        }
    }

    private String accountKey(String scene, String account) {
        return StringUtils.hasText(account)
                ? KEY_PREFIX + scene + ":account:" + account.trim()
                : null;
    }

    private String ipKey(String scene, String ip) {
        return StringUtils.hasText(ip)
                ? KEY_PREFIX + scene + ":ip:" + ip
                : null;
    }
}
