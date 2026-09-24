package com.wobushi041.codemate.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wobushi041.codemate.exception.BusinessException;
import com.wobushi041.codemate.model.domain.User;
import com.wobushi041.codemate.service.ChatRoomService;
import com.wobushi041.codemate.service.PrivateChatService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 聊天室长连接与消息交互业务处理器
 *
 * @author wobushi041
 */
@Slf4j
@RequiredArgsConstructor
public class ChatConnectionHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    /**
     * 注入聊天室通道管理器依赖
     */
    private final ChatChannelManager chatChannelManager;

    /**
     * 注入队伍聊天室业务服务依赖
     */
    private final ChatRoomService chatRoomService;

    /**
     * 注入单人私聊业务服务依赖
     */
    private final PrivateChatService privateChatService;

    /**
     * 注入 Jackson JSON 序列化工具依赖
     */
    private final ObjectMapper objectMapper;

    /**
     * 捕获并处理 WebSocket 协议握手完成事件，绑定全局用户在线通道
     *
     * @param ctx 通道处理器上下文
     * @param evt 触发的事件对象
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // 监听到协议握手完成事件时，将已认证用户注册至全局在线通道路由表
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            User loginUser = ctx.channel().attr(ChatAttributes.LOGIN_USER).get();
            if (loginUser != null) {
                chatChannelManager.registerUserChannel(loginUser.getId(), ctx.channel());
                log.info("netty websocket connected & registered userChannel, userId={}", loginUser.getId());
            }
        }

        // 继续向后续 Handler 传播事件
        super.userEventTriggered(ctx, evt);
    }

    /**
     * 读取并分发客户端发送的 WebSocket 文本消息帧
     *
     * @param ctx   通道处理器上下文
     * @param frame 接收到的文本帧对象
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        try {
            // 反序列化上行消息体并获取当前通道绑定的登录用户
            ChatInboundMessage request = objectMapper.readValue(frame.text(), ChatInboundMessage.class);
            User loginUser = ctx.channel().attr(ChatAttributes.LOGIN_USER).get();

            // 分发处理加入队伍聊天室请求
            if ("JOIN".equalsIgnoreCase(request.getType())) {
                chatRoomService.joinTeamRoom(request.getTeamId(), loginUser, ctx.channel());
                return;
            }

            // 分发处理发送队伍聊天消息请求
            if ("CHAT".equalsIgnoreCase(request.getType())) {
                chatRoomService.sendRoomMessage(loginUser, ctx.channel(), request);
                return;
            }

            // 分发处理进入单人私聊房间请求，绑定单聊会话上下文
            if ("PRIVATE_JOIN".equalsIgnoreCase(request.getType())) {
                if (request.getSessionId() != null && request.getSessionId() > 0) {
                    privateChatService.ensureSessionParticipant(request.getSessionId(), loginUser.getId());
                    ctx.channel().attr(ChatAttributes.SESSION_ID).set(request.getSessionId());
                    ChatMessageResponse response = ChatMessageResponse.privateJoined(request.getSessionId());
                    ctx.writeAndFlush(new TextWebSocketFrame(objectMapper.writeValueAsString(response)));
                }
                return;
            }

            // 分发处理发送单人私聊消息请求
            if ("PRIVATE_CHAT".equalsIgnoreCase(request.getType())) {
                privateChatService.sendPrivateMessage(loginUser, ctx.channel(), request);
                return;
            }

            // 未匹配到支持的消息类型，回传错误提示
            writeError(ctx, "不支持的消息类型");
        } catch (BusinessException e) {
            // 捕获业务校验异常并回传具体错误原因
            writeError(ctx, e.getDescription());
        } catch (Exception e) {
            // 捕获反序列化或系统异常并回传通用格式错误提示
            log.error("handle websocket message failed", e);
            writeError(ctx, "消息格式错误");
        }
    }

    /**
     * 处理客户端通道断开事件，清理全局与房间内的残留连接
     *
     * @param ctx 通道处理器上下文
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 获取断开连接对应的登录用户并清理在线路由
        User loginUser = ctx.channel().attr(ChatAttributes.LOGIN_USER).get();
        if (loginUser != null) {
            // 移除全局用户在线通道
            chatChannelManager.removeUserChannel(loginUser.getId(), ctx.channel());

            // 若用户此前已加入队伍房间，同步退出队伍房间
            Long teamId = ctx.channel().attr(ChatAttributes.TEAM_ID).get();
            if (teamId != null) {
                chatChannelManager.leaveRoom(teamId, loginUser.getId(), ctx.channel());
            }
            log.info("netty websocket disconnected, userId={}", loginUser.getId());
        }

        // 继续向后续 Handler 传播断开事件
        super.channelInactive(ctx);
    }

    /**
     * 构造标准错误格式响应并写回给客户端
     *
     * @param ctx     通道处理器上下文
     * @param message 错误提示信息
     */
    private void writeError(ChannelHandlerContext ctx, String message) {
        // 封装错误响应对象
        ChatMessageResponse response = ChatMessageResponse.error(message);

        // 序列化为 JSON 文本帧并刷入通道
        try {
            ctx.writeAndFlush(new TextWebSocketFrame(objectMapper.writeValueAsString(response)));
        } catch (Exception e) {
            log.error("write websocket error response failed", e);
        }
    }

}
