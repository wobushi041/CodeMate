package com.wobushi041.codemate.ai.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ResourceUtils;

import jakarta.annotation.Resource;
import java.io.File;
import java.util.List;

/**
 * RAG 检索增强生成配置
 *
 * @author wobushi041
 */
@Configuration
@Slf4j
public class RagConfig {

    /**
     * 注入向量化模型依赖
     */
    @Resource
    private EmbeddingModel embeddingModel;

    /**
     * 注入内存向量存储依赖
     */
    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;

    /**
     * 构建并注册 RAG 内容检索器，加载本地编程文档并切段向量化入库
     *
     * @return 内容检索器实例；若文档目录不存在或向量化失败则返回 null
     */
    @Bean
    public ContentRetriever contentRetriever() {
        // 解析类路径下的 RAG 文档目录
        File docsDir;
        try {
            docsDir = ResourceUtils.getFile("classpath:docs");
        } catch (Exception e) {
            log.warn("未找到 RAG 文档目录 classpath:docs，跳过 RAG 初始化");
            return null;
        }

        // 校验文档目录是否存在且有效
        if (!docsDir.exists() || !docsDir.isDirectory()) {
            log.warn("RAG 文档目录不存在: {}，跳过 RAG 初始化", docsDir.getPath());
            return null;
        }

        // 加载目录下的全部文档并判空
        List<Document> documents = FileSystemDocumentLoader.loadDocuments(docsDir.toPath());
        if (documents.isEmpty()) {
            log.warn("RAG 文档目录为空，跳过向量化");
            return null;
        }

        // 配置文档段落切分器：每段最大 1000 个字符，重叠 200 个字符
        DocumentByParagraphSplitter splitter = new DocumentByParagraphSplitter(1000, 200);

        // 构建向量入库器：切段后拼接文件名并调用向量模型写入存储
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(splitter)
                .textSegmentTransformer(segment -> TextSegment.from(
                        segment.metadata().getString("file_name") + "\n" + segment.text(),
                        segment.metadata()))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        // 执行文档向量化入库
        try {
            ingestor.ingest(documents);
            log.info("RAG 文档加载完成，共 {} 篇文档", documents.size());
        } catch (Exception e) {
            log.error("RAG 文档向量化失败: {}", e.getMessage());
            return null;
        }

        // 构建并返回基于内存向量存储的内容检索器
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)
                .minScore(0.75)
                .build();
    }

}
