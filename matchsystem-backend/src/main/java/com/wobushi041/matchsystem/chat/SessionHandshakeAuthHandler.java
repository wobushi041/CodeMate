package com.wobushi041.matchsystem.chat;

import com.wobushi041.matchsystem.model.domain.User;
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

import static com.wobushi041.matchsystem.contant.UserConstant.USER_LOGIN_STATE;

@Slf4j
public class SessionHandshakeAuthHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final String SESSION_COOKIE_NAME = "SESSION";

    private final SessionRepository<? extends Session> sessionRepository;

    public SessionHandshakeAuthHandler(SessionRepository<? extends Session> sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        //从session中提取到User对象
        User loginUser = resolveLoginUser(request);
        if (loginUser == null) {
            //如果为空，拒绝reject方法连接
            reject(ctx);
            return;
        }
        //认证通过后User传递给channel的属性中
        ctx.channel().attr(ChatAttributes.LOGIN_USER).set(loginUser);
        ctx.fireChannelRead(request.retain());
    }

    //从cookie提取session id
    private User resolveLoginUser(FullHttpRequest request) {
        String cookieHeader = request.headers().get(HttpHeaderNames.COOKIE);
        if (cookieHeader == null || cookieHeader.isBlank()) {
            return null;
        }
        for (Cookie cookie : ServerCookieDecoder.STRICT.decode(cookieHeader)) {
            if (!SESSION_COOKIE_NAME.equals(cookie.name())) {
                continue;
            }
            for (String sessionId : candidateSessionIds(cookie.value())) {
                //SessionRepository 查找对应的 Session
                Session session = sessionRepository.findById(sessionId);
                if (session == null) {
                    continue;
                }
                //转换User对象
                Object userObj = session.getAttribute(USER_LOGIN_STATE);
                if (userObj instanceof User user) {
                    return user;
                }
            }
        }
        return null;
    }

    private Set<String> candidateSessionIds(String cookieValue) {
        //由一个hashset维护候选session ID
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
    //校验sessionID是否为空
    private void addCandidate(Set<String> candidates, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        candidates.add(sessionId.trim());
    }

    private void reject(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}
