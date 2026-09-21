package com.wobushi041.matchsystem.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wobushi041.matchsystem.service.ChatRoomService;
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
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.context.SmartLifecycle;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Component;

/**
 * Netty WebSocket 服务端启动类
 *
 * 基于 Netty 主从 Reactor 线程模型构建的高性能异步长连接服务，实现 SmartLifecycle 接口，
 * 由 Spring 容器自动托管其生命周期的启动与平滑停机。
 *
 * 线程模型架构：
 * - BossGroup：单线程 Reactor，专门负责监听客户端 TCP 连接接入事件
 * - WorkerGroup：多线程 Reactor（默认为 CPU 核心数 * 2），负责处理已连接 Channel 的网络 I/O 读写与编解码
 *
 * Pipeline 处理器链编排顺序：
 * 1. HttpServerCodec：HTTP 编解码器
 * 2. HttpObjectAggregator：HTTP 消息聚合器（限制最大请求体为 64KB）
 * 3. SessionHandshakeAuthHandler：自定义 Spring Session Redis 握手鉴权处理器
 * 4. WebSocketServerProtocolHandler：Netty 官方 WebSocket 协议处理器（完成协议升级与帧处理）
 * 5. ChatConnectionHandler：自定义聊天室业务消息分发与连接生命周期处理器
 *
 * @author 硫酸铜
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NettyWebSocketServer implements SmartLifecycle {

    /**
     * Netty WebSocket 服务配置参数
     */
    private final NettyWebSocketProperties properties;

    /**
     * Spring Session 数据仓储，用于基于分布式 Redis 会话校验登录态
     */
    private final SessionRepository<? extends Session> sessionRepository;

    /**
     * 聊天室通道会话管理器，维护队伍与客户端 Channel 的映射
     */
    private final ChatChannelManager chatChannelManager;

    /**
     * 聊天室业务服务层接口
     */
    private final ChatRoomService chatRoomService;

    /**
     * Jackson JSON 序列化工具类
     */
    private final ObjectMapper objectMapper;

    /**
     * 主 Reactor 线程池：负责接收客户端的 TCP 连接请求
     */
    private EventLoopGroup bossGroup;

    /**
     * 从 Reactor 线程池：负责处理各连接 Channel 的网络 I/O 与业务流水线处理
     */
    private EventLoopGroup workerGroup;

    /**
     * 服务端网络监听通道 Channel
     */
    private Channel serverChannel;

    /**
     * 标记服务当前是否处于运行状态（volatile 保证线程可见性）
     */
    private volatile boolean running;

    /**
     * 启动 Netty WebSocket 服务端
     *
     * 初始化主从线程组、绑定端口并装配 ChannelPipeline 责任链。
     */
    @Override
    public void start() {
        if (!properties.isEnabled() || running) {
            return;
        }
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) {
                            ChannelPipeline pipeline = channel.pipeline();
                            pipeline.addLast(new HttpServerCodec());
                            pipeline.addLast(new HttpObjectAggregator(65536));
                            pipeline.addLast(new SessionHandshakeAuthHandler(sessionRepository));
                            pipeline.addLast(new WebSocketServerProtocolHandler(properties.getPath(), null, true));
                            pipeline.addLast(new ChatConnectionHandler(chatChannelManager, chatRoomService, objectMapper));
                        }
                    });
            serverChannel = bootstrap.bind(properties.getPort()).syncUninterruptibly().channel();
            running = true;
            log.info("netty websocket server started, port={}, path={}", properties.getPort(), properties.getPath());
        } catch (RuntimeException e) {
            shutdownGroups();
            throw e;
        }
    }

    /**
     * 停止 Netty WebSocket 服务端并释放端口与线程池资源
     */
    @Override
    public void stop() {
        if (!running) {
            return;
        }
        if (serverChannel != null) {
            serverChannel.close().syncUninterruptibly();
        }
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
        return running;
    }

    /**
     * 优雅关闭并释放 Boss 和 Worker 线程组
     */
    private void shutdownGroups() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }
}
