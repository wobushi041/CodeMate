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
 * 用户队伍关联实体
 *
 * @author wobushi041
 */
@TableName(value = "user_team")
@Data
public class UserTeam implements Serializable {

    /**
     * 主键 id
     */
    @TableId(type = IdType.AUTO)
    private long id;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 队伍 id
     */
    private Long teamId;

    /**
     * 加入时间
     */
    private Date joinTime;

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
    private static final long serialVersionUID = -8174351511473322464L;

}