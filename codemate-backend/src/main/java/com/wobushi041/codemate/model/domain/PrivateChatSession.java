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
 * 单人私聊会话实体
 *
 * @author wobushi041
 */
@TableName(value = "private_chat_session")
@Data
public class PrivateChatSession implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 会话 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户1 ID (较小值)
     */
    private Long user1Id;

    /**
     * 用户2 ID (较大值)
     */
    private Long user2Id;

    /**
     * 最后一条消息摘要
     */
    private String lastMessage;

    /**
     * 最后消息时间
     */
    private Date lastMessageTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 逻辑删除 (0-未删除 1-已删除)
     */
    @TableLogic
    @TableField("isDelete")
    private Integer isDelete;
}
