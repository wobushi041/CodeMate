package com.wobushi041.codemate.mq;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wobushi041.codemate.config.RabbitMqConfig;
import com.wobushi041.codemate.model.domain.User;
import com.wobushi041.codemate.model.dto.RecommendCacheSnapshot;
import com.wobushi041.codemate.model.dto.RecommendCacheValue;
import com.wobushi041.codemate.model.dto.RecommendCacheWarmupMessage;
import com.wobushi041.codemate.model.enums.RecommendCacheStatus;
import com.wobushi041.codemate.service.RecommendCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 推荐缓存预热消息流转单元测试
 *
 * @author wobushi041
 */
@SpringBootTest(properties = {
        "codemate.cache.warmup.bootstrap-enabled=false",
        "codemate.cache.warmup.delay-millis=30000",
        "codemate.cache.warmup.logic-expire-millis=30000",
        "codemate.cache.warmup.refresh-ahead-millis=1000",
        "codemate.cache.warmup.lock-lease-seconds=5",
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
class CacheWarmupFlowTest {

    /**
     * 注入缓存预热消息生产者依赖
     */
    @Autowired
    private CacheWarmupProducer cacheWarmupProducer;

    /**
     * 注入缓存预热消息消费者依赖
     */
    @Autowired
    private CacheWarmupConsumer cacheWarmupConsumer;

    /**
     * 模拟 RabbitMQ 消息模板依赖
     */
    @MockBean
    private RabbitTemplate rabbitTemplate;

    /**
     * 模拟推荐缓存服务依赖
     */
    @MockBean
    private RecommendCacheService recommendCacheService;

    /**
     * 模拟 Redisson 客户端依赖
     */
    @MockBean
    private RedissonClient redissonClient;

    /**
     * 模拟分布式锁实例
     */
    private RLock lock;

    /**
     * 初始化每个测试用例的分布式锁模拟行为
     */
    @BeforeEach
    void setUp() throws InterruptedException {
        lock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(eq(0L), anyLong(), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
    }

    /**
     * 测试调度预热任务时设置指定的延迟时间与持久化投递模式
     */
    // 场景：测试生产者发送延迟预热任务时正确设置消息 TTL 与持久化模式
    @Test
    void scheduleWarmupTask_shouldSetSpecifiedDelay() {
        // 1. 准备测试数据
        RecommendCacheWarmupMessage message = cacheWarmupProducer.newWarmupMessage(1001L, 1L, 10L);

        // 2. 调用待测方法
        cacheWarmupProducer.scheduleWarmupTask(message, 30000L);

        // 3. 断言结果
        ArgumentCaptor<MessagePostProcessor> postProcessorCaptor =
                ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMqConfig.CACHE_WARMUP_DELAY_EXCHANGE),
                eq(RabbitMqConfig.CACHE_WARMUP_DELAY_ROUTING_KEY),
                eq(message),
                postProcessorCaptor.capture());

        Message processedMessage = postProcessorCaptor.getValue().postProcessMessage(
                new Message("{}".getBytes(StandardCharsets.UTF_8), new MessageProperties()));

        assertEquals("30000", processedMessage.getMessageProperties().getExpiration());
        assertEquals(MessageDeliveryMode.PERSISTENT, processedMessage.getMessageProperties().getDeliveryMode());
    }

    /**
     * 测试消费回调在缓存逻辑过期时刷新缓存并调度下一轮任务
     */
    // 场景：测试缓存处于逻辑过期状态时消费者触发缓存刷新并投递下一轮延迟任务
    @Test
    void deliverCallback_shouldRefreshExpiredCacheAndScheduleNextRound() {
        // 1. 准备测试数据与模拟依赖
        RecommendCacheWarmupMessage message = cacheWarmupProducer.newWarmupMessage(1003L, 2L, 5L);
        Page<User> userPage = new Page<>(2L, 5L);
        userPage.setTotal(3L);
        RecommendCacheSnapshot expiredSnapshot = new RecommendCacheSnapshot(
                "user:recommend:1003:2:5",
                RecommendCacheStatus.LOGIC_EXPIRED,
                new RecommendCacheValue(userPage, System.currentTimeMillis() - 1000)
        );
        RecommendCacheSnapshot refreshedSnapshot = new RecommendCacheSnapshot(
                "user:recommend:1003:2:5",
                RecommendCacheStatus.VALID,
                new RecommendCacheValue(userPage, System.currentTimeMillis() + 30000)
        );
        when(recommendCacheService.getRecommendCacheSnapshot(1003L, 2L, 5L)).thenReturn(expiredSnapshot);
        when(recommendCacheService.refreshRecommendCache(1003L, 2L, 5L)).thenReturn(refreshedSnapshot);
        when(recommendCacheService.calculateNextDelayMillis(refreshedSnapshot)).thenReturn(29000L);

        // 2. 调用待测方法
        cacheWarmupConsumer.deliverCallback(message);

        // 3. 断言结果
        verify(recommendCacheService).refreshRecommendCache(1003L, 2L, 5L);

        ArgumentCaptor<RecommendCacheWarmupMessage> nextMessageCaptor =
                ArgumentCaptor.forClass(RecommendCacheWarmupMessage.class);
        ArgumentCaptor<MessagePostProcessor> postProcessorCaptor =
                ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMqConfig.CACHE_WARMUP_DELAY_EXCHANGE),
                eq(RabbitMqConfig.CACHE_WARMUP_DELAY_ROUTING_KEY),
                nextMessageCaptor.capture(),
                postProcessorCaptor.capture());

        RecommendCacheWarmupMessage nextMessage = nextMessageCaptor.getValue();
        assertNotNull(nextMessage);
        assertEquals(message.getTaskId(), nextMessage.getTaskId());
        assertEquals(message.getUserId(), nextMessage.getUserId());
        assertEquals(message.getPageNum(), nextMessage.getPageNum());
        assertEquals(message.getPageSize(), nextMessage.getPageSize());
        assertNotEquals(message.getRunId(), nextMessage.getRunId());
        Message processedMessage = postProcessorCaptor.getValue().postProcessMessage(
                new Message("{}".getBytes(StandardCharsets.UTF_8), new MessageProperties()));
        assertEquals("29000", processedMessage.getMessageProperties().getExpiration());
        verify(lock).unlock();
    }

    /**
     * 测试消费回调在缓存进入提前刷新窗口时刷新缓存
     */
    // 场景：测试缓存进入提前刷新时间窗口时消费者主动刷新缓存并调度下一轮任务
    @Test
    void deliverCallback_shouldRefreshWhenCacheEntersRefreshAheadWindow() {
        // 1. 准备测试数据与模拟依赖
        RecommendCacheWarmupMessage message = cacheWarmupProducer.newWarmupMessage(1005L, 1L, 20L);
        Page<User> userPage = new Page<>(1L, 20L);
        userPage.setTotal(12L);
        RecommendCacheSnapshot refreshAheadSnapshot = new RecommendCacheSnapshot(
                "user:recommend:1005:1:20",
                RecommendCacheStatus.REFRESH_AHEAD,
                new RecommendCacheValue(userPage, System.currentTimeMillis() + 800)
        );
        RecommendCacheSnapshot refreshedSnapshot = new RecommendCacheSnapshot(
                "user:recommend:1005:1:20",
                RecommendCacheStatus.VALID,
                new RecommendCacheValue(userPage, System.currentTimeMillis() + 30000)
        );
        when(recommendCacheService.getRecommendCacheSnapshot(1005L, 1L, 20L)).thenReturn(refreshAheadSnapshot);
        when(recommendCacheService.refreshRecommendCache(1005L, 1L, 20L)).thenReturn(refreshedSnapshot);
        when(recommendCacheService.calculateNextDelayMillis(refreshedSnapshot)).thenReturn(29000L);

        // 2. 调用待测方法
        cacheWarmupConsumer.deliverCallback(message);

        // 3. 断言结果
        verify(recommendCacheService).refreshRecommendCache(1005L, 1L, 20L);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMqConfig.CACHE_WARMUP_DELAY_EXCHANGE),
                eq(RabbitMqConfig.CACHE_WARMUP_DELAY_ROUTING_KEY),
                any(RecommendCacheWarmupMessage.class),
                any(MessagePostProcessor.class));
        verify(lock).unlock();
    }

    /**
     * 测试消费回调在缓存仍有效时直接复用缓存并继续调度下一轮任务
     */
    // 场景：测试缓存有效期内消费者跳过刷新操作并继续调度下一轮延迟任务
    @Test
    void deliverCallback_shouldReuseValidCacheAndStillScheduleNextRound() {
        // 1. 准备测试数据与模拟依赖
        RecommendCacheWarmupMessage message = cacheWarmupProducer.newWarmupMessage(1004L, 1L, 20L);
        Page<User> userPage = new Page<>(1L, 20L);
        userPage.setTotal(8L);
        RecommendCacheSnapshot validSnapshot = new RecommendCacheSnapshot(
                "user:recommend:1004:1:20",
                RecommendCacheStatus.VALID,
                new RecommendCacheValue(userPage, System.currentTimeMillis() + 15000)
        );
        when(recommendCacheService.getRecommendCacheSnapshot(1004L, 1L, 20L)).thenReturn(validSnapshot);
        when(recommendCacheService.calculateNextDelayMillis(validSnapshot)).thenReturn(12000L);

        // 2. 调用待测方法
        cacheWarmupConsumer.deliverCallback(message);

        // 3. 断言结果
        verify(recommendCacheService, never()).refreshRecommendCache(1004L, 1L, 20L);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMqConfig.CACHE_WARMUP_DELAY_EXCHANGE),
                eq(RabbitMqConfig.CACHE_WARMUP_DELAY_ROUTING_KEY),
                any(RecommendCacheWarmupMessage.class),
                any(MessagePostProcessor.class));
        verify(lock).unlock();
    }

}
