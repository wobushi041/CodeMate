package com.wobushi041.codemate.ai.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;

import java.util.Set;

/**
 * 输入安全过滤护轨
 *
 * @author wobushi041
 */
public class SafeInputGuardrail implements InputGuardrail {

    /**
     * 敏感词黑名单集合
     */
    private static final Set<String> SENSITIVE_WORDS = Set.of(
            "kill", "evil", "hack", "attack", "exploit",
            "暴力", "色情", "赌博", "毒品"
    );

    /**
     * 校验用户输入消息是否包含敏感词
     *
     * @param userMessage 用户输入消息
     * @return 护轨校验结果
     */
    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        // 提取并标准化用户输入文本，按非单词字符切分词项
        String inputText = userMessage.singleText().toLowerCase();
        String[] words = inputText.split("\\W+");

        // 遍历词项检测敏感词，命中则立即拦截
        for (String word : words) {
            if (SENSITIVE_WORDS.contains(word)) {
                return fatal("检测到敏感词: " + word);
            }
        }

        // 校验通过，放行用户输入
        return success();
    }

}
