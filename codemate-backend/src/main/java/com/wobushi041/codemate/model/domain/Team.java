package com.wobushi041.codemate.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 队伍实体
 *
 * @author wobushi041
 */
@TableName(value = "team")
@Data
public class Team implements Serializable {

    /**
     * 队伍 id
     */
    @TableId(type = IdType.AUTO)
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
     * 队长用户 id
     */
    private Long userId;

    /**
     * 队伍状态 0 - 公开 1 - 私有 2 - 加密
     */
    private Integer status;

    /**
     * 队伍密码（VO 不暴露）
     */
    private String password;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除 0 - 未删除 1 - 已删除
     */
    @TableLogic
    @TableField("isDelete")
    private Integer isDelete;

    /// 序列化字段 ///
    @TableField(exist = false)
    private static final long serialVersionUID = 3673266922282662709L;

}