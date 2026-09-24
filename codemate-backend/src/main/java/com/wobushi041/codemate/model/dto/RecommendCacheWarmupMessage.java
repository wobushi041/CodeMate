package com.wobushi041.codemate.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 推荐缓存预热消息数据对象
 *
 * @author wobushi041
 */
@Data
public class RecommendCacheWarmupMessage implements Serializable {

    /**
     * 长期任务标识（由 userId、pageNum、pageSize 拼接而成）
     */
    private String taskId;

    /**
     * 单次投递运行标识
     */
    private String runId;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 分页页码
     */
    private long pageNum;

    /**
     * 每页记录数
     */
    private long pageSize;

    /**
     * 消息创建时间戳
     */
    private Long createTime;

    /// 序列化字段 ///
    private static final long serialVersionUID = 6419200407303929663L;

}
