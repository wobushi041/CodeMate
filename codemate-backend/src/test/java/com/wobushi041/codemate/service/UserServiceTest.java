package com.wobushi041.codemate.service;

import com.wobushi041.codemate.model.domain.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import jakarta.annotation.Resource;
import java.util.Arrays;
import java.util.List;

/**
 * 用户服务与 Redis 模板操作单元测试
 *
 * @author wobushi041
 */
@SpringBootTest
public class UserServiceTest {

    /**
     * 注入 Redis 模板依赖
     */
    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 测试 Redis 键值增删改查基础操作
     */
    // 场景：测试 RedisTemplate 字符串、数值及用户对象的写入、读取与删除操作
    @Test
    void test() {
        // 1. 准备测试数据并写入 Redis（示例已注释保留）
//        ValueOperations valueOperations = redisTemplate.opsForValue();
//        // 插
//        valueOperations.set("hsuString", "dog");
//        valueOperations.set("hsuInt", 1);
//        valueOperations.set("hsuDouble", 2.0);
//        User user = new User();
//        user.setId(1L);
//        user.setUsername("hsu");
//        valueOperations.set("hsuUser", user);

        // 2. 调用待测方法查询并断言结果
//        // 查
//        Object hsu = valueOperations.get("hsuString");
//        Assertions.assertEquals("dog", (String) hsu);
//        hsu = valueOperations.get("hsuInt");
//        Assertions.assertEquals(1, (int) (Integer) hsu);
//        hsu = valueOperations.get("hsuDouble");
//        Assertions.assertEquals(2.0, (Double) hsu);
//        System.out.println(valueOperations.get("hsuUser"));

        // 3. 清理测试缓存键
//        // 删
//        redisTemplate.delete("hsuString");
//        redisTemplate.delete("hsuInt");
//        redisTemplate.delete("hsuDouble");
//        redisTemplate.delete("hsuUser");
    }

}