package com.wobushi041.matchsystem.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wobushi041.matchsystem.exception.BusinessException;
import com.wobushi041.matchsystem.model.domain.User;
import com.wobushi041.matchsystem.service.ChatRoomService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ChatConnectionHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    //构造器方式注入,这里用到@requiredArgsConstructor注解,来源于lombok
    private final ChatChannelManager chatChannelManager;
    private final ChatRoomService chatRoomService;
    private final ObjectMapper objectMapper;

    @Override
    //父类提供一个扩展父类，重写父类让用户可以自定义处理各种通道事件
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            User loginUser = ctx.channel().attr(ChatAttributes.LOGIN_USER).get();
            if (loginUser != null) {
                log.info("netty websocket connected, userId={}", loginUser.getId());
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override    // 重写父类方法的注解
    /**
     * 处理WebSocket文本消息的方法
     * @param ctx ChannelHandlerContext对象，提供Channel相关的操作
     * @param frame 接收到的TextWebSocketFrame消息对象
     */
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        try {
            // 使用Jackson将消息体转换为ChatInboundMessage对象，反序列化
            ChatInboundMessage request = objectMapper.readValue(frame.text(), ChatInboundMessage.class);
            // 从Channel中获取已登录的用户信息
            User loginUser = ctx.channel().attr(ChatAttributes.LOGIN_USER).get();
            // 处理加入聊天室类型的消息，真正干活的业务层是chatRoomServiceImpl
            if ("JOIN".equalsIgnoreCase(request.getType())) {
                chatRoomService.joinTeamRoom(request.getTeamId(), loginUser, ctx.channel());
                return;
            }
            // 处理聊天消息类型的消息
            if ("CHAT".equalsIgnoreCase(request.getType())) {
                chatRoomService.sendRoomMessage(loginUser, ctx.channel(), request);
                return;
            }
            // 如果消息类型不支持，返回错误信息
            writeError(ctx, "不支持的消息类型");
        } catch (BusinessException e) {
            // 处理业务异常，返回错误描述
            writeError(ctx, e.getDescription());
        } catch (Exception e) {
            // 处理其他异常，记录错误日志并返回错误信息
            log.error("handle websocket message failed", e);
            writeError(ctx, "消息格式错误");
        }
    }

    @Override
    /**
     * 当通道变为非活跃状态时调用此方法
     * 这通常意味着客户端已经断开连接
     * @param ChannelHandlerContext ctx 通道处理器上下文，包含通道信息和相关操作
     * @throws Exception 可能抛出的异常
     */
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 从通道上下文中获取已登录的用户信息
        User loginUser = ctx.channel().attr(ChatAttributes.LOGIN_USER).get();
        // 如果用户已登录（不为null）
        if (loginUser != null) {
            // 从聊天通道管理器中移除该用户的通道连接
            Long teamId = ctx.channel().attr(ChatAttributes.TEAM_ID).get();
            if (teamId != null) {
                chatChannelManager.leaveRoom(teamId, loginUser.getId(), ctx.channel());
            }
            // 记录用户断开连接的日志，包含用户ID信息
            log.info("netty websocket disconnected, userId={}", loginUser.getId());
        }
        // 调用父类的channelInactive方法，确保父类的处理逻辑也能执行
        super.channelInactive(ctx);
    }

    private void writeError(ChannelHandlerContext ctx, String message) {
        ChatMessageResponse response = ChatMessageResponse.error(message);
        try {
            ctx.writeAndFlush(new TextWebSocketFrame(objectMapper.writeValueAsString(response)));
        } catch (Exception e) {
            log.error("write websocket error response failed", e);
        }
    }
}
