package com.wobushi041.codemate.once.importuser;

import com.wobushi041.codemate.mapper.UserMapper;
import com.wobushi041.codemate.model.domain.User;
import com.wobushi041.codemate.service.UserService;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 批量导入用户数据任务组件
 *
 * @author wobushi041
 */
@Data
@Component
public class InsertUsers {

    /**
     * 单线程循环插入的用户总数量
     */
    public final int INSERT_COUNT = 5000;

    /**
     * 注入用户持久层依赖
     */
    @Resource
    private UserMapper userMapper;

    /**
     * 注入用户服务依赖
     */
    @Resource
    private UserService userService;

    /**
     * 并发批量插入用户专用自定义线程池
     */
    private ExecutorService executorService = new ThreadPoolExecutor(
            16,
            1000,
            10000,
            TimeUnit.MINUTES,
            new ArrayBlockingQueue<>(10000)
    );

    /**
     * 单线程循环批量插入测试用户数据
     */
    public void doInsertUsers() {
        // 启动秒表计时器
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // 循环构造并逐条插入测试用户记录
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
            user.setTags("");
            userMapper.insert(user);
        }

        // 停止计时并打印总耗时
        stopWatch.stop();
        System.out.println("循环插入执行时间（毫秒）：" + stopWatch.getLastTaskTimeMillis());
    }

    /**
     * 使用自定义线程池与 CompletableFuture 并发批量插入用户数据
     */
    public void doConcurrencyInsertUser() {
        // 启动秒表计时并定义并发分批参数
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final int INSERT_NUM = 100000;
        final int batchSize = 5000;
        List<CompletableFuture<Void>> futureList = new ArrayList<>();

        // 按批次构建用户数据并提交异步批量保存任务
        for (int i = 0; i < Math.ceil((double) INSERT_NUM / batchSize); i++) {
            List<User> userList = new ArrayList<>();
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
                user.setTags("java，python，c++，go");
                userMapper.insert(user);
            }
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                System.out.println("ThreadName：" + Thread.currentThread().getName());
                userService.saveBatch(userList, batchSize);
            }, executorService);
            futureList.add(future);
        }

        // 阻塞等待所有并发批次任务执行完毕并输出总耗时
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0])).join();
        stopWatch.stop();
        System.out.println("并发批量插入执行时间（毫秒）：" + stopWatch.getLastTaskTimeMillis());
    }

}
