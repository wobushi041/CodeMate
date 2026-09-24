package com.wobushi041.codemate.ai.model;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * DeepSeek 模型配置
 *
 * @author wobushi041
 */
@Configuration
@Slf4j
public class DeepSeekModelConfig {

    /**
     * DeepSeek 接口访问密钥
     */
    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String apiKey;

    /**
     * DeepSeek 模型名称
     */
    @Value("${langchain4j.open-ai.chat-model.model-name}")
    private String modelName;

    /**
     * DeepSeek 接口基础地址
     */
    @Value("${langchain4j.open-ai.chat-model.base-url}")
    private String baseUrl;

    /**
     * 模型采样温度参数
     */
    @Value("${langchain4j.open-ai.chat-model.temperature:0.7}")
    private double temperature;

    /**
     * 模型单次生成最大 Token 数
     */
    @Value("${langchain4j.open-ai.chat-model.max-tokens:2048}")
    private int maxTokens;

    /**
     * 注入聊天模型调用日志监听器依赖
     */
    @Resource
    private ChatModelListener chatModelListener;

    /**
     * 构建并注册 DeepSeek 普通聊天模型
     *
     * @return 普通聊天模型实例
     */
    @Bean
    public ChatModel deepSeekChatModel() {
        // 基于 OpenAI 兼容协议构建同步聊天模型并挂载日志监听器
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .listeners(List.of(chatModelListener))
                .build();
    }

    /**
     * 构建并注册 DeepSeek 流式聊天模型（SSE）
     *
     * @return 流式聊天模型实例
     */
    @Bean
    public StreamingChatModel deepSeekStreamingChatModel() {
        // 基于 OpenAI 兼容协议构建流式聊天模型并挂载日志监听器
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .listeners(List.of(chatModelListener))
                .build();
    }

}
