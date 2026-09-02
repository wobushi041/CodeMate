package com.wobushi041.matchsystem.chat;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "match.websocket")
public class NettyWebSocketProperties {

    private boolean enabled = true;

    private int port = 8091;

    private String path = "/ws/chat";
}
