package com.wobushi041.codemate.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wobushi041.codemate.mapper.PrivateChatMessageMapper;
import com.wobushi041.codemate.mapper.PrivateChatSessionMapper;
import com.wobushi041.codemate.model.domain.PrivateChatSession;
import com.wobushi041.codemate.model.domain.User;
import com.wobushi041.codemate.model.vo.PrivateChatUnreadVO;
import com.wobushi041.codemate.service.UserService;
import com.wobushi041.codemate.service.impl.PrivateChatServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 私聊未读状态服务单元测试
 *
 * @author wobushi041
 */
class PrivateChatServiceImplTest {

    private final PrivateChatSessionMapper sessionMapper = mock(PrivateChatSessionMapper.class);
    private final PrivateChatMessageMapper messageMapper = mock(PrivateChatMessageMapper.class);
    private final UserService userService = mock(UserService.class);
    private final ChatChannelManager channelManager = new ChatChannelManager();
    private final RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);

    private final PrivateChatServiceImpl service = new PrivateChatServiceImpl(
            sessionMapper,
            messageMapper,
            userService,
            channelManager,
            new ObjectMapper(),
            redisTemplate
    );

    @Test
    void listsUnreadCountsForCurrentUser() {
        User loginUser = user(1L);
        PrivateChatUnreadVO unread = new PrivateChatUnreadVO();
        unread.setSessionId(8L);
        unread.setTargetUserId(2L);
        unread.setUnreadCount(3L);
        when(messageMapper.listUnreadCounts(1L)).thenReturn(List.of(unread));

        List<PrivateChatUnreadVO> result = service.listUnreadCounts(loginUser);

        assertEquals(1, result.size());
        assertEquals(3L, result.get(0).getUnreadCount());
    }

    @Test
    void marksSessionReadForParticipant() {
        User loginUser = user(1L);
        PrivateChatSession session = new PrivateChatSession();
        session.setId(8L);
        session.setUser1Id(1L);
        session.setUser2Id(2L);
        when(sessionMapper.selectById(8L)).thenReturn(session);

        service.markSessionRead(8L, loginUser);

        verify(messageMapper).markSessionRead(8L, 1L);
    }

    private User user(long id) {
        User user = new User();
        user.setId(id);
        return user;
    }
}
