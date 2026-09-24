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
 * 队伍聊天消息实体
 *
 * @author wobushi041
 */
@TableName(value = "chat_message")
@Data
public class ChatMessage implements Serializable {

    /**
     * 消息 id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属队伍 id
     */
    private Long teamId;

    /**
     * 客户端防重追踪 id
     */
    private String clientMessageId;

    /**
     * 发送方用户 id
     */
    private Long fromUserId;

    /**
     * 发送方用户昵称
     */
    private String fromUsername;

    /**
     * 消息正文内容
     */
    private String content;

    /**
     * 发送时间
     */
    private Date createTime;

    /**
     * 是否删除 0 - 未删除 1 - 已删除
     */
    @TableLogic
    @TableField("isDelete")
    private Integer isDelete;

    /// 序列化字段 ///
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}
