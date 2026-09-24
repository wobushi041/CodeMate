package com.wobushi041.codemate.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 队伍用户信息视图对象
 *
 * @author wobushi041
 */
@Data
public class TeamUserVO implements Serializable {

    /**
     * 队伍 id
     */
    private Long id;

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
     * 过期时间
     */
    private Date expireTime;

    /**
     * 创建人用户 id
     */
    private Long userId;

    /**
     * 队伍状态 0 - 公开 1 - 私有 2 - 加密
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 创建人用户信息
     */
    private UserVO createUser;

    /**
     * 已加入队伍的用户数量
     */
    private Integer hasJoinNum;

    /**
     * 当前用户是否已加入该队伍
     */
    private boolean hasJoin = false;

    /// 序列化字段 ///
    private static final long serialVersionUID = 9138973079051132588L;

}