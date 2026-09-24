package com.wobushi041.codemate.runner;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wobushi041.codemate.model.domain.User;
import com.wobushi041.codemate.model.dto.RecommendCacheSnapshot;
import com.wobushi041.codemate.model.dto.RecommendCacheValue;
import com.wobushi041.codemate.model.dto.RecommendCacheWarmupMessage;
import com.wobushi041.codemate.model.enums.RecommendCacheStatus;
import com.wobushi041.codemate.mq.CacheWarmupProducer;
import com.wobushi041.codemate.service.RecommendCacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 推荐缓存启动预热执行器单元测试
 *
 * @author wobushi041
 */
@ExtendWith(MockitoExtension.class)
class CacheWarmupBootstrapRunnerTest {

    /**
     * 模拟缓存预热消息生产者依赖
     */
    @Mock
    private CacheWarmupProducer cacheWarmupProducer;

    /**
     * 模拟推荐缓存服务依赖
     */
    @Mock
    private RecommendCacheService recommendCacheService;

    /**
     * 模拟 Redisson 客户端依赖
     */
    @Mock
    private RedissonClient redissonClient;

    /**
     * 模拟应用启动参数依赖
     */
    @Mock
    private ApplicationArguments applicationArguments;

    /**
     * 测试启动时当缓存不存在则刷新缓存并调度下一轮预热任务
     */
    // 场景：测试应用启动时遇到缺失缓存能够主动触发刷新并投递下一轮延迟预热任务
    @Test
    void run_shouldRefreshMissingCacheAndScheduleNextRound() throws InterruptedException {
        // 1. 准备测试数据与模拟依赖
        CacheWarmupBootstrapRunner runner = new CacheWarmupBootstrapRunner();
        RLock lock = mock(RLock.class);
        RecommendCacheWarmupMessage bootstrapMessage = new RecommendCacheWarmupMessage();
        bootstrapMessage.setTaskId("recommend:1:1:10");
        bootstrapMessage.setRunId("bootstrap-run");
        bootstrapMessage.setUserId(1L);
        bootstrapMessage.setPageNum(1L);
        bootstrapMessage.setPageSize(10L);

        RecommendCacheWarmupMessage nextMessage = new RecommendCacheWarmupMessage();
        nextMessage.setTaskId("recommend:1:1:10");
        nextMessage.setRunId("next-run");
        nextMessage.setUserId(1L);
        nextMessage.setPageNum(1L);
        nextMessage.setPageSize(10L);

        Page<User> userPage = new Page<>(1L, 10L);
        userPage.setTotal(10L);
        RecommendCacheSnapshot missingSnapshot = new RecommendCacheSnapshot(
                "user:recommend:1:1:10",
                RecommendCacheStatus.ABSENT,
                null
        );
        RecommendCacheSnapshot refreshedSnapshot = new RecommendCacheSnapshot(
                "user:recommend:1:1:10",
                RecommendCacheStatus.VALID,
                new RecommendCacheValue(userPage, System.currentTimeMillis() + 30000)
        );

        ReflectionTestUtils.setField(runner, "cacheWarmupProducer", cacheWarmupProducer);
        ReflectionTestUtils.setField(runner, "recommendCacheService", recommendCacheService);
        ReflectionTestUtils.setField(runner, "redissonClient", redissonClient);
        ReflectionTestUtils.setField(runner, "bootstrapEnabled", true);
        ReflectionTestUtils.setField(runner, "seedUserIds", "1");
        ReflectionTestUtils.setField(runner, "defaultPageNum", 1L);
        ReflectionTestUtils.setField(runner, "defaultPageSize", 10L);
        ReflectionTestUtils.setField(runner, "lockLeaseSeconds", 5L);

        when(cacheWarmupProducer.newWarmupMessage(1L, 1L, 10L)).thenReturn(bootstrapMessage, nextMessage);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(eq(0L), eq(5L), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(recommendCacheService.getRecommendCacheSnapshot(1L, 1L, 10L)).thenReturn(missingSnapshot);
        when(recommendCacheService.refreshRecommendCache(1L, 1L, 10L)).thenReturn(refreshedSnapshot);
        when(recommendCacheService.calculateNextDelayMillis(refreshedSnapshot)).thenReturn(29000L);

        // 2. 调用待测方法
        runner.run(applicationArguments);

        // 3. 断言结果
        verify(recommendCacheService).refreshRecommendCache(1L, 1L, 10L);
        verify(cacheWarmupProducer).scheduleWarmupTask(nextMessage, 29000L);
        verify(lock).unlock();
    }

    /**
     * 测试启动时当缓存有效则直接复用而不重新刷新
     */
    // 场景：测试应用启动时遇到有效缓存直接复用并继续投递下一轮延迟预热任务
    @Test
    void run_shouldReuseValidCacheWithoutRefreshing() throws InterruptedException {
        // 1. 准备测试数据与模拟依赖
        CacheWarmupBootstrapRunner runner = new CacheWarmupBootstrapRunner();
        RLock lock = mock(RLock.class);
        RecommendCacheWarmupMessage bootstrapMessage = new RecommendCacheWarmupMessage();
        bootstrapMessage.setTaskId("recommend:2:1:10");
        bootstrapMessage.setRunId("bootstrap-run");
        bootstrapMessage.setUserId(2L);
        bootstrapMessage.setPageNum(1L);
        bootstrapMessage.setPageSize(10L);

        RecommendCacheWarmupMessage nextMessage = new RecommendCacheWarmupMessage();
        nextMessage.setTaskId("recommend:2:1:10");
        nextMessage.setRunId("next-run");
        nextMessage.setUserId(2L);
        nextMessage.setPageNum(1L);
        nextMessage.setPageSize(10L);

        Page<User> userPage = new Page<>(1L, 10L);
        userPage.setTotal(6L);
        RecommendCacheSnapshot validSnapshot = new RecommendCacheSnapshot(
                "user:recommend:2:1:10",
                RecommendCacheStatus.VALID,
                new RecommendCacheValue(userPage, System.currentTimeMillis() + 30000)
        );

        ReflectionTestUtils.setField(runner, "cacheWarmupProducer", cacheWarmupProducer);
        ReflectionTestUtils.setField(runner, "recommendCacheService", recommendCacheService);
        ReflectionTestUtils.setField(runner, "redissonClient", redissonClient);
        ReflectionTestUtils.setField(runner, "bootstrapEnabled", true);
        ReflectionTestUtils.setField(runner, "seedUserIds", "2");
        ReflectionTestUtils.setField(runner, "defaultPageNum", 1L);
        ReflectionTestUtils.setField(runner, "defaultPageSize", 10L);
        ReflectionTestUtils.setField(runner, "lockLeaseSeconds", 5L);

        when(cacheWarmupProducer.newWarmupMessage(2L, 1L, 10L)).thenReturn(bootstrapMessage, nextMessage);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(eq(0L), eq(5L), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(recommendCacheService.getRecommendCacheSnapshot(2L, 1L, 10L)).thenReturn(validSnapshot);
        when(recommendCacheService.calculateNextDelayMillis(validSnapshot)).thenReturn(26000L);

        // 2. 调用待测方法
        runner.run(applicationArguments);

        // 3. 断言结果
        verify(recommendCacheService, never()).refreshRecommendCache(2L, 1L, 10L);
        verify(cacheWarmupProducer).scheduleWarmupTask(nextMessage, 26000L);
        verify(lock).unlock();
    }

    /**
     * 测试启动时当缓存进入提前刷新窗口则刷新缓存并调度下一轮任务
     */
    // 场景：测试应用启动时遇到处于提前刷新窗口内的缓存主动执行刷新并调度下一轮任务
    @Test
    void run_shouldRefreshWhenCacheEntersRefreshAheadWindow() throws InterruptedException {
        // 1. 准备测试数据与模拟依赖
        CacheWarmupBootstrapRunner runner = new CacheWarmupBootstrapRunner();
        RLock lock = mock(RLock.class);
        RecommendCacheWarmupMessage bootstrapMessage = new RecommendCacheWarmupMessage();
        bootstrapMessage.setTaskId("recommend:3:1:10");
        bootstrapMessage.setRunId("bootstrap-run");
        bootstrapMessage.setUserId(3L);
        bootstrapMessage.setPageNum(1L);
        bootstrapMessage.setPageSize(10L);

        RecommendCacheWarmupMessage nextMessage = new RecommendCacheWarmupMessage();
        nextMessage.setTaskId("recommend:3:1:10");
        nextMessage.setRunId("next-run");
        nextMessage.setUserId(3L);
        nextMessage.setPageNum(1L);
        nextMessage.setPageSize(10L);

        Page<User> userPage = new Page<>(1L, 10L);
        userPage.setTotal(7L);
        RecommendCacheSnapshot refreshAheadSnapshot = new RecommendCacheSnapshot(
                "user:recommend:3:1:10",
                RecommendCacheStatus.REFRESH_AHEAD,
                new RecommendCacheValue(userPage, System.currentTimeMillis() + 500)
        );
        RecommendCacheSnapshot refreshedSnapshot = new RecommendCacheSnapshot(
                "user:recommend:3:1:10",
                RecommendCacheStatus.VALID,
                new RecommendCacheValue(userPage, System.currentTimeMillis() + 30000)
        );

        ReflectionTestUtils.setField(runner, "cacheWarmupProducer", cacheWarmupProducer);
        ReflectionTestUtils.setField(runner, "recommendCacheService", recommendCacheService);
        ReflectionTestUtils.setField(runner, "redissonClient", redissonClient);
        ReflectionTestUtils.setField(runner, "bootstrapEnabled", true);
        ReflectionTestUtils.setField(runner, "seedUserIds", "3");
        ReflectionTestUtils.setField(runner, "defaultPageNum", 1L);
        ReflectionTestUtils.setField(runner, "defaultPageSize", 10L);
        ReflectionTestUtils.setField(runner, "lockLeaseSeconds", 5L);

        when(cacheWarmupProducer.newWarmupMessage(3L, 1L, 10L)).thenReturn(bootstrapMessage, nextMessage);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(eq(0L), eq(5L), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(recommendCacheService.getRecommendCacheSnapshot(3L, 1L, 10L)).thenReturn(refreshAheadSnapshot);
        when(recommendCacheService.refreshRecommendCache(3L, 1L, 10L)).thenReturn(refreshedSnapshot);
        when(recommendCacheService.calculateNextDelayMillis(refreshedSnapshot)).thenReturn(29000L);

        // 2. 调用待测方法
        runner.run(applicationArguments);

        // 3. 断言结果
        verify(recommendCacheService).refreshRecommendCache(3L, 1L, 10L);
        verify(cacheWarmupProducer).scheduleWarmupTask(nextMessage, 29000L);
        verify(lock).unlock();
    }

}
