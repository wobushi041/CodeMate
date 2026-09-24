package com.wobushi041.codemate.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.wobushi041.codemate.ai.AiChatService;
import com.wobushi041.codemate.model.domain.User;
import com.wobushi041.codemate.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * AI 编程助手控制层
 *
 * @author wobushi041
 */
@RestController
@RequestMapping("/ai")
@Slf4j
public class AiController {

    /**
     * 注入 AI 对话服务依赖
     */
    @Resource
    private AiChatService aiChatService;

    /**
     * 注入用户服务依赖
     */
    @Resource
    private UserService userService;

    /**
     * 注入 Gson 序列化依赖
     */
    @Resource
    private Gson gson;

    /**
     * 执行 AI 编程助手 SSE 流式对话接口（自动注入当前用户标签上下文增强 RAG 检索）
     *
     * @param message 用户提问消息内容
     * @param request HTTP 请求对象
     * @return SSE 流式文本事件响应流
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(@RequestParam(value = "message") String message,
                                              HttpServletRequest request) {
        // 获取当前登录用户及会话记忆 id
        User loginUser = userService.getLoginUserFromRequest(request);
        int memoryId = (int) loginUser.getId();

        // 读取用户标签并拼接上下文增强查询文本
        String enrichedMessage = buildEnrichedMessage(loginUser, message);

        // 调用 AI 流式对话并封装为 ServerSentEvent 响应流
        return aiChatService.chatStream(memoryId, enrichedMessage)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build())
                .doOnError(e -> log.error("AI 流式对话异常, userId={}", loginUser.getId(), e));
    }

    /**
     * 执行 AI 编程助手同步对话接口（非流式）
     *
     * @param message 用户提问消息内容
     * @param request HTTP 请求对象
     * @return AI 助手完整回复文本
     */
    @PostMapping("/chat")
    public String chatSync(@RequestParam(value = "message") String message,
                           HttpServletRequest request) {
        // 获取当前登录用户并拼接标签上下文
        User loginUser = userService.getLoginUserFromRequest(request);
        String enrichedMessage = buildEnrichedMessage(loginUser, message);

        // 调用同步对话接口并返回响应结果
        return aiChatService.chat(enrichedMessage);
    }

    /**
     * 将用户昵称与技术标签拼接到提问消息中以增强 RAG 检索上下文
     *
     * @param user    当前登录用户对象
     * @param message 原始用户提问消息
     * @return 拼接用户画像上下文后的增强消息字符串
     */
    private String buildEnrichedMessage(User user, String message) {
        // 解析用户标签列表，若为空则直接返回原始消息
        List<String> tags = parseTags(user.getTags());
        if (tags.isEmpty()) {
            return message;
        }

        // 获取用户展示名称并拼接画像提示词前缀
        String username = Optional.ofNullable(user.getUsername())
                .orElse(user.getUserAccount());
        return "用户叫做" + username + "，用户偏向 " + String.join(", ", tags) + "，想深入学习编程。" + message;
    }

    /**
     * 解析用户标签 JSON 字符串为标签列表
     *
     * @param tagsJson 用户标签 JSON 字符串
     * @return 解析后的标签字符串列表
     */
    private List<String> parseTags(String tagsJson) {
        // 校验 JSON 字符串是否为空白
        if (tagsJson == null || tagsJson.isBlank()) {
            return Collections.emptyList();
        }

        // 反序列化 JSON 字符串为字符串列表，解析失败时降级返回空列表
        try {
            List<String> tags = gson.fromJson(tagsJson, new TypeToken<List<String>>() {}.getType());
            return tags != null ? tags : Collections.emptyList();
        } catch (Exception e) {
            log.warn("解析用户 tags 失败: {}", tagsJson, e);
            return Collections.emptyList();
        }
    }

}
