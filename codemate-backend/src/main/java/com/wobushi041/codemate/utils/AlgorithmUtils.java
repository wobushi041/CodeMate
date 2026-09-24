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

/**
 * 文本相似度与编辑距离算法工具类
 *
 * @author wobushi041
 */
public class AlgorithmUtils {

    /**
     * 中英文混合文本类型标识
     */
    public static final int MIXED_CHINESE_ENGLISH = 0;

    /**
     * 纯中文文本类型标识
     */
    public static final int CHINESE = 1;

    /**
     * 纯英文文本类型标识
     */
    public static final int ENGLISH = 2;

    /**
     * 中文字符匹配正则表达式
     */
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]+");

    /**
     * 英文字符匹配正则表达式
     */
    private static final Pattern ENGLISH_PATTERN = Pattern.compile("[a-zA-Z]+");

    /**
     * Elasticsearch REST 客户端实例
     */
    private static final RestClient ES_CLIENT = RestClient.builder(new HttpHost("localhost", 9200, "http")).build();

    /**
     * 计算两个用户标签列表之间的最小编辑距离
     *
     * @param tagList1 第一个用户的标签列表
     * @param tagList2 第二个用户的标签列表
     * @return 两个标签列表之间的最小编辑距离
     */
    public static int minDistance(List<String> tagList1, List<String> tagList2) {
        // 判空处理，任一列表为 null 时返回非空列表长度之和
        if (tagList1 == null || tagList2 == null) {
            return (tagList1 == null ? 0 : tagList1.size()) +
                    (tagList2 == null ? 0 : tagList2.size());
        }

        // 处理任一标签列表为空集合的边界情况
        int n = tagList1.size();
        int m = tagList2.size();
        if (n * m == 0) {
            return n + m;
        }

        // 初始化动态规划数组与第一行、第一列边界条件
        int[][] d = new int[n + 1][m + 1];
        for (int i = 0; i < n + 1; i++) {
            d[i][0] = i;
        }
        for (int j = 0; j < m + 1; j++) {
            d[0][j] = j;
        }

        // 使用动态规划算法填充状态转移矩阵
        for (int i = 1; i < n + 1; i++) {
            for (int j = 1; j < m + 1; j++) {
                int left = d[i - 1][j] + 1; // 插入操作开销
                int down = d[i][j - 1] + 1; // 删除操作开销
                int left_down = d[i - 1][j - 1]; // 替换操作开销
                if (!Objects.equals(tagList1.get(i - 1), tagList2.get(j - 1))) {
                    left_down += 1; // 元素不同时增加一次替换代价
                }
                d[i][j] = Math.min(left, Math.min(down, left_down));
            }
        }
        return d[n][m];
    }

    /**
     * 计算两个标签列表的编辑距离（保留兼容调用入口）
     *
     * @param tagList1 第一个标签列表
     * @param tagList2 第二个标签列表
     * @return 两个标签列表之间的最小编辑距离
     */
    public static int calculateEditDistance(List<String> tagList1, List<String> tagList2) {
        // 委托调用最小编辑距离计算方法
        return minDistance(tagList1, tagList2);
    }

    /**
     * 判断字符串列表的语言构成类型
     *
     * @param strings 待检测的字符串列表
     * @return 语言构成类型常量（中英混合、纯中文或纯英文）
     */
    public static int getStrType(List<String> strings) {
        // 统计列表中包含中英混合与纯中文的字符串数量
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
                }
                // 当前字符串仅含中文不含英文
                else {
                    chineseCount++;
                }
            }
        }

        // 根据统计计数判定整体文本类型
        if (mixedChineseEnglishCount != 0) {
            return MIXED_CHINESE_ENGLISH;
        }
        if (chineseCount != 0) {
            return CHINESE;
        }
        return ENGLISH;
    }

    /**
     * 剔除中英文混合标签中的中文字符并提取英文词元列表
     *
     * @param strings 原始标签字符串列表
     * @return 转小写后的英文词元列表
     */
    public static List<String> tokenize(List<String> strings) {
        // 校验输入列表是否为空
        List<String> result = new ArrayList<>();
        if (strings == null) {
            return result;
        }

        // 按中文字符切分字符串并提取非空英文词元
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
     * 计算两个字符串列表的 Jaccard 相似度
     *
     * @param list1 第一个字符串列表
     * @param list2 第二个字符串列表
     * @return 交集大小除以并集大小的 Jaccard 相似度值
     */
    public static double calculateJaccardSimilarity(List<String> list1, List<String> list2) {
        // 校验入参列表是否为空
        if (list1 == null || list2 == null || list1.isEmpty() && list2.isEmpty()) {
            return 0D;
        }

        // 构建集合并计算交集与并集
        Set<String> set1 = new HashSet<>(list1);
        Set<String> set2 = new HashSet<>(list2);
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        // 计算并返回交并比
        if (union.isEmpty()) {
            return 0D;
        }
        return (double) intersection.size() / union.size();
    }

    /**
     * 根据标签词频向量计算两个列表的余弦相似度
     *
     * @param list1 第一个字符串列表
     * @param list2 第二个字符串列表
     * @return 两个词频向量的余弦相似度值
     */
    public static double cosineSimilarity(List<String> list1, List<String> list2) {
        // 校验入参列表是否为空
        if (list1 == null || list2 == null || list1.isEmpty() || list2.isEmpty()) {
            return 0D;
        }

        // 构建词频向量并计算向量点积
        Map<String, Integer> vector1 = buildTermFrequencyVector(list1);
        Map<String, Integer> vector2 = buildTermFrequencyVector(list2);
        double dotProduct = 0D;
        for (String term : vector1.keySet()) {
            if (vector2.containsKey(term)) {
                dotProduct += vector1.get(term) * vector2.get(term);
            }
        }

        // 计算两个词频向量的模长并返回夹角余弦值
        double norm1 = vector1.values().stream().mapToDouble(value -> Math.pow(value, 2)).sum();
        double norm2 = vector2.values().stream().mapToDouble(value -> Math.pow(value, 2)).sum();
        if (norm1 == 0D || norm2 == 0D) {
            return 0D;
        }
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 根据字符串列表构建词频统计向量
     *
     * @param list 待统计的字符串列表
     * @return 词元到出现频次的映射表
     */
    private static Map<String, Integer> buildTermFrequencyVector(List<String> list) {
        // 遍历列表统计非空白词元的出现次数
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
     * 调用 Elasticsearch IK 分词器对中文文本进行分词
     *
     * @param text 待分词的中文文本
     * @return 分词后的词元列表
     */
    public static List<String> analyzeText(String text) throws IOException {
        // 校验待分词文本是否为空白
        List<String> tokens = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return tokens;
        }

        // 构造并发送 Elasticsearch IK 分词请求
        Request request = new Request("POST", "/_analyze");
        request.setJsonEntity("{\"analyzer\":\"ik_max_word\",\"text\":\"" + escapeJson(text) + "\"}");
        Response response = ES_CLIENT.performRequest(request);

        // 解析响应 JSON 并提取分词结果列表
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
     * 提取标签列表中的所有中文字符并拼接为字符串
     *
     * @param list 标签字符串列表
     * @return 拼接后的纯中文字符串
     */
    public static String collectChineseChars(List<String> list) {
        // 校验输入列表是否为空
        if (list == null || list.isEmpty()) {
            return "";
        }

        // 逐字符扫描并收集 Unicode 中文字符区间内的字符
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

    /**
     * 转义 JSON 字符串中的反斜杠与双引号
     *
     * @param text 原始文本字符串
     * @return 转义后的安全 JSON 字符串
     */
    private static String escapeJson(String text) {
        // 替换反斜杠与双引号字符
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * 计算两个字符串之间的最小编辑距离
     *
     * @param word1 第一个字符串
     * @param word2 第二个字符串
     * @return 两个字符串之间的最小编辑距离
     */
    public static int minDistance(String word1, String word2) {
        // 处理任一字符串为空的边界情况
        int n = word1.length();
        int m = word2.length();
        if (n * m == 0) {
            return n + m;
        }

        // 初始化动态规划状态数组及边界值
        int[][] d = new int[n + 1][m + 1];
        for (int i = 0; i < n + 1; i++) {
            d[i][0] = i;
        }
        for (int j = 0; j < m + 1; j++) {
            d[0][j] = j;
        }

        // 使用动态规划计算字符串最小编辑距离
        for (int i = 1; i < n + 1; i++) {
            for (int j = 1; j < m + 1; j++) {
                int left = d[i - 1][j] + 1; // 插入字符开销
                int down = d[i][j - 1] + 1; // 删除字符开销
                int left_down = d[i - 1][j - 1]; // 替换字符开销
                if (word1.charAt(i - 1) != word2.charAt(j - 1)) {
                    left_down += 1; // 字符不同时增加替换代价
                }
                d[i][j] = Math.min(left, Math.min(down, left_down));
            }
        }
        return d[n][m];
    }

}
