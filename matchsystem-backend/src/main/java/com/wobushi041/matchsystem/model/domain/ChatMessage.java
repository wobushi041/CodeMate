package com.wobushi041.matchsystem.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName(value = "chat_message")
@Data
public class ChatMessage implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long teamId;

    private String clientMessageId;

    private Long fromUserId;

    private String fromUsername;

    private String content;

    private Date createTime;

    @TableLogic
    @TableField("isDelete")
    private Integer isDelete;
}
