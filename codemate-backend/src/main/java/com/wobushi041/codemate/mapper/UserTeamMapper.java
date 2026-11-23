package com.wobushi041.codemate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wobushi041.codemate.model.domain.UserTeam;
import com.wobushi041.codemate.model.vo.TeamChatUnreadVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 用户队伍关系 Mapper 接口
 *
 * @author wobushi041
 */
public interface UserTeamMapper extends BaseMapper<UserTeam> {

    /**
     * 统计当前用户已加入队伍的聊天室未读消息
     *
     * @param userId 当前用户 id
     * @return 队伍未读统计列表
     */
    @Select("""
            SELECT ut.teamId AS teamId, COUNT(cm.id) AS unreadCount
            FROM user_team ut
            LEFT JOIN chat_message cm
              ON cm.teamId = ut.teamId
             AND cm.id > COALESCE(ut.lastReadMessageId, 0)
             AND cm.fromUserId <> #{userId}
             AND cm.createTime >= COALESCE(ut.joinTime, ut.createTime, '1970-01-01')
             AND cm.isDelete = 0
            WHERE ut.userId = #{userId}
              AND ut.isDelete = 0
            GROUP BY ut.teamId
            """)
    List<TeamChatUnreadVO> listChatUnreadCounts(@Param("userId") Long userId);

    /**
     * 将用户在指定队伍的已读位置推进到最新持久化消息
     *
     * @param teamId 队伍 id
     * @param userId 当前用户 id
     * @return 更新行数
     */
    @Update("""
            UPDATE user_team
            SET lastReadMessageId = (
                    SELECT COALESCE(MAX(cm.id), 0)
                    FROM chat_message cm
                    WHERE cm.teamId = #{teamId} AND cm.isDelete = 0
                ),
                updateTime = CURRENT_TIMESTAMP
            WHERE teamId = #{teamId}
              AND userId = #{userId}
              AND isDelete = 0
            """)
    int markTeamChatRead(@Param("teamId") Long teamId, @Param("userId") Long userId);
}
