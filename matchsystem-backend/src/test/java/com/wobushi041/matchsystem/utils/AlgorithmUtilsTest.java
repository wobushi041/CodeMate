package com.wobushi041.matchsystem.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class AlgorithmUtilsTest {

    @Test
    void calculatesJaccardSimilarityByIntersectionOverUnion() {
        double score = AlgorithmUtils.calculateJaccardSimilarity(
                List.of("java", "后端", "redis"),
                List.of("java", "后端", "mysql")
        );

        Assertions.assertEquals(0.5D, score);
    }

    @Test
    void calculatesCosineSimilarityByTermFrequencyVector() {
        double score = AlgorithmUtils.cosineSimilarity(
                List.of("java", "java", "redis"),
                List.of("java", "redis", "redis")
        );

        Assertions.assertEquals(0.8D, score, 0.000001D);
    }

    @Test
    void extractsChineseCharactersFromTagList() {
        String result = AlgorithmUtils.collectChineseChars(List.of("\"Java后端\"", "\"Redis缓存\""));

        Assertions.assertEquals("后端缓存", result);
    }

    @Test
    void removesChineseCharactersFromMixedTags() {
        List<String> result = AlgorithmUtils.tokenize(List.of("Java后端", "Redis缓存"));

        Assertions.assertEquals(List.of("java", "redis"), result);
    }
}
