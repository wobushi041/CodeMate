package com.wobushi041.codemate.runner;

import com.wobushi041.codemate.model.dto.RecommendCacheSnapshot;
import com.wobushi041.codemate.model.dto.RecommendCacheWarmupMessage;
import com.wobushi041.codemate.model.enums.RecommendCacheStatus;
import com.wobushi041.codemate.mq.CacheWarmupProducer;
import com.wobushi041.codemate.service.RecommendCacheService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 推荐缓存预热启动引导执行器
 *
 * @author wobushi041
 */
@Component
@Slf4j
public class CacheWarmupBootstrapRunner implements ApplicationRunner {

    /**
     * 缓存预热引导分布式锁键前缀
     */
    private static final String CACHE_WARMUP_BOOTSTRAP_LOCK_KEY_PREFIX = "cache:warmup:bootstrap:lock:";

    /**
     * 注入缓存预热消息生产者依赖
     */
    @Resource
    private CacheWarmupProducer cacheWarmupProducer;

    /**
     * 注入推荐缓存服务依赖
     */
    @Resource
    private RecommendCacheService recommendCacheService;

    /**
     * 注入 Redisson 客户端依赖
     */
    @Resource
    private RedissonClient redissonClient;

    /**
     * 是否开启启动时缓存预热引导开关
     */
    @Value("${codemate.cache.warmup.bootstrap-enabled}")
    private boolean bootstrapEnabled;

    /**
     * 种子预热用户 id 列表配置（逗号分隔）
     */
    @Value("${codemate.cache.warmup.seed-user-ids}")
    private String seedUserIds;

    /**
     * 默认预热页码
     */
    @Value("${codemate.cache.warmup.default-page-num}")
    private long defaultPageNum;

    /**
     * 默认预热每页大小
     */
    @Value("${codemate.cache.warmup.default-page-size}")
    private long defaultPageSize;

    /**
     * 分布式锁租约时长（分钟）
     */
    @Value("${codemate.cache.warmup.lock-lease-seconds}")
    private long lockLeaseSeconds;

    /**
     * 应用启动完成后解析种子用户并执行初始推荐缓存预热引导
     *
     * @param args 应用启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        // 检查缓存预热引导开关是否启用
        if (!bootstrapEnabled) {
            log.info("cache warmup bootstrap is disabled");
            return;
        }

        // 解析种子用户 id 列表并逐个触发预热引导任务
        try {
            List<Long> userIds = Arrays.stream(seedUserIds.split(","))
                    .map(String::trim)
                    .filter(item -> !item.isEmpty())
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
            for (Long userId : userIds) {
                RecommendCacheWarmupMessage message =
                        cacheWarmupProducer.newWarmupMessage(userId, defaultPageNum, defaultPageSize);
                bootstrapWarmupTask(message);
            }
            log.info("缓存预热已启动, userIds={}", userIds);
        } catch (Exception e) {
            throw new IllegalStateException("缓存预热失败", e);
        }
    }

    /**
     * 基于 Redisson 分布式锁执行单用户推荐缓存预热并投递下一轮延时调度消息
     *
     * @param message 推荐缓存预热任务消息对象
     */
    private void bootstrapWarmupTask(RecommendCacheWarmupMessage message) throws InterruptedException {
        // 构造分布式锁并尝试加锁保障多实例启动幂等性
        String lockKey = CACHE_WARMUP_BOOTSTRAP_LOCK_KEY_PREFIX + message.getTaskId();
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(0, lockLeaseSeconds, TimeUnit.MINUTES);
            if (!locked) {
                log.info("cache warmup bootstrap lock busy, taskId={}, runId={}", message.getTaskId(), message.getRunId());
                return;
            }

            // 读取当前推荐缓存快照，若缓存非有效状态则触发刷新
            RecommendCacheSnapshot cacheSnapshot = recommendCacheService.getRecommendCacheSnapshot(
                    message.getUserId(),
                    message.getPageNum(),
                    message.getPageSize());
            boolean refreshed = false;
            if (cacheSnapshot.getStatus() != RecommendCacheStatus.VALID) {
                cacheSnapshot = recommendCacheService.refreshRecommendCache(
                        message.getUserId(),
                        message.getPageNum(),
                        message.getPageSize());
                refreshed = true;
            }

            // 计算下次调度延迟时间并投递下一轮延时预热任务
            long nextDelayMillis = recommendCacheService.calculateNextDelayMillis(cacheSnapshot);
            RecommendCacheWarmupMessage nextMessage = cacheWarmupProducer.newWarmupMessage(
                    message.getUserId(),
                    message.getPageNum(),
                    message.getPageSize());
            cacheWarmupProducer.scheduleWarmupTask(nextMessage, nextDelayMillis);
            log.info("{} bootstrap warmup, taskId={}, runId={}, status={}, nextDelayMillis={}",
                    refreshed ? "refresh" : "reuse",
                    message.getTaskId(),
                    message.getRunId(),
                    cacheSnapshot.getStatus(),
                    nextDelayMillis);
        } finally {
            // 释放当前线程持有的分布式锁
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

}
