package com.wobushi041.codemate.chat;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Netty WebSocket 配置属性类
 *
 * @author wobushi041
 */
@Data
@ConfigurationProperties(prefix = "codemate.websocket")
public class NettyWebSocketProperties {

    /**
     * 是否启用 Netty WebSocket 服务，默认为 true
     */
    private boolean enabled = true;

    /**
     * Netty WebSocket 服务端监听端口，默认为 8091
     */
    private int port = 8091;

    /**
     * WebSocket 握手与通信端点访问路径，默认为 /ws/chat
     */
    private String path = "/ws/chat";

}
