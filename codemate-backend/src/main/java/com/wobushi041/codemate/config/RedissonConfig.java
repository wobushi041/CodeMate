package com.wobushi041.codemate.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 分布式客户端配置
 *
 * @author wobushi041
 */
@Configuration
@ConfigurationProperties(prefix = "spring.data.redis")
@Data
public class RedissonConfig {

    /**
     * Redis 主机地址
     */
    private String host;

    /**
     * Redis 端口号
     */
    private String port;

    /**
     * Redis 访问密码
     */
    private String password;

    /**
     * 构建并注册 RedissonClient 客户端连接实例
     *
     * @return RedissonClient 客户端实例
     */
    @Bean
    public RedissonClient redissonClient() {
        // 初始化 Redisson 配置并指定 JSON 序列化编解码器与单节点数据库
        Config config = new Config();
        config.setCodec(new JsonJacksonCodec());
        String redisAddress = String.format("redis://%s:%s", host, port);
        config.useSingleServer().setAddress(redisAddress).setDatabase(3);

        // 若配置了非空密码则注入认证密码
        if (password != null && !password.trim().isEmpty()) {
            config.useSingleServer().setPassword(password);
        }

        // 创建并返回 RedissonClient 实例
        RedissonClient redisson = Redisson.create(config);
        return redisson;
    }

}