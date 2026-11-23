-- 为现有数据库启用队伍聊天室未读统计。
-- 执行前请先备份数据库；本脚本只需要执行一次。

ALTER TABLE user_team
    ADD COLUMN lastReadMessageId BIGINT NOT NULL DEFAULT 0 COMMENT '聊天室最后已读消息id' AFTER joinTime;

CREATE INDEX idx_user_team_unread ON user_team (userId, teamId, isDelete);
CREATE INDEX idx_chat_message_unread ON chat_message (teamId, id, isDelete);
CREATE INDEX idx_private_chat_unread ON private_chat_message (toUserId, isRead, isDelete, sessionId);

-- 上线时将已有消息视为已读，避免老用户首次打开后出现大量历史红点。
UPDATE user_team ut
SET ut.lastReadMessageId = (
    SELECT COALESCE(MAX(cm.id), 0)
    FROM chat_message cm
    WHERE cm.teamId = ut.teamId
      AND cm.isDelete = 0
)
WHERE ut.isDelete = 0;
