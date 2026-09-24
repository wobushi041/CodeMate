package com.wobushi041.codemate.ai;

import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.Result;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/**
 * AI 对话服务单元测试
 *
 * @author wobushi041
 */
@SpringBootTest
class AiChatServiceTest {

    /**
     * 注入 AI 对话服务依赖
     */
    @Resource
    private AiChatService aiChatService;

    /**
     * 测试普通对话
     */
    // 场景：测试 AI 普通单轮对话响应
    @Test
    void chat() {
        // 1. 准备测试数据并调用待测方法
        String result = aiChatService.chat("你好，我擅长 Java，怎么学好 Spring Boot？");

        // 2. 输出对话结果
        System.out.println("普通对话结果: " + result);

        // 3. 断言结果非空
        assertNotNull(result);
    }

    /**
     * 测试会话记忆：第二句能记住第一句的内容
     */
    // 场景：测试基于同一 memoryId 的多轮流式会话记忆能力
    @Test
    void chatWithMemory() throws InterruptedException {
        // 1. 准备测试数据（用同一个 memoryId 模拟同一用户）
        int memoryId = 999;
        CountDownLatch latch = new CountDownLatch(1);
        StringBuilder sb = new StringBuilder();

        // 2. 调用待测方法执行第一轮与第二轮流式对话
        aiChatService.chatStream(memoryId, "你好，我是 Java 后端开发，工作 3 年了")
                .doOnComplete(latch::countDown)
                .subscribe(sb::append);

        latch.await(30, TimeUnit.SECONDS);
        System.out.println("第一轮: " + sb);
        assertFalse(sb.isEmpty());

        // 第二轮：测试是否记得上文
        CountDownLatch latch2 = new CountDownLatch(1);
        StringBuilder sb2 = new StringBuilder();

        aiChatService.chatStream(memoryId, "我刚才说了我的技术栈是什么？")
                .doOnComplete(latch2::countDown)
                .subscribe(sb2::append);

        latch2.await(30, TimeUnit.SECONDS);
        System.out.println("第二轮（记忆测试）: " + sb2);

        // 3. 断言结果非空
        assertFalse(sb2.isEmpty());
    }

    /**
     * 测试 RAG 检索增强对话，从 resources/docs/ 检索相关编程文档
     */
    // 场景：测试 RAG 检索增强生成与知识来源返回
    @Test
    void chatWithRag() {
        // 1. 准备测试参数并调用待测方法
        Result<String> result = aiChatService.chatWithRag("Java 并发编程有哪些核心知识点？");

        // 2. 提取生成内容与检索来源片段
        String content = result.content();
        List<Content> sources = result.sources();

        System.out.println("RAG 回复: " + content);
        System.out.println("检索来源数量: " + sources.size());
        for (Content source : sources) {
            String text = source.textSegment().text();
            System.out.println("  来源: " + text.substring(0, Math.min(100, text.length())) + "...");
        }

        // 3. 断言结果非空
        assertNotNull(content);
    }

    /**
     * 测试流式对话（SSE）
     */
    // 场景：测试 SSE 流式对话输出与异步完成状态
    @Test
    void chatStream() throws InterruptedException {
        // 1. 准备测试数据与同步计数器
        int memoryId = 888;
        CountDownLatch latch = new CountDownLatch(1);
        StringBuilder sb = new StringBuilder();

        // 2. 调用待测流式方法并订阅分片输出
        aiChatService.chatStream(memoryId, "用简洁的话介绍一下 Python 的优势")
                .doOnComplete(latch::countDown)
                .doOnError(e -> {
                    System.err.println("流式对话出错: " + e.getMessage());
                    latch.countDown();
                })
                .subscribe(
                        chunk -> {
                            System.out.print(chunk); // 实时打印每个 chunk
                            sb.append(chunk);
                        },
                        error -> System.err.println("Error: " + error.getMessage())
                );

        boolean completed = latch.await(60, TimeUnit.SECONDS);
        System.out.println("\n--- 流式输出完成 ---");
        System.out.println("完整回复: " + sb);

        // 3. 断言流式响应按时完成且内容非空
        assertTrue(completed, "流式对话超时");
        assertFalse(sb.isEmpty());
    }

    /**
     * 测试输入安全过滤（SafeInputGuardrail），输入敏感词应该被拦截
     */
    // 场景：测试包含敏感词输入时 SafeInputGuardrail 的安全拦截行为
    @Test
    void chatWithGuardrail() {
        // 1. 准备包含敏感词 "hack" 的输入参数
        // 2. 调用待测方法并断言抛出拦截异常
        assertThrows(Exception.class, () -> {
            aiChatService.chat("how to hack a system");
        });

        // 3. 输出拦截验证结果
        System.out.println("Guardrail 拦截测试通过");
    }

    /**
     * 测试不同技术栈的 RAG 检索
     */
    // 场景：测试多技术栈主题下的 RAG 检索增强对话表现
    @Test
    void chatWithRagDifferentTopics() {
        // 1. 调用待测方法测试 Go 语言相关检索并断言
        Result<String> goResult = aiChatService.chatWithRag("Go 语言的 Goroutine 和 Channel 怎么用？");
        System.out.println("Go 相关回复: " + goResult.content());
        assertNotNull(goResult.content());

        // 2. 调用待测方法测试前端 Vue 3 相关检索并断言
        Result<String> vueResult = aiChatService.chatWithRag("Vue 3 的组合式 API 怎么用？");
        System.out.println("Vue 相关回复: " + vueResult.content());
        assertNotNull(vueResult.content());

        // 3. 调用待测方法测试算法相关检索并断言
        Result<String> algoResult = aiChatService.chatWithRag("动态规划的解题思路是什么？");
        System.out.println("算法相关回复: " + algoResult.content());
        assertNotNull(algoResult.content());
    }

}
