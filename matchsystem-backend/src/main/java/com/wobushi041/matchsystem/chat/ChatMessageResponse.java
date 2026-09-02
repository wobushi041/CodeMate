package com.wobushi041.matchsystem.chat;

import lombok.Data;

import java.io.Serializable;

@Data
public class ChatMessageResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String type;

    private Long teamId;

    private Long messageId;

    private String clientMessageId;

    private Long fromUserId;

    private String fromUsername;

    private String content;

    private String message;

    private String createTime;

    public static ChatMessageResponse error(String message) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setType("ERROR");
        response.setMessage(message);
        return response;
    }

    public static ChatMessageResponse joined(Long teamId) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setType("JOINED");
        response.setTeamId(teamId);
        return response;
    }
}
