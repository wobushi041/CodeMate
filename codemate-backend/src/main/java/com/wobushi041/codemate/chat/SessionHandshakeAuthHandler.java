package com.wobushi041.codemate.chat;

import com.wobushi041.codemate.model.domain.User;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Set;

import static com.wobushi041.codemate.contant.UserConstant.USER_LOGIN_STATE;

/**
 * WebSocket 握手认证处理器
 *
 * 拦截客户端发起的 HTTP 升级（Upgrade）握手请求，从请求头 Cookie 中提取分布式会话标识 SESSION，
 * 并通过 SessionRepository 查询 Redis 中存储的分布式会话及登录用户对象。
 *
 * - 若鉴权失败（未登录、会话过期、Cookie 缺失或非法），直接向客户端返回 HTTP 401 Unauthorized 并关闭底层通道。
 * - 若鉴权成功，将解析得到的 User 实体绑定至当前 Netty Channel 的属性 LOGIN_USER 中，
 *   随后放行请求至下一级 WebSocketServerProtocolHandler。
 *
 * @author 硫酸铜
 */
@Slf4j
public class SessionHandshakeAuthHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    /**
     * Spring Session 默认存储在客户端 Cookie 中的键名
     */
    private static final String SESSION_COOKIE_NAME = "SESSION";

    /**
     * Spring Session 数据仓储接口
     */
    private final SessionRepository<? extends Session> sessionRepository;

    /**
     * 构造函数
     *
     * @param sessionRepository Spring Session 数据仓储
     */
    public SessionHandshakeAuthHandler(SessionRepository<? extends Session> sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    /**
     * 读取并处理 HTTP 握手请求
     *
     * @param ctx 通道处理器上下文
     * @param request HTTP 完整请求对象
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        // 1. 尝试从 HTTP 请求头 Cookie 中解析当前登录用户
        User loginUser = resolveLoginUser(request);
        if (loginUser == null) {
            // 2. 鉴权失败，拒绝连接
            reject(ctx);
            return;
        }
        // 3. 鉴权通过，将用户信息挂载到当前 Channel 属性中供后续业务 Handler 使用
        ctx.channel().attr(ChatAttributes.LOGIN_USER).set(loginUser);
        // 4. 引用计数加 1 并放行至下一个 Handler（WebSocketServerProtocolHandler 处理协议升级）
        ctx.fireChannelRead(request.retain());
    }

    /**
     * 从 HTTP 请求头的 Cookie 中提取并解析登录用户信息
     *
     * @param request HTTP 请求
     * @return 已登录的 User 实体；若未登录或会话不存在则返回 null
     */
    private User resolveLoginUser(FullHttpRequest request) {
        String cookieHeader = request.headers().get(HttpHeaderNames.COOKIE);
        if (cookieHeader == null || cookieHeader.isBlank()) {
            log.warn("websocket handshake rejected: missing Cookie header, uri={}", request.uri());
            return null;
        }
        Set<Cookie> cookies;
        try {
            cookies = ServerCookieDecoder.STRICT.decode(cookieHeader);
        } catch (IllegalArgumentException e) {
            log.warn("websocket handshake rejected: invalid Cookie header, uri={}", request.uri(), e);
            return null;
        }
        boolean hasSessionCookie = false;
        for (Cookie cookie : cookies) {
            if (!SESSION_COOKIE_NAME.equals(cookie.name())) {
                continue;
            }
            hasSessionCookie = true;
            for (String sessionId : candidateSessionIds(cookie.value())) {
                // SessionRepository 查找对应的 Session
                Session session = sessionRepository.findById(sessionId);
                if (session == null) {
                    continue;
                }
                // 提取存储的用户对象
                Object userObj = session.getAttribute(USER_LOGIN_STATE);
                if (userObj instanceof User user) {
                    return user;
                }
                log.warn("websocket handshake rejected: session found but login user missing, uri={}", request.uri());
                return null;
            }
        }
        if (hasSessionCookie) {
            log.warn("websocket handshake rejected: SESSION cookie found but session not found, uri={}", request.uri());
        } else {
            log.warn("websocket handshake rejected: SESSION cookie missing, uri={}", request.uri());
        }
        return null;
    }

    /**
     * 解析 Cookie 值生成候选 SessionId 列表
     *
     * 兼容原始值、URL 编码、Base64 编码等不同客户端或中间件传递的 Cookie 变种形式。
     *
     * @param cookieValue Cookie 中 SESSION 的原始值
     * @return 候选 SessionId 集合
     */
    private Set<String> candidateSessionIds(String cookieValue) {
        Set<String> candidates = new LinkedHashSet<>();
        addCandidate(candidates, cookieValue);
        addCandidate(candidates, URLDecoder.decode(cookieValue, StandardCharsets.UTF_8));
        try {
            addCandidate(candidates, new String(Base64.getDecoder().decode(cookieValue), StandardCharsets.UTF_8));
        } catch (IllegalArgumentException ignored) {
            log.debug("SESSION cookie is not Base64 encoded");
        }
        return candidates;
    }

    /**
     * 将候选 SessionId 去除首尾空格后加入候选集合
     *
     * @param candidates 候选集合
     * @param sessionId SessionId 字符串
     */
    private void addCandidate(Set<String> candidates, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        candidates.add(sessionId.trim());
    }

    /**
     * 拒绝客户端的握手连接，返回 HTTP 401 并立即关闭连接
     *
     * @param ctx 通道处理器上下文
     */
    private void reject(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}
