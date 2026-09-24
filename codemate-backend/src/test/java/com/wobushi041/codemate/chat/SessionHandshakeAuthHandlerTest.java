package com.wobushi041.codemate.chat;

import com.wobushi041.codemate.model.domain.User;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.jupiter.api.Test;
import org.springframework.session.MapSession;
import org.springframework.session.SessionRepository;

import java.util.HashMap;
import java.util.Map;
import static com.wobushi041.codemate.contant.UserConstant.USER_LOGIN_STATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * WebSocket 握手会话认证处理器单元测试
 *
 * @author wobushi041
 */
class SessionHandshakeAuthHandlerTest {

    /**
     * 测试携带合法 Spring Session Cookie 的请求通过认证并绑定用户到通道
     */
    // 场景：测试携带有效 Spring Session Cookie 的握手请求通过鉴权并绑定登录用户到 Channel
    @Test
    void acceptsRequestWithValidSpringSessionCookieAndBindsUserToChannel() {
        // 1. 准备测试数据与内存会话存储
        User loginUser = new User();
        loginUser.setId(41L);
        loginUser.setUsername("tester");

        MapSession session = new MapSession("session-123");
        session.setAttribute(USER_LOGIN_STATE, loginUser);
        InMemorySessionRepository sessionRepository = new InMemorySessionRepository(session);

        EmbeddedChannel channel = new EmbeddedChannel(new SessionHandshakeAuthHandler(sessionRepository));
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/ws/chat");
        request.headers().set(HttpHeaderNames.COOKIE, "SESSION=session-123");

        // 2. 调用待测方法写入握手请求
        channel.writeInbound(request);

        // 3. 断言结果并释放资源
        assertSame(loginUser, channel.attr(ChatAttributes.LOGIN_USER).get());
        assertSame(request, channel.readInbound());
        assertNull(channel.readOutbound());

        request.release();
        channel.finishAndReleaseAll();
    }

    /**
     * 测试缺少有效 Spring Session Cookie 的请求返回未授权状态
     */
    // 场景：测试未携带有效 Spring Session Cookie 的握手请求被拒绝并返回 401 状态码
    @Test
    void rejectsRequestWithoutValidSpringSessionCookie() {
        // 1. 准备测试数据
        EmbeddedChannel channel = new EmbeddedChannel(new SessionHandshakeAuthHandler(new InMemorySessionRepository()));
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/ws/chat");

        // 2. 调用待测方法写入未认证请求
        channel.writeInbound(request);

        // 3. 断言结果并释放资源
        assertNull(channel.attr(ChatAttributes.LOGIN_USER).get());
        FullHttpResponse response = channel.readOutbound();
        assertEquals(HttpResponseStatus.UNAUTHORIZED, response.status());

        response.release();
        channel.finishAndReleaseAll();
    }

    /**
     * 测试用内存会话存储实现类
     *
     * @author wobushi041
     */
    private static final class InMemorySessionRepository implements SessionRepository<MapSession> {

        /**
         * 内存会话映射表
         */
        private final Map<String, MapSession> sessions = new HashMap<>();

        /**
         * 构造内存会话仓库并加载初始会话
         *
         * @param initialSessions 初始会话数组
         */
        private InMemorySessionRepository(MapSession... initialSessions) {
            for (MapSession session : initialSessions) {
                sessions.put(session.getId(), session);
            }
        }

        /**
         * 创建新的内存会话
         *
         * @return 新建的 MapSession 实例
         */
        @Override
        public MapSession createSession() {
            return new MapSession();
        }

        /**
         * 保存内存会话至映射表
         *
         * @param session 待保存的会话对象
         */
        @Override
        public void save(MapSession session) {
            sessions.put(session.getId(), session);
        }

        /**
         * 根据会话 id 查询内存会话
         *
         * @param id 会话 id
         * @return 匹配的 MapSession 实例
         */
        @Override
        public MapSession findById(String id) {
            return sessions.get(id);
        }

        /**
         * 根据会话 id 删除内存会话
         *
         * @param id 会话 id
         */
        @Override
        public void deleteById(String id) {
            sessions.remove(id);
        }

    }

}
