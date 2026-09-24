package com.wobushi041.codemate.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * RedisTemplate 与 Spring Session 序列化配置
 *
 * @author wobushi041
 */
@Configuration
public class RedisTemplateConfig {

    /**
     * 构建并注册自定义 JSON 序列化的 RedisTemplate 实例
     *
     * @param redisConnectionFactory Redis 连接工厂
     * @return RedisTemplate 操作模板实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        // 创建 RedisTemplate 实例并绑定连接工厂
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // 配置 Key 与 HashKey 使用 String 序列化器
        RedisSerializer<String> string = RedisSerializer.string();
        redisTemplate.setKeySerializer(string);
        redisTemplate.setHashKeySerializer(string);

        // 配置 Value 与 HashValue 使用自定义 JSON 序列化器
        GenericJackson2JsonRedisSerializer json = getJsonRedisSerializer();
        redisTemplate.setHashValueSerializer(json);
        redisTemplate.setValueSerializer(json);
        return redisTemplate;
    }

    /**
     * 构建并注册 Spring Session 默认使用的 Redis JSON 序列化器
     *
     * @return Redis 序列化器实例
     */
    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        // 复用支持 Java 8 时间与多态类型的 JSON 序列化器
        return getJsonRedisSerializer();
    }

    /**
     * 构建支持 Java 8 时间模块与类型元信息的 Jackson Redis 序列化器
     *
     * @return GenericJackson2JsonRedisSerializer 实例
     */
    private GenericJackson2JsonRedisSerializer getJsonRedisSerializer() {
        // 创建 ObjectMapper 并开启非 final 类的默认类型信息记录
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);

        // 注册 Java 8 时间模块并禁用时间戳输出格式
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 封装并返回通用 Jackson Redis 序列化器
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

}
