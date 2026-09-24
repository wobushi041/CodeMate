package com.wobushi041.codemate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 应用上下文与基础组件单元测试
 *
 * @author wobushi041
 */
@SpringBootTest
class CodemateApplicationTests {

    /**
     * 注入 Redisson 客户端依赖
     */
    @Resource
    private RedissonClient redissonClient;

    /**
     * 测试 Redisson 键值读写与分布式锁获取
     */
    // 场景：测试 Redisson 桶存储与分布式锁加锁机制
    @Test
    void redissonTest() {
        // 1. 准备测试数据并写入 Redisson 桶
        RBucket<Object> test = redissonClient.getBucket("test");
        test.set("123", 1, TimeUnit.DAYS);
        System.out.println(test.get());

        // 2. 断言桶存储结果
        Assertions.assertEquals("123", test.get());

        // 3. 获取分布式锁并验证加锁行为
        RLock lock = redissonClient.getLock("041:lock");
        try {
            if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                System.out.println("获取到锁" + Thread.currentThread().getId());
                Thread.sleep(30000);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 测试 JUnit 基础断言方法
     */
    // 场景：测试 JUnit 常用基础断言与组合断言行为
    @Test
    void assertionTest() {
        // 1. 准备测试数据与基础值断言
        Assertions.assertEquals(1, 1 + 0);
        Assertions.assertTrue(true);
        Assertions.assertFalse(false);
        Assertions.assertNotEquals(2, 3);
        java.lang.String string1 = "hello";
        java.lang.String string2 = "hello";

        // 2. 执行对象引用一致性断言
        // 判断两个引用是不是指向同一个对象
        Assertions.assertSame(string1, string2);

        // 3. 断言组合条件结果
        Assertions.assertAll(
                () -> Assertions.assertEquals(1, 1 + 0),
                () -> Assertions.assertTrue(true),
                () -> Assertions.assertFalse(false)
        );
    }

}
