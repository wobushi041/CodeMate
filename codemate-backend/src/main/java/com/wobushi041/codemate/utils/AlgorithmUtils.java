package com.wobushi041.codemate.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.http.HttpHost;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AlgorithmUtils {

    public static final int MIXED_CHINESE_ENGLISH = 0;
    public static final int CHINESE = 1;
    public static final int ENGLISH = 2;

    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]+");
    private static final Pattern ENGLISH_PATTERN = Pattern.compile("[a-zA-Z]+");
    private static final RestClient ES_CLIENT = RestClient.builder(new HttpHost("localhost", 9200, "http")).build();

    /**
     * 计算两个用户的标签列表之间的最小编辑距离。
     * @param tagList1 第一个用户的标签列表。
     * @param tagList2 第二个用户的标签列表。
     * @return 两个标签列表之间的最小编辑距离。
     */
    public static int minDistance(List<String> tagList1, List<String> tagList2) {


        if (tagList1 == null || tagList2 == null) {
            // 如果任一列表为null，返回最大可能距离（或根据业务需求调整）
            return (tagList1 == null ? 0 : tagList1.size()) +
                    (tagList2 == null ? 0 : tagList2.size());
        }
        int n = tagList1.size();
        int m = tagList2.size();

        // 处理任一标签列表为空的情况
        if (n * m == 0) {
            return n + m;
        }

        int[][] d = new int[n + 1][m + 1];

        // 初始化边界条件，单个列表到空列表的编辑距离
        for (int i = 0; i < n + 1; i++) {
            d[i][0] = i;
        }
        for (int j = 0; j < m + 1; j++) {
            d[0][j] = j;
        }

        // 使用动态规划算法填充表格
        for (int i = 1; i < n + 1; i++) {
            for (int j = 1; j < m + 1; j++) {
                int left = d[i - 1][j] + 1; // 插入操作
                int down = d[i][j - 1] + 1; // 删除操作
                int left_down = d[i - 1][j - 1]; // 替换操作
                if (!Objects.equals(tagList1.get(i - 1), tagList2.get(j - 1))) {
                    left_down += 1; // 不同字符需替换
                }
                d[i][j] = Math.min(left, Math.min(down, left_down));
            }
        }
        return d[n][m];
    }

    /**
     * friend 项目里的编辑距离方法名，保留同名调用入口。
     */
    public static int calculateEditDistance(List<String> tagList1, List<String> tagList2) {
        return minDistance(tagList1, tagList2);
    }

    /**
     * 判断标签列表的文本类型。
     */
    public static int getStrType(List<String> strings) {
        int mixedChineseEnglishCount = 0;
        int chineseCount = 0;
        for (String str : strings) {
            if (str == null) {
                continue;
            }
            Matcher chineseMatcher = CHINESE_PATTERN.matcher(str);
            if (chineseMatcher.find()) {
                Matcher englishMatcher = ENGLISH_PATTERN.matcher(str);
                if (englishMatcher.find()) {
                    mixedChineseEnglishCount++;
                } else {
                    chineseCount++;
                }
            }
        }
        if (mixedChineseEnglishCount != 0) {
            return MIXED_CHINESE_ENGLISH;
        }
        if (chineseCount != 0) {
            return CHINESE;
        }
        return ENGLISH;
    }

    /**
     * 将中英文混合标签中的中文剔除，保留英文部分参与常规相似度计算。
     */
    public static List<String> tokenize(List<String> strings) {
        List<String> result = new ArrayList<>();
        if (strings == null) {
            return result;
        }
        for (String string : strings) {
            if (string == null) {
                continue;
            }
            String[] words = string.split("[\\u4e00-\\u9fa5]+");
            for (String word : words) {
                if (!word.isBlank()) {
                    result.add(word.toLowerCase(Locale.ROOT));
                }
            }
        }
        return result;
    }

    /**
     * Jaccard 相似度：交集大小 / 并集大小。
     */
    public static double calculateJaccardSimilarity(List<String> list1, List<String> list2) {
        if (list1 == null || list2 == null || list1.isEmpty() && list2.isEmpty()) {
            return 0D;
        }
        Set<String> set1 = new HashSet<>(list1);
        Set<String> set2 = new HashSet<>(list2);
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        if (union.isEmpty()) {
            return 0D;
        }
        return (double) intersection.size() / union.size();
    }

    /**
     * 余弦相似度：根据标签词频向量计算夹角余弦。
     */
    public static double cosineSimilarity(List<String> list1, List<String> list2) {
        if (list1 == null || list2 == null || list1.isEmpty() || list2.isEmpty()) {
            return 0D;
        }
        Map<String, Integer> vector1 = buildTermFrequencyVector(list1);
        Map<String, Integer> vector2 = buildTermFrequencyVector(list2);
        double dotProduct = 0D;
        for (String term : vector1.keySet()) {
            if (vector2.containsKey(term)) {
                dotProduct += vector1.get(term) * vector2.get(term);
            }
        }
        double norm1 = vector1.values().stream().mapToDouble(value -> Math.pow(value, 2)).sum();
        double norm2 = vector2.values().stream().mapToDouble(value -> Math.pow(value, 2)).sum();
        if (norm1 == 0D || norm2 == 0D) {
            return 0D;
        }
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    private static Map<String, Integer> buildTermFrequencyVector(List<String> list) {
        Map<String, Integer> vector = new HashMap<>();
        for (String item : list) {
            if (item == null || item.isBlank()) {
                continue;
            }
            vector.merge(item, 1, Integer::sum);
        }
        return vector;
    }

    /**
     * 通过 Elasticsearch IK 分词器对中文文本做分词。
     */
    public static List<String> analyzeText(String text) throws IOException {
        List<String> tokens = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return tokens;
        }
        Request request = new Request("POST", "/_analyze");
        request.setJsonEntity("{\"analyzer\":\"ik_max_word\",\"text\":\"" + escapeJson(text) + "\"}");
        Response response = ES_CLIENT.performRequest(request);
        String responseBody = EntityUtils.toString(response.getEntity());
        JsonArray tokenArray = JsonParser.parseString(responseBody).getAsJsonObject().getAsJsonArray("tokens");
        for (JsonElement tokenElement : tokenArray) {
            String term = tokenElement.getAsJsonObject().get("token").getAsString();
            if (!term.isBlank()) {
                tokens.add(term);
            }
        }
        return tokens;
    }

    /**
     * 提取标签列表里的中文字符，供 IK 分词使用。
     */
    public static String collectChineseChars(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        StringBuilder chineseChars = new StringBuilder();
        for (String str : list) {
            if (str == null) {
                continue;
            }
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                if (c >= '\u4E00' && c <= '\u9FFF') {
                    chineseChars.append(c);
                }
            }
        }
        return chineseChars.toString();
    }

    private static String escapeJson(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * 计算两个单词或字符串之间的最小编辑距离。
     * @param word1 第一个字符串。
     * @param word2 第二个字符串。
     * @return 两个字符串之间的最小编辑距离。
     */
    public static int minDistance(String word1, String word2) {
        int n = word1.length();
        int m = word2.length();

        // 处理任一字符串为空的情况
        if (n * m == 0) {
            return n + m;
        }

        int[][] d = new int[n + 1][m + 1];

        // 初始化边界条件
        for (int i = 0; i < n + 1; i++) {
            d[i][0] = i;
        }
        for (int j = 0; j < m + 1; j++) {
            d[0][j] = j;
        }

        // 动态规划解决问题
        for (int i = 1; i < n + 1; i++) {
            for (int j = 1; j < m + 1; j++) {
                int left = d[i - 1][j] + 1; // 插入
                int down = d[i][j - 1] + 1; // 删除
                int left_down = d[i - 1][j - 1]; // 替换
                if (word1.charAt(i - 1) != word2.charAt(j - 1)) {
                    left_down += 1; // 需要替换
                }
                d[i][j] = Math.min(left, Math.min(down, left_down));
            }
        }
        return d[n][m];
    }

}

