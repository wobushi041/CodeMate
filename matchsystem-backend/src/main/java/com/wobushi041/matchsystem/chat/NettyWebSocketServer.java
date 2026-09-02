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

@Component
@Slf4j
@RequiredArgsConstructor//自动生成带final 参数的构造器，构造器注入Bean
/**
 * 启动netty websocket服务类，主从Reactor(1主，默认线程数为从workerGroup）
 */
public class NettyWebSocketServer implements SmartLifecycle {

    private final NettyWebSocketProperties properties;
    private final SessionRepository<? extends Session> sessionRepository;
    private final ChatChannelManager chatChannelManager;
    private final ChatRoomService chatRoomService;
    private final ObjectMapper objectMapper;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    private volatile boolean running;

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
                            pipeline.addLast(new ChatConnectionHandler(chatChannelManager, chatRoomService, objectMapper));//自定义业务处理器chatchannelmanager
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

    @Override
    public boolean isRunning() {
        return running;
    }

    private void shutdownGroups() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }
}
