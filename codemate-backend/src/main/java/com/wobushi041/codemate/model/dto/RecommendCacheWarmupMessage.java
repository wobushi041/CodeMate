package com.wobushi041.codemate.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * RecommendCacheWarmupMessage类用于推荐缓存预热消息的封装，实现了Serializable接口以支持序列化。
 * 该类包含了任务标识、运行标识、用户ID、分页信息以及创建时间等关键字段。
 * 使用@Data注解来自动生成getter、setter、toString等方法。
 */
@Data
public class RecommendCacheWarmupMessage implements Serializable {

    /**
     * 序列化版本UID，用于确保序列化和反序列化时的版本一致性。
     */
    private static final long serialVersionUID = 6419200407303929663L;

    /**
     * 长期任务标识，相同 userId/pageNum/pageSize 保持不变。
     * 由userId，pageNum，pageSize拼接而成
     */
    private String taskId;

    /**
     * 单次投递标识，方便追踪一条具体消息。
     */
    private String runId;

    /**
     * 用户ID，用于标识特定的用户。
     */
    private Long userId;

    /**
     * 页码，用于分页查询。
     */
    private long pageNum;

    /**
     * 每页大小，用于控制每页返回的数据量。
     */
    private long pageSize;

    /**
     * 创建时间，记录消息创建的时间戳。
     */
    private Long createTime;
}
