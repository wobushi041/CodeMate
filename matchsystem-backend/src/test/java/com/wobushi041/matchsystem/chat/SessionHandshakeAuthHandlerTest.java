package com.wobushi041.matchsystem.chat;

import com.wobushi041.matchsystem.model.domain.User;
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

import static com.wobushi041.matchsystem.contant.UserConstant.USER_LOGIN_STATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class SessionHandshakeAuthHandlerTest {

    @Test
    void acceptsRequestWithValidSpringSessionCookieAndBindsUserToChannel() {
        User loginUser = new User();
        loginUser.setId(41L);
        loginUser.setUsername("tester");

        MapSession session = new MapSession("session-123");
        session.setAttribute(USER_LOGIN_STATE, loginUser);
        InMemorySessionRepository sessionRepository = new InMemorySessionRepository(session);

        EmbeddedChannel channel = new EmbeddedChannel(new SessionHandshakeAuthHandler(sessionRepository));
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/ws/chat");
        request.headers().set(HttpHeaderNames.COOKIE, "SESSION=session-123");

        channel.writeInbound(request);

        assertSame(loginUser, channel.attr(ChatAttributes.LOGIN_USER).get());
        assertSame(request, channel.readInbound());
        assertNull(channel.readOutbound());

        request.release();
        channel.finishAndReleaseAll();
    }

    @Test
    void rejectsRequestWithoutValidSpringSessionCookie() {
        EmbeddedChannel channel = new EmbeddedChannel(new SessionHandshakeAuthHandler(new InMemorySessionRepository()));
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/ws/chat");

        channel.writeInbound(request);

        assertNull(channel.attr(ChatAttributes.LOGIN_USER).get());
        FullHttpResponse response = channel.readOutbound();
        assertEquals(HttpResponseStatus.UNAUTHORIZED, response.status());

        response.release();
        channel.finishAndReleaseAll();
    }

    private static final class InMemorySessionRepository implements SessionRepository<MapSession> {

        private final Map<String, MapSession> sessions = new HashMap<>();

        private InMemorySessionRepository(MapSession... initialSessions) {
            for (MapSession session : initialSessions) {
                sessions.put(session.getId(), session);
            }
        }

        @Override
        public MapSession createSession() {
            return new MapSession();
        }

        @Override
        public void save(MapSession session) {
            sessions.put(session.getId(), session);
        }

        @Override
        public MapSession findById(String id) {
            return sessions.get(id);
        }

        @Override
        public void deleteById(String id) {
            sessions.remove(id);
        }
    }
}
