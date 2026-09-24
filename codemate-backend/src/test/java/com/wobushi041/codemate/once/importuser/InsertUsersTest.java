package com.wobushi041.codemate.once.importuser;

import com.wobushi041.codemate.mapper.UserMapper;
import com.wobushi041.codemate.model.domain.User;
import com.wobushi041.codemate.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 批量导入用户数据单元测试
 *
 * @author wobushi041
 */
@SpringBootTest
class InsertUsersTest {

    /**
     * 注入用户 Mapper 依赖
     */
    @Resource
    private UserMapper userMapper;

    /**
     * 注入用户服务依赖
     */
    @Resource
    private UserService userService;

    /**
     * 并发批量插入任务自定义线程池
     */
    private final ExecutorService executorService = new ThreadPoolExecutor(
            16, // corePoolSize: 核心线程数，线程池中始终保持活跃的线程数量，即使它们处于空闲状态
            1000, // maximumPoolSize: 最大线程数，线程池中允许的最大线程数量
            10000, // keepAliveTime: 当线程数超过核心线程数时，非核心线程空闲前的最大存活时间
            TimeUnit.MINUTES, // 时间单位，上面的 keepAliveTime 的单位
            new ArrayBlockingQueue<>(10000) // 工作队列，存放待执行任务的阻塞队列
    );

    /**
     * 测试单线程循环插入用户数据
     */
    // 场景：测试单线程顺序循环插入 100000 条用户数据并统计耗时
    @Test
    // @Scheduled(initialDelay = 5000, fixedRate = Long.MAX_VALUE)
    public void doInsertUsers() {
        // 1. 准备计时器与插入总量参数
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final int INSERT_COUNT = 100000;

        // 2. 调用待测方法循环写入用户记录
        for (int i = 0; i < INSERT_COUNT; i++) {
            User user = new User();
            user.setUsername("041041");
            user.setUserAccount("10086");
            user.setAvatarUrl("https://thirdwx.qlogo.cn/mmopen/vi_32/PiajxSqBRaELkfM4IsxxWrB70flGuaDcq55mDxh8r4DuwOJLuluSmRCH9Pk1MFibry5icVgHtfwMmnYGqT49svVKV3X1wMer2OCC3ob5leZX5lF8HMbPo1Qww/132");
            user.setGender(0);
            user.setUserPassword("12345678");
            user.setPhone("1343");
            user.setEmail("133435");
            user.setUserStatus(0);
            user.setUserRole(0);
            user.setPlanetCode("12");
            user.setTags("[\"java\",\"python\"]");
            userMapper.insert(user);
        }

        // 3. 停止计时并输出执行耗时结果
        stopWatch.stop();
        System.out.println("循环插入执行时间（毫秒）：" + stopWatch.getLastTaskTimeMillis());
    }

    /**
     * 测试多线程并发分批插入用户数据
     */
    // 场景：测试使用自定义线程池与 CompletableFuture 并发分批插入用户数据
    @Test
    public void doConcurrencyInsertUser() {
        // 1. 准备计时器与分批任务参数
        StopWatch stopWatch = new StopWatch();
        stopWatch.start(); // 开始计时
        final int INSERT_NUM = 100000; // 总插入数据量
        final int batchSize = 5000; // 每批次处理的数据量
        List<CompletableFuture<Void>> futureList = new ArrayList<>();

        // 2. 根据批次大小分割任务并提交异步批量插入
        for (int i = 0; i < Math.ceil((double) INSERT_NUM / batchSize); i++) {
            List<User> userList = new ArrayList<>();
            // 创建每批次的用户数据
            for (int j = 0; j < batchSize; j++) {
                User user = new User();
                user.setUsername("041041");
                user.setUserAccount("10086");
                user.setAvatarUrl("https://thirdwx.qlogo.cn/mmopen/vi_32/PiajxSqBRaELkfM4IsxxWrB70flGuaDcq55mDxh8r4DuwOJLuluSmRCH9Pk1MFibry5icVgHtfwMmnYGqT49svVKV3X1wMer2OCC3ob5leZX5lF8HMbPo1Qww/132");
                user.setGender(0);
                user.setUserPassword("12345678");
                user.setPhone("1343");
                user.setEmail("133435");
                user.setUserStatus(0);
                user.setUserRole(0);
                user.setPlanetCode("12");
                user.setTags("[\"java\",\"python\"]");
                userMapper.insert(user);
            }
            // 异步执行数据库插入操作
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                System.out.println("ThreadName：" + Thread.currentThread().getName());
                userService.saveBatch(userList, batchSize);
            }, executorService);
            futureList.add(future);
        }

        // 3. 等待所有异步任务完成并输出总耗时结果
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0])).join();
        stopWatch.stop(); // 停止计时
        System.out.println("并发批量插入执行时间（毫秒）：" + stopWatch.getLastTaskTimeMillis());
    }

}