package com.wobushi041.codemate.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 消息队列与死信延时交换机配置
 *
 * @author wobushi041
 */
@Configuration
public class RabbitMqConfig {

    /**
     * 缓存预热延时交换机名称
     */
    public static final String CACHE_WARMUP_DELAY_EXCHANGE = "cache.warmup.delay.exchange";

    /**
     * 缓存预热延时队列名称
     */
    public static final String CACHE_WARMUP_DELAY_QUEUE = "cache.warmup.delay.queue";

    /**
     * 缓存预热延时路由键
     */
    public static final String CACHE_WARMUP_DELAY_ROUTING_KEY = "cache.warmup.delay";

    /**
     * 缓存预热执行交换机名称
     */
    public static final String CACHE_WARMUP_EXECUTE_EXCHANGE = "cache.warmup.execute.exchange";

    /**
     * 缓存预热执行队列名称
     */
    public static final String CACHE_WARMUP_EXECUTE_QUEUE = "cache.warmup.execute.queue";

    /**
     * 缓存预热执行路由键
     */
    public static final String CACHE_WARMUP_EXECUTE_ROUTING_KEY = "cache.warmup.execute";

    /**
     * 缓存预热失败兜底交换机名称
     */
    public static final String CACHE_WARMUP_FAIL_EXCHANGE = "cache.warmup.fail.exchange";

    /**
     * 缓存预热失败兜底队列名称
     */
    public static final String CACHE_WARMUP_FAIL_QUEUE = "cache.warmup.fail.queue";

    /**
     * 缓存预热失败兜底路由键
     */
    public static final String CACHE_WARMUP_FAIL_ROUTING_KEY = "cache.warmup.fail";

    /**
     * 声明缓存预热延时直连交换机
     *
     * @return 延时直连交换机实例
     */
    @Bean
    public DirectExchange cacheWarmupDelayExchange() {
        // 创建持久化、非自动删除的直连交换机
        return new DirectExchange(CACHE_WARMUP_DELAY_EXCHANGE, true, false);
    }

    /**
     * 声明缓存预热延时队列并绑定死信转发至执行交换机
     *
     * @return 延时队列实例
     */
    @Bean
    public Queue cacheWarmupDelayQueue() {
        // 配置死信交换机与死信路由键参数
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", CACHE_WARMUP_EXECUTE_EXCHANGE);
        arguments.put("x-dead-letter-routing-key", CACHE_WARMUP_EXECUTE_ROUTING_KEY);

        // 创建持久化、非独占、非自动删除且携带 DLX 参数的延时队列
        return new Queue(CACHE_WARMUP_DELAY_QUEUE, true, false, false, arguments);
    }

    /**
     * 绑定缓存预热延时队列至延时交换机
     *
     * @return 延时队列绑定关系实例
     */
    @Bean
    public Binding cacheWarmupDelayBinding() {
        // 使用延时路由键将延时队列绑定到延时交换机
        return BindingBuilder.bind(cacheWarmupDelayQueue())
                .to(cacheWarmupDelayExchange())
                .with(CACHE_WARMUP_DELAY_ROUTING_KEY);
    }

    /**
     * 声明缓存预热执行直连交换机
     *
     * @return 执行直连交换机实例
     */
    @Bean
    public DirectExchange cacheWarmupExecuteExchange() {
        // 创建持久化、非自动删除的执行直连交换机
        return new DirectExchange(CACHE_WARMUP_EXECUTE_EXCHANGE, true, false);
    }

    /**
     * 声明缓存预热执行队列并绑定死信转发至失败兜底交换机
     *
     * @return 执行队列实例
     */
    @Bean
    public Queue cacheWarmupExecuteQueue() {
        // 配置执行失败时的死信兜底交换机与路由键参数
        Map<String, Object> arguments = new HashMap<String, Object>();
        arguments.put("x-dead-letter-exchange", CACHE_WARMUP_FAIL_EXCHANGE);
        arguments.put("x-dead-letter-routing-key", CACHE_WARMUP_FAIL_ROUTING_KEY);

        // 创建持久化执行队列
        return new Queue(CACHE_WARMUP_EXECUTE_QUEUE, true, false, false, arguments);
    }

    /**
     * 绑定缓存预热执行队列至执行交换机
     *
     * @return 执行队列绑定关系实例
     */
    @Bean
    public Binding cacheWarmupExecuteBinding() {
        // 使用执行路由键将执行队列绑定到执行交换机
        return BindingBuilder.bind(cacheWarmupExecuteQueue())
                .to(cacheWarmupExecuteExchange())
                .with(CACHE_WARMUP_EXECUTE_ROUTING_KEY);
    }

    /**
     * 声明缓存预热失败兜底直连交换机
     *
     * @return 失败兜底直连交换机实例
     */
    @Bean
    public DirectExchange cacheWarmupFailExchange() {
        // 创建持久化、非自动删除的失败兜底直连交换机
        return new DirectExchange(CACHE_WARMUP_FAIL_EXCHANGE, true, false);
    }

    /**
     * 声明缓存预热失败兜底持久化队列
     *
     * @return 失败兜底队列实例
     */
    @Bean
    public Queue cacheWarmupFailQueue() {
        // 创建持久化失败兜底队列
        return new Queue(CACHE_WARMUP_FAIL_QUEUE, true);
    }

    /**
     * 绑定缓存预热失败兜底队列至失败兜底交换机
     *
     * @return 失败兜底队列绑定关系实例
     */
    @Bean
    public Binding cacheWarmupFailBinding() {
        // 使用失败路由键将失败队列绑定到失败交换机
        return BindingBuilder.bind(cacheWarmupFailQueue())
                .to(cacheWarmupFailExchange())
                .with(CACHE_WARMUP_FAIL_ROUTING_KEY);
    }

    /**
     * 构建基于 Jackson 的 JSON 消息序列化转换器
     *
     * @return JSON 消息转换器实例
     */
    @Bean
    public MessageConverter messageConverter() {
        // 使用 Jackson2JsonMessageConverter 替代默认 JDK 序列化
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 构建并配置挂载 JSON 转换器的 RabbitTemplate
     *
     * @param connectionFactory RabbitMQ 连接工厂
     * @param messageConverter  消息序列化转换器
     * @return RabbitTemplate 模板实例
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        // 初始化 RabbitTemplate 并绑定 JSON 消息转换器
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

    /**
     * 构建并配置挂载 JSON 转换器的 RabbitMQ 消息监听容器工厂
     *
     * @param configurer        监听容器工厂自动配置器
     * @param connectionFactory RabbitMQ 连接工厂
     * @param messageConverter  消息序列化转换器
     * @return 消息监听容器工厂实例
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        // 初始化监听容器工厂并应用 Spring Boot 默认配置与 JSON 消息转换器
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(messageConverter);
        return factory;
    }

}
