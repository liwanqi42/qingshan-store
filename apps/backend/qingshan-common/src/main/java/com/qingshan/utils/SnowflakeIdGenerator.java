package com.qingshan.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 雪花算法 ID 生成器。
 * 64 位组成：1 位符号位 + 41 位时间戳 + 10 位机器号 + 12 位序列号。
 * 单节点每毫秒最多生成 4096 个 ID，最多支持 1024 个节点。
 */
@Component
public class SnowflakeIdGenerator {

    /**
     * 起始时间戳（2024-01-01 00:00:00 UTC），41 位时间戳可用约 69 年
     */
    private static final long EPOCH = 1704067200000L;

    private static final long WORKER_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    /**
     * 允许的时钟回拨毫秒数
     */
    private static final long MAX_BACKWARD_MS = 5L;

    private final long workerId;

    private long lastTimestamp = -1L;
    private long sequence = 0L;

    public SnowflakeIdGenerator(@Value("${qingshan.snowflake.worker-id:1}") long workerId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("workerId 必须在 0 ~ " + MAX_WORKER_ID + " 之间");
        }
        this.workerId = workerId;
    }

    /**
     * 生成下一个 ID
     */
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();

        // 处理时钟回拨
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            if (offset > MAX_BACKWARD_MS) {
                throw new IllegalStateException("系统时钟回拨 " + offset + "ms，拒绝生成 ID");
            }
            try {
                Thread.sleep(offset + 1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("生成 ID 时线程被中断", e);
            }
            timestamp = System.currentTimeMillis();
            if (timestamp < lastTimestamp) {
                throw new IllegalStateException("系统时钟仍在回拨，拒绝生成 ID");
            }
        }

        if (timestamp == lastTimestamp) {
            // 同一毫秒内自增序列号，超过 4095 后等待下一毫秒
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 生成下一个 ID 的字符串形式，用于订单号等 varchar 字段
     */
    public String nextIdStr() {
        return Long.toString(nextId());
    }

    private long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
