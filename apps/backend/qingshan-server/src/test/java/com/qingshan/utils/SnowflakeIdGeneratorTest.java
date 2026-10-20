package com.qingshan.utils;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnowflakeIdGeneratorTest {

    @Test
    void shouldGenerateUniqueIdsInParallel() throws Exception {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1);
        int threadCount = 8;
        int idsPerThread = 10_000;
        Set<Long> ids = ConcurrentHashMap.newKeySet();

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int j = 0; j < idsPerThread; j++) {
                        ids.add(generator.nextId());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS), "ID 生成超时");
        pool.shutdown();

        assertEquals(threadCount * idsPerThread, ids.size(), "并发生成出现了重复 ID");
    }
}
