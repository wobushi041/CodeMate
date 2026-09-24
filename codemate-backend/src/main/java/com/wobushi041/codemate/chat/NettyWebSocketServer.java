package com.wobushi041.codemate.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wobushi041.codemate.service.ChatRoomService;
import com.wobushi041.codemate.service.PrivateChatService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Component;

/**
 * Netty WebSocket 服务端启动类
 *
 * @author wobushi041
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NettyWebSocketServer implements SmartLifecycle {

    /**
     * 注入 Netty WebSocket 服务配置参数依赖
     */
    private final NettyWebSocketProperties properties;

    /**
     * 注入 Spring Session 数据仓储依赖
     */
    private final SessionRepository<? extends Session> sessionRepository;

    /**
     * 注入聊天室通道会话管理器依赖
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
     * 主 Reactor 线程池，负责接收客户端 TCP 连接请求
     */
    private EventLoopGroup bossGroup;

    /**
     * 从 Reactor 线程池，负责处理各连接 Channel 的网络 I/O 与业务流水线
     */
    private EventLoopGroup workerGroup;

    /**
     * 服务端网络监听通道 Channel
     */
    private Channel serverChannel;

    /**
     * 标记服务当前是否处于运行状态（volatile 保证多线程可见性）
     */
    private volatile boolean running;

    /**
     * 启动 Netty WebSocket 服务端，初始化线程组并绑定监听端口
     */
    @Override
    public void start() {
        // 若配置未启用或服务已处于运行状态，则直接跳过
        if (!properties.isEnabled() || running) {
            return;
        }

        // 初始化主从 Reactor 线程组
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        // 装配 ServerBootstrap 管道处理器链并绑定监听端口
        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        /**
                         * 初始化客户端 SocketChannel 的处理器责任链
                         *
                         * @param channel 客户端 Socket 通道
                         */
                        @Override
                        protected void initChannel(SocketChannel channel) {
                            // 依次向 Pipeline 注册 HTTP 编解码、聚合、Session 鉴权、WebSocket 协议升级与业务处理器
                            ChannelPipeline pipeline = channel.pipeline();
                            pipeline.addLast(new HttpServerCodec());
                            pipeline.addLast(new HttpObjectAggregator(65536));
                            pipeline.addLast(new SessionHandshakeAuthHandler(sessionRepository));
                            pipeline.addLast(new WebSocketServerProtocolHandler(properties.getPath(), null, true));
                            pipeline.addLast(new ChatConnectionHandler(
                                    chatChannelManager, chatRoomService, privateChatService, objectMapper));
                        }

                    });
            serverChannel = bootstrap.bind(properties.getPort()).syncUninterruptibly().channel();
            running = true;
            log.info("netty websocket server started, port={}, path={}", properties.getPort(), properties.getPath());
        } catch (RuntimeException e) {
            // 启动异常时优雅关闭线程组并向上抛出异常
            shutdownGroups();
            throw e;
        }
    }

    /**
     * 停止 Netty WebSocket 服务端并释放端口与线程池资源
     */
    @Override
    public void stop() {
        // 若服务未运行则直接返回
        if (!running) {
            return;
        }

        // 关闭服务端监听通道
        if (serverChannel != null) {
            serverChannel.close().syncUninterruptibly();
        }

        // 释放主从线程组并更新运行状态标记
        shutdownGroups();
        running = false;
        log.info("netty websocket server stopped");
    }

    /**
     * 检查 Netty 服务当前是否正在运行
     *
     * @return true 表示正在运行，false 表示未运行
     */
    @Override
    public boolean isRunning() {
        // 返回服务运行状态标记
        return running;
    }

    /**
     * 优雅关闭并释放 Boss 和 Worker 线程组
     */
    private void shutdownGroups() {
        // 优雅关闭 Boss 线程组
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }

        // 优雅关闭 Worker 线程组
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }

}
