package com.wobushi041.codemate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wobushi041.codemate.model.domain.PrivateChatMessage;
import com.wobushi041.codemate.model.vo.PrivateChatUnreadVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 单人私聊消息 Mapper 接口
 *
 * @author wobushi041
 */
public interface PrivateChatMessageMapper extends BaseMapper<PrivateChatMessage> {

    /**
     * 按私聊会话统计当前用户收到的未读消息
     *
     * @param userId 当前用户 id
     * @return 私聊未读统计列表
     */
    @Select("""
            SELECT pcm.sessionId AS sessionId,
                   CASE WHEN pcs.user1Id = #{userId} THEN pcs.user2Id ELSE pcs.user1Id END AS targetUserId,
                   COUNT(*) AS unreadCount
            FROM private_chat_message pcm
            INNER JOIN private_chat_session pcs ON pcs.id = pcm.sessionId AND pcs.isDelete = 0
            WHERE pcm.toUserId = #{userId}
              AND pcm.isRead = 0
              AND pcm.isDelete = 0
            GROUP BY pcm.sessionId, targetUserId
            """)
    List<PrivateChatUnreadVO> listUnreadCounts(@Param("userId") Long userId);

    /**
     * 将指定会话中发给当前用户的消息标记为已读
     *
     * @param sessionId 会话 id
     * @param userId    当前用户 id
     * @return 更新行数
     */
    @Update("""
            UPDATE private_chat_message
            SET isRead = 1
            WHERE sessionId = #{sessionId}
              AND toUserId = #{userId}
              AND isRead = 0
              AND isDelete = 0
            """)
    int markSessionRead(@Param("sessionId") Long sessionId, @Param("userId") Long userId);
}
