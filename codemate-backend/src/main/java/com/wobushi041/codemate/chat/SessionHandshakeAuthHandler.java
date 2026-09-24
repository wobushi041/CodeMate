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
 * @author wobushi041
 */
@Slf4j
public class SessionHandshakeAuthHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    /**
     * Spring Session 默认存储在客户端 Cookie 中的键名
     */
    private static final String SESSION_COOKIE_NAME = "SESSION";

    /**
     * 注入 Spring Session 数据仓储依赖
     */
    private final SessionRepository<? extends Session> sessionRepository;

    /**
     * 构造握手认证处理器
     *
     * @param sessionRepository Spring Session 数据仓储
     */
    public SessionHandshakeAuthHandler(SessionRepository<? extends Session> sessionRepository) {
        // 初始化 Session 数据仓储引用
        this.sessionRepository = sessionRepository;
    }

    /**
     * 读取并处理 HTTP 握手请求，完成分布式会话鉴权
     *
     * @param ctx     通道处理器上下文
     * @param request HTTP 完整请求对象
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        // 尝试从 HTTP 请求头 Cookie 中解析当前登录用户
        User loginUser = resolveLoginUser(request);
        if (loginUser == null) {
            reject(ctx);
            return;
        }

        // 鉴权通过，将用户信息挂载到当前 Channel 属性中并放行至下一级处理器
        ctx.channel().attr(ChatAttributes.LOGIN_USER).set(loginUser);
        ctx.fireChannelRead(request.retain());
    }

    /**
     * 从 HTTP 请求头的 Cookie 中提取并解析登录用户信息
     *
     * @param request HTTP 请求对象
     * @return 已登录的 User 实体；若未登录或会话不存在则返回 null
     */
    private User resolveLoginUser(FullHttpRequest request) {
        // 提取并校验请求头中的 Cookie 字符串
        String cookieHeader = request.headers().get(HttpHeaderNames.COOKIE);
        if (cookieHeader == null || cookieHeader.isBlank()) {
            log.warn("websocket handshake rejected: missing Cookie header, uri={}", request.uri());
            return null;
        }

        // 严格模式解码 Cookie 集合
        Set<Cookie> cookies;
        try {
            cookies = ServerCookieDecoder.STRICT.decode(cookieHeader);
        } catch (IllegalArgumentException e) {
            log.warn("websocket handshake rejected: invalid Cookie header, uri={}", request.uri(), e);
            return null;
        }

        // 遍历 Cookie 查找 SESSION 标识并从 SessionRepository 检索登录态
        boolean hasSessionCookie = false;
        for (Cookie cookie : cookies) {
            if (!SESSION_COOKIE_NAME.equals(cookie.name())) {
                continue;
            }
            hasSessionCookie = true;
            for (String sessionId : candidateSessionIds(cookie.value())) {
                Session session = sessionRepository.findById(sessionId);
                if (session == null) {
                    continue;
                }
                Object userObj = session.getAttribute(USER_LOGIN_STATE);
                if (userObj instanceof User user) {
                    return user;
                }
                log.warn("websocket handshake rejected: session found but login user missing, uri={}", request.uri());
                return null;
            }
        }

        // 记录鉴权失败的具体原因日志
        if (hasSessionCookie) {
            log.warn("websocket handshake rejected: SESSION cookie found but session not found, uri={}", request.uri());
        }
        // 请求未携带 SESSION Cookie
        else {
            log.warn("websocket handshake rejected: SESSION cookie missing, uri={}", request.uri());
        }
        return null;
    }

    /**
     * 解析 Cookie 值生成候选 SessionId 列表（兼容原始值、URL 编码与 Base64 编码）
     *
     * @param cookieValue Cookie 中 SESSION 的原始值
     * @return 候选 SessionId 集合
     */
    private Set<String> candidateSessionIds(String cookieValue) {
        // 依次添加原始值与 URL 解码后的候选值
        Set<String> candidates = new LinkedHashSet<>();
        addCandidate(candidates, cookieValue);
        addCandidate(candidates, URLDecoder.decode(cookieValue, StandardCharsets.UTF_8));

        // 尝试进行 Base64 解码并添加至候选集合
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
     * @param sessionId  SessionId 字符串
     */
    private void addCandidate(Set<String> candidates, String sessionId) {
        // 过滤空字符串
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }

        // 去除首尾空白后加入集合
        candidates.add(sessionId.trim());
    }

    /**
     * 拒绝客户端的握手连接，返回 HTTP 401 状态码并立即关闭通道
     *
     * @param ctx 通道处理器上下文
     */
    private void reject(ChannelHandlerContext ctx) {
        // 构建 401 响应并在写出完成后关闭底层连接
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

}
