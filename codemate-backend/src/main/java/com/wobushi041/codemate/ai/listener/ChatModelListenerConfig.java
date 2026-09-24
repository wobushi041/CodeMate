package com.wobushi041.codemate.ai.listener;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 模型调用日志监听器配置
 *
 * @author wobushi041
 */
@Configuration
@Slf4j
public class ChatModelListenerConfig {

    /**
     * 创建并注册聊天模型调用日志监听器
     *
     * @return 聊天模型监听器实例
     */
    @Bean
    ChatModelListener chatModelListener() {
        // 构建匿名监听器实例，记录请求、响应与异常日志
        return new ChatModelListener() {

            /**
             * 监听并记录 AI 模型请求日志
             *
             * @param requestContext 模型请求上下文
             */
            @Override
            public void onRequest(ChatModelRequestContext requestContext) {
                // 打印模型请求详情日志
                log.info("AI 请求: {}", requestContext.chatRequest());
            }

            /**
             * 监听并记录 AI 模型响应日志
             *
             * @param responseContext 模型响应上下文
             */
            @Override
            public void onResponse(ChatModelResponseContext responseContext) {
                // 打印模型响应详情日志
                log.info("AI 响应: {}", responseContext.chatResponse());
            }

            /**
             * 监听并记录 AI 模型调用异常日志
             *
             * @param errorContext 模型异常上下文
             */
            @Override
            public void onError(ChatModelErrorContext errorContext) {
                // 打印模型调用错误消息日志
                log.error("AI 错误: {}", errorContext.error().getMessage());
            }

        };
    }

}
