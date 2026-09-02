package com.wobushi041.matchsystem.chat;

import lombok.Data;

import java.io.Serializable;

@Data
public class ChatInboundMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String type;

    private Long teamId;

    private String clientMessageId;

    private String content;
}
