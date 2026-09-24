package com.wobushi041.codemate.config;

import org.junit.jupiter.api.Test;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Redisson 配置与分布式集合单元测试
 *
 * @author wobushi041
 */
@SpringBootTest
class RedissonConfigTest {

    /**
     * 注入 Redisson 客户端依赖
     */
    @Resource
    private RedissonClient redissonClient;

    /**
     * 测试本地列表与 Redisson 分布式列表读写
     */
    // 场景：测试本地 ArrayList 与 Redisson RList 的元素添加与读取操作
    @Test
    void redissonClient() {
        // 1. 准备本地列表测试数据并验证基础操作
        ArrayList<String> list = new ArrayList<>();
        list.add("041");
        System.out.println(list.get(0));
        list.remove(0);

        // 2. 调用 Redisson 客户端操作分布式列表
        RList<String> rList = redissonClient.getList("test-list");
        rList.add("hsu");

        // 3. 输出并验证分布式列表元素结果
        System.out.println("rlist:" + rList.get(0));
    }

}