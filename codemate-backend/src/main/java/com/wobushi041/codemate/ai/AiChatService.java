package com.wobushi041.codemate.ai;

import com.wobushi041.codemate.ai.guardrail.SafeInputGuardrail;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.guardrail.InputGuardrails;
import reactor.core.publisher.Flux;

/**
 * AI 编程助手服务接口
 *
 * @author wobushi041
 */
@InputGuardrails({SafeInputGuardrail.class})
public interface AiChatService {

    /**
     * 执行流式对话（SSE）
     *
     * @param memoryId    会话 ID，用于隔离不同用户的对话记忆
     * @param userMessage 用户消息（已注入 tags 上下文）
     * @return 流式响应文本数据流
     */
    @SystemMessage(fromResource = "system-prompt.txt")
    Flux<String> chatStream(@MemoryId int memoryId, @UserMessage String userMessage);

    /**
     * 执行普通非流式对话
     *
     * @param userMessage 用户消息
     * @return AI 回复内容
     */
    @SystemMessage(fromResource = "system-prompt.txt")
    String chat(String userMessage);

    /**
     * 执行 RAG 增强对话并返回检索来源信息
     *
     * @param userMessage 用户消息
     * @return AI 回复内容及检索来源封装结果
     */
    @SystemMessage(fromResource = "system-prompt.txt")
    Result<String> chatWithRag(String userMessage);

}
