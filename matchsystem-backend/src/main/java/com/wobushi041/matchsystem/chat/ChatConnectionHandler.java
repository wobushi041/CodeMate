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

/**
 * 聊天室长连接与消息交互业务处理器
 *
 * 继承自 Netty 的 SimpleChannelInboundHandler，专门负责处理已升级成功的 WebSocket 文本帧（TextWebSocketFrame）。
 *
 * 核心职责：
 * - 监听握手完成事件（HandshakeComplete）记录用户连接日志；
 * - 反序列化客户端请求帧并根据消息类型（JOIN / CHAT）分发调用聊天室业务服务；
 * - 监听连接断开事件（channelInactive），自动执行退房与在线通道释放；
 * - 捕获业务异常与协议解析异常，格式化构建统一错误响应帧回传至客户端。
 *
 * @author 硫酸铜
 */
@Slf4j
@RequiredArgsConstructor
public class ChatConnectionHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    /**
     * 聊天室通道管理器
     */
    private final ChatChannelManager chatChannelManager;

    /**
     * 聊天室业务层接口 (队伍群聊)
     */
    private final ChatRoomService chatRoomService;

    /**
     * 单人私聊业务层接口
     */
    private final com.wobushi041.matchsystem.service.PrivateChatService privateChatService;

    /**
     * Jackson JSON 序列化与反序列化工具
     */
    private final ObjectMapper objectMapper;

    /**
     * 捕获并处理用户自定义事件
     *
     * 当接收到协议握手完成事件 HandshakeComplete 时，
     * 读取此前认证绑定的用户信息，通过 userId 注册到全局 userChannels 路由表，并打印上线日志。
     *
     * @param ctx 通道处理器上下文
     * @param evt 触发的事件对象
     * @throws Exception 异常
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            User loginUser = ctx.channel().attr(ChatAttributes.LOGIN_USER).get();
            if (loginUser != null) {
                // 单方开启 Channel：通过 userId 绑定全局路由映射，无需强制进房即可双向寻址
                chatChannelManager.registerUserChannel(loginUser.getId(), ctx.channel());
                log.info("netty websocket connected & registered userChannel, userId={}", loginUser.getId());
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    /**
     * 读取并处理客户端发送的 WebSocket 文本消息帧
     *
     * @param ctx   通道处理器上下文
     * @param frame 接收到的文本帧对象
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        try {
            // 1. 反序列化消息体为 ChatInboundMessage
            ChatInboundMessage request = objectMapper.readValue(frame.text(), ChatInboundMessage.class);
            // 2. 从 Channel 属性中获取握手阶段绑定的登录用户
            User loginUser = ctx.channel().attr(ChatAttributes.LOGIN_USER).get();

            // 3. 分发处理加入队伍聊天室请求
            if ("JOIN".equalsIgnoreCase(request.getType())) {
                chatRoomService.joinTeamRoom(request.getTeamId(), loginUser, ctx.channel());
                return;
            }

            // 4. 分发处理发送队伍聊天消息请求
            if ("CHAT".equalsIgnoreCase(request.getType())) {
                chatRoomService.sendRoomMessage(loginUser, ctx.channel(), request);
                return;
            }

            // 5. 分发处理进入单人私聊房间请求 (绑定单聊会话上下文)
            if ("PRIVATE_JOIN".equalsIgnoreCase(request.getType())) {
                if (request.getSessionId() != null && request.getSessionId() > 0) {
                    privateChatService.ensureSessionParticipant(request.getSessionId(), loginUser.getId());
                    ctx.channel().attr(ChatAttributes.SESSION_ID).set(request.getSessionId());
                    ChatMessageResponse response = ChatMessageResponse.privateJoined(request.getSessionId());
                    ctx.writeAndFlush(new TextWebSocketFrame(objectMapper.writeValueAsString(response)));
                }
                return;
            }

            // 6. 分发处理发送单人私聊消息请求 (支持离线持久化与在线点对点直推)
            if ("PRIVATE_CHAT".equalsIgnoreCase(request.getType())) {
                privateChatService.sendPrivateMessage(loginUser, ctx.channel(), request);
                return;
            }

            // 7. 不支持的消息类型提示
            writeError(ctx, "不支持的消息类型");
        } catch (BusinessException e) {
            // 捕获业务校验异常（如未加入队伍、消息为空等）
            writeError(ctx, e.getDescription());
        } catch (Exception e) {
            // 捕获反序列化或系统级不可预期异常
            log.error("handle websocket message failed", e);
            writeError(ctx, "消息格式错误");
        }
    }

    /**
     * 当通道处于非活跃状态（客户端断开连接）时触发
     *
     * 自动从全局 userChannels 路由表以及队伍聊天室中移出该用户的 Channel 连接，避免脏连接残留，并打印断开日志。
     *
     * @param ctx 通道处理器上下文
     * @throws Exception 异常
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        User loginUser = ctx.channel().attr(ChatAttributes.LOGIN_USER).get();
        if (loginUser != null) {
            // 移出全局用户通道
            chatChannelManager.removeUserChannel(loginUser.getId(), ctx.channel());

            // 若此前加入了队伍房间，同步清理
            Long teamId = ctx.channel().attr(ChatAttributes.TEAM_ID).get();
            if (teamId != null) {
                chatChannelManager.leaveRoom(teamId, loginUser.getId(), ctx.channel());
            }
            log.info("netty websocket disconnected, userId={}", loginUser.getId());
        }
        super.channelInactive(ctx);
    }

    /**
     * 构造标准错误格式响应并写回给客户端
     *
     * @param ctx     通道处理器上下文
     * @param message 错误提示信息
     */
    private void writeError(ChannelHandlerContext ctx, String message) {
        ChatMessageResponse response = ChatMessageResponse.error(message);
        try {
            ctx.writeAndFlush(new TextWebSocketFrame(objectMapper.writeValueAsString(response)));
        } catch (Exception e) {
            log.error("write websocket error response failed", e);
        }
    }
}
