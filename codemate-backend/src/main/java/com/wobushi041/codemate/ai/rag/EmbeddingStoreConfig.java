package com.wobushi041.codemate.ai.rag;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 内存向量存储配置
 *
 * @author wobushi041
 */
@Configuration
public class EmbeddingStoreConfig {

    /**
     * 构建并注册基于内存的文本切片向量存储实例
     *
     * @return 内存向量存储实例
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        // 初始化内存向量存储实例
        return new InMemoryEmbeddingStore<>();
    }

}
