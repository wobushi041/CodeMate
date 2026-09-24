package com.wobushi041.codemate.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * 算法工具类单元测试
 *
 * @author wobushi041
 */
class AlgorithmUtilsTest {

    /**
     * 测试通过交并比计算 Jaccard 相似度
     */
    // 场景：测试两个标签列表基于交集除以并集计算 Jaccard 相似度
    @Test
    void calculatesJaccardSimilarityByIntersectionOverUnion() {
        // 1. 准备测试数据并调用待测方法
        double score = AlgorithmUtils.calculateJaccardSimilarity(
                List.of("java", "后端", "redis"),
                List.of("java", "后端", "mysql")
        );

        // 2. 断言结果
        Assertions.assertEquals(0.5D, score);
    }

    /**
     * 测试通过词频向量计算余弦相似度
     */
    // 场景：测试两个标签词元列表基于词频向量计算余弦相似度
    @Test
    void calculatesCosineSimilarityByTermFrequencyVector() {
        // 1. 准备测试数据并调用待测方法
        double score = AlgorithmUtils.cosineSimilarity(
                List.of("java", "java", "redis"),
                List.of("java", "redis", "redis")
        );

        // 2. 断言结果
        Assertions.assertEquals(0.8D, score, 0.000001D);
    }

    /**
     * 测试从标签列表中提取中文字符
     */
    // 场景：测试从带引号的中英混合标签列表中提取纯中文字符序列
    @Test
    void extractsChineseCharactersFromTagList() {
        // 1. 准备测试数据并调用待测方法
        String result = AlgorithmUtils.collectChineseChars(List.of("\"Java后端\"", "\"Redis缓存\""));

        // 2. 断言结果
        Assertions.assertEquals("后端缓存", result);
    }

    /**
     * 测试从中英混合标签中移除中文字符并提取小写英文词元
     */
    // 场景：测试从中英混合标签列表中过滤中文字符并分词为小写英文标签
    @Test
    void removesChineseCharactersFromMixedTags() {
        // 1. 准备测试数据并调用待测方法
        List<String> result = AlgorithmUtils.tokenize(List.of("Java后端", "Redis缓存"));

        // 2. 断言结果
        Assertions.assertEquals(List.of("java", "redis"), result);
    }

}
