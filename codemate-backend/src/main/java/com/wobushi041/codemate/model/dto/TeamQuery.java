package com.wobushi041.codemate.model.dto;

import com.wobushi041.codemate.model.request.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 队伍查询条件数据对象
 *
 * @author wobushi041
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TeamQuery extends PageRequest {

    /**
     * 队伍 id
     */
    private Long id;

    /**
     * 队伍 id 列表
     */
    private List<Long> idList;

    /**
     * 搜索关键词
     */
    private String searchText;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 队伍描述
     */
    private String description;

    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 创建人用户 id
     */
    private Long userId;

    /**
     * 队伍状态 0 - 公开 1 - 私有 2 - 加密
     */
    private Integer status;

}
