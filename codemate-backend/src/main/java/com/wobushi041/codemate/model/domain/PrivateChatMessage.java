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
 * 单人私聊消息实体
 *
 * @author wobushi041
 */
@TableName(value = "private_chat_message")
@Data
public class PrivateChatMessage implements Serializable {

    /**
     * 消息 id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属会话 id
     */
    private Long sessionId;

    /**
     * 客户端防重追踪 id
     */
    private String clientMessageId;

    /**
     * 发送方用户 id
     */
    private Long fromUserId;

    /**
     * 接收方用户 id
     */
    private Long toUserId;

    /**
     * 消息正文
     */
    private String content;

    /**
     * 是否已读 0 - 未读 1 - 已读
     */
    private Integer isRead;

    /**
     * 发送时间
     */
    private Date createTime;

    /**
     * 逻辑删除 0 - 未删除 1 - 已删除
     */
    @TableLogic
    @TableField("isDelete")
    private Integer isDelete;

    /// 序列化字段 ///
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}
