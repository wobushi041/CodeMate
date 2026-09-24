package com.wobushi041.codemate.ai;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 编程助手服务工厂配置
 *
 * @author wobushi041
 */
@Configuration
public class AiChatServiceFactory {

    /**
     * 注入 DeepSeek 普通聊天模型依赖
     */
    @Autowired
    private ChatModel deepSeekChatModel;

    /**
     * 注入 DeepSeek 流式聊天模型依赖
     */
    @Autowired
    private StreamingChatModel deepSeekStreamingChatModel;

    /**
     * 注入 RAG 内容检索器依赖
     */
    @Autowired(required = false)
    private ContentRetriever contentRetriever;

    /**
     * 构建并注册 AI 编程助手服务实例
     *
     * @return AI 编程助手服务实例
     */
    @Bean
    public AiChatService aiChatService() {
        // 构建 AI 服务基础配置，组装聊天模型与滑动窗口会话记忆
        var builder = AiServices.builder(AiChatService.class)
                .chatModel(deepSeekChatModel)
                .streamingChatModel(deepSeekStreamingChatModel)
                .chatMemoryProvider(memoryId ->
                        MessageWindowChatMemory.withMaxMessages(20));

        // 若 RAG 内容检索器可用，则挂载检索增强能力
        if (contentRetriever != null) {
            builder.contentRetriever(contentRetriever);
        }

        // 完成 AI 服务实例构建并返回
        return builder.build();
    }

}
