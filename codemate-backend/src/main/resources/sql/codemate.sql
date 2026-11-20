-- ----------------------------
-- CodeMate 数据库初始化脚本
-- Target Server Type    : MySQL
-- Target Server Version : 8.0.43
-- File Encoding         : utf8mb4
-- Date: 20/11/2026
-- ----------------------------

DROP DATABASE IF EXISTS `codemate`;
CREATE DATABASE `codemate` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

USE `codemate`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for chat_message
-- ----------------------------
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `teamId` bigint NOT NULL COMMENT '队伍id/聊天室id',
  `clientMessageId` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户端消息id',
  `fromUserId` bigint NOT NULL COMMENT '发送者id',
  `fromUsername` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '发送者昵称',
  `content` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息内容',
  `createTime` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `isDelete` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '聊天消息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of chat_message
-- ----------------------------
INSERT INTO `chat_message` (`id`, `teamId`, `clientMessageId`, `fromUserId`, `fromUsername`, `content`, `createTime`, `isDelete`) VALUES (1, 1, 'demo-team-001', 1, '曹小岐', '大家好，今天把 CodeMate 的联调任务拆一下：首页推荐、队伍协作和聊天链路优先。', '2026-09-24 18:00:00', 0);
INSERT INTO `chat_message` (`id`, `teamId`, `clientMessageId`, `fromUserId`, `fromUsername`, `content`, `createTime`, `isDelete`) VALUES (2, 1, 'demo-team-002', 2, '曹小丹', '前端这边已经接好伙伴卡片和标签筛选，稍后补移动端细节。', '2026-09-24 18:03:00', 0);
INSERT INTO `chat_message` (`id`, `teamId`, `clientMessageId`, `fromUserId`, `fromUsername`, `content`, `createTime`, `isDelete`) VALUES (3, 1, 'demo-team-003', 4, '陈小辉', '后端推荐接口已切到 codemate 库，分页和排序都正常。', '2026-09-24 18:06:00', 0);
INSERT INTO `chat_message` (`id`, `teamId`, `clientMessageId`, `fromUserId`, `fromUsername`, `content`, `createTime`, `isDelete`) VALUES (4, 1, 'demo-team-004', 3, '陈小雪', '我来检查 390px 视口下的卡片间距和底部导航遮挡。', '2026-09-24 18:09:00', 0);
INSERT INTO `chat_message` (`id`, `teamId`, `clientMessageId`, `fromUserId`, `fromUsername`, `content`, `createTime`, `isDelete`) VALUES (5, 1, 'demo-team-005', 7, '冯小伟', 'WebSocket 心跳和断线重连已经通过本地测试，群聊消息能正常落库。', '2026-09-24 18:14:00', 0);
INSERT INTO `chat_message` (`id`, `teamId`, `clientMessageId`, `fromUserId`, `fromUsername`, `content`, `createTime`, `isDelete`) VALUES (6, 1, 'demo-team-006', 1, '曹小岐', '很好，今晚先把真实数据和截图状态准备好，明天再做异常场景。', '2026-09-24 18:18:00', 0);
INSERT INTO `chat_message` (`id`, `teamId`, `clientMessageId`, `fromUserId`, `fromUsername`, `content`, `createTime`, `isDelete`) VALUES (7, 1, 'demo-team-007', 2, '曹小丹', 'AI 助手页我加了流式输出状态，首屏反馈会更明确。', '2026-09-24 18:23:00', 0);
INSERT INTO `chat_message` (`id`, `teamId`, `clientMessageId`, `fromUserId`, `fromUsername`, `content`, `createTime`, `isDelete`) VALUES (8, 1, 'demo-team-008', 4, '陈小辉', 'RabbitMQ 消费端也已确认，离线消息补偿没有积压。', '2026-09-24 18:27:00', 0);
INSERT INTO `chat_message` (`id`, `teamId`, `clientMessageId`, `fromUserId`, `fromUsername`, `content`, `createTime`, `isDelete`) VALUES (9, 1, 'demo-team-009', 3, '陈小雪', '移动端回归完成，搜索框、分类标签和列表都没有横向溢出。', '2026-09-24 18:31:00', 0);
INSERT INTO `chat_message` (`id`, `teamId`, `clientMessageId`, `fromUserId`, `fromUsername`, `content`, `createTime`, `isDelete`) VALUES (10, 1, 'demo-team-010', 7, '冯小伟', '我把聊天室压测结果整理好了，50 个并发连接下表现稳定。', '2026-09-24 18:34:00', 0);
INSERT INTO `chat_message` (`id`, `teamId`, `clientMessageId`, `fromUserId`, `fromUsername`, `content`, `createTime`, `isDelete`) VALUES (11, 1, 'demo-team-011', 1, '曹小岐', '收到，发布前再统一检查一次登录态和权限边界。', '2026-09-24 18:37:00', 0);
INSERT INTO `chat_message` (`id`, `teamId`, `clientMessageId`, `fromUserId`, `fromUsername`, `content`, `createTime`, `isDelete`) VALUES (12, 1, 'demo-team-012', 2, '曹小丹', '没问题，我会把最终交互录屏和页面截图一起归档。', '2026-09-24 18:40:00', 0);

-- ----------------------------
-- Table structure for private_chat_message
-- ----------------------------
DROP TABLE IF EXISTS `private_chat_message`;
CREATE TABLE `private_chat_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息id',
  `sessionId` bigint NOT NULL COMMENT '所属会话id',
  `clientMessageId` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户端消息防重id',
  `fromUserId` bigint NOT NULL COMMENT '发送者id',
  `toUserId` bigint NOT NULL COMMENT '接收者id',
  `content` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息内容',
  `isRead` tinyint NOT NULL DEFAULT 0 COMMENT '是否已读 0-未读 1-已读',
  `createTime` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `isDelete` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 23 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '单人私聊消息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of private_chat_message
-- ----------------------------
INSERT INTO `private_chat_message` (`id`, `sessionId`, `clientMessageId`, `fromUserId`, `toUserId`, `content`, `isRead`, `createTime`, `isDelete`) VALUES (1, 1, 'demo-private-002', 2, 1, '已经稳定了，筛选标签和联系人入口都能正常使用。', 1, '2026-09-24 17:02:00', 0);
INSERT INTO `private_chat_message` (`id`, `sessionId`, `clientMessageId`, `fromUserId`, `toUserId`, `content`, `isRead`, `createTime`, `isDelete`) VALUES (2, 1, 'demo-private-003', 1, 2, '麻烦再确认一下小屏设备的底部导航。', 1, '2026-09-24 17:05:00', 0);
INSERT INTO `private_chat_message` (`id`, `sessionId`, `clientMessageId`, `fromUserId`, `toUserId`, `content`, `isRead`, `createTime`, `isDelete`) VALUES (3, 1, 'demo-private-004', 2, 1, '刚检查完，安全区和滚动区域都没有遮挡。', 1, '2026-09-24 17:08:00', 0);
INSERT INTO `private_chat_message` (`id`, `sessionId`, `clientMessageId`, `fromUserId`, `toUserId`, `content`, `isRead`, `createTime`, `isDelete`) VALUES (4, 1, 'demo-private-005', 1, 2, '很好，今晚我们把作品集截图需要的页面都整理出来。', 1, '2026-09-24 17:12:00', 0);
INSERT INTO `private_chat_message` (`id`, `sessionId`, `clientMessageId`, `fromUserId`, `toUserId`, `content`, `isRead`, `createTime`, `isDelete`) VALUES (5, 1, 'demo-private-006', 2, 1, '可以，我会优先准备首页、队伍和聊天三个状态。', 1, '2026-09-24 17:15:00', 0);
INSERT INTO `private_chat_message` (`id`, `sessionId`, `clientMessageId`, `fromUserId`, `toUserId`, `content`, `isRead`, `createTime`, `isDelete`) VALUES (6, 2, 'demo-private-007', 12, 1, 'AI 助手的知识检索结果已经能附带来源摘要。', 1, '2026-09-24 16:00:00', 0);
INSERT INTO `private_chat_message` (`id`, `sessionId`, `clientMessageId`, `fromUserId`, `toUserId`, `content`, `isRead`, `createTime`, `isDelete`) VALUES (7, 2, 'demo-private-008', 1, 12, '流式输出过程中断线后能继续恢复吗？', 1, '2026-09-24 16:04:00', 0);
INSERT INTO `private_chat_message` (`id`, `sessionId`, `clientMessageId`, `fromUserId`, `toUserId`, `content`, `isRead`, `createTime`, `isDelete`) VALUES (8, 2, 'demo-private-009', 12, 1, '已经加了失败提示和重新发送入口，演示流程没问题。', 1, '2026-09-24 16:08:00', 0);
INSERT INTO `private_chat_message` (`id`, `sessionId`, `clientMessageId`, `fromUserId`, `toUserId`, `content`, `isRead`, `createTime`, `isDelete`) VALUES (9, 2, 'demo-private-010', 1, 12, '那就保留一个真实问答案例，截图时更有说服力。', 1, '2026-09-24 16:12:00', 0);
INSERT INTO `private_chat_message` (`id`, `sessionId`, `clientMessageId`, `fromUserId`, `toUserId`, `content`, `isRead`, `createTime`, `isDelete`) VALUES (10, 2, 'demo-private-011', 12, 1, '收到，我会用 Spring Boot 性能优化作为示例问题。', 0, '2026-09-24 16:15:00', 0);
INSERT INTO `private_chat_message` (`id`, `sessionId`, `clientMessageId`, `fromUserId`, `toUserId`, `content`, `isRead`, `createTime`, `isDelete`) VALUES (11, 3, 'demo-private-012', 1, 28, '向量检索这块你建议用 Milvus 还是 Elasticsearch？', 1, '2026-09-23 20:00:00', 0);
INSERT INTO `private_chat_message` (`id`, `sessionId`, `clientMessageId`, `fromUserId`, `toUserId`, `content`, `isRead`, `createTime`, `isDelete`) VALUES (12, 3, 'demo-private-013', 28, 1, '数据量不大先用 Elasticsearch，后续再根据召回效果评估迁移。', 1, '2026-09-23 20:05:00', 0);
INSERT INTO `private_chat_message` (`id`, `sessionId`, `clientMessageId`, `fromUserId`, `toUserId`, `content`, `isRead`, `createTime`, `isDelete`) VALUES (13, 3, 'demo-private-014', 1, 28, '明白，我先把索引结构和召回指标记录下来。', 1, '2026-09-23 20:09:00', 0);
INSERT INTO `private_chat_message` (`id`, `sessionId`, `clientMessageId`, `fromUserId`, `toUserId`, `content`, `isRead`, `createTime`, `isDelete`) VALUES (14, 3, 'demo-private-015', 28, 1, '可以，记得把关键词检索作为降级兜底。', 0, '2026-09-23 20:12:00', 0);
INSERT INTO `private_chat_message` (`id`, `sessionId`, `clientMessageId`, `fromUserId`, `toUserId`, `content`, `isRead`, `createTime`, `isDelete`) VALUES (16, 1, 'demo-private-001', 1, 2, '首页推荐卡片的交互已经稳定了吗？', 1, '2026-09-24 17:00:00', 0);
INSERT INTO `private_chat_message` (`id`, `sessionId`, `clientMessageId`, `fromUserId`, `toUserId`, `content`, `isRead`, `createTime`, `isDelete`) VALUES (17, 4, 'portfolio-private-01', 2, 36, '首页伙伴推荐的标签筛选已经联调完成，你那边接口返回稳定吗？', 1, '2026-09-24 20:02:00', 0);
INSERT INTO `private_chat_message` (`id`, `sessionId`, `clientMessageId`, `fromUserId`, `toUserId`, `content`, `isRead`, `createTime`, `isDelete`) VALUES (18, 4, 'portfolio-private-02', 36, 2, '稳定，分页、排序和头像字段都对齐了，我正在补异常状态。', 1, '2026-09-24 20:04:00', 0);
INSERT INTO `private_chat_message` (`id`, `sessionId`, `clientMessageId`, `fromUserId`, `toUserId`, `content`, `isRead`, `createTime`, `isDelete`) VALUES (19, 4, 'portfolio-private-03', 2, 36, '很好。我把移动端卡片间距也调整好了，390px 视口没有横向溢出。', 1, '2026-09-24 20:07:00', 0);
INSERT INTO `private_chat_message` (`id`, `sessionId`, `clientMessageId`, `fromUserId`, `toUserId`, `content`, `isRead`, `createTime`, `isDelete`) VALUES (20, 4, 'portfolio-private-04', 36, 2, '收到，我再检查一次私聊离线补偿和 WebSocket 重连。', 1, '2026-09-24 20:09:00', 0);
INSERT INTO `private_chat_message` (`id`, `sessionId`, `clientMessageId`, `fromUserId`, `toUserId`, `content`, `isRead`, `createTime`, `isDelete`) VALUES (21, 4, 'portfolio-private-05', 2, 36, '测试通过后发我截图，我们一起整理作品集展示素材。', 1, '2026-09-24 20:12:00', 0);
INSERT INTO `private_chat_message` (`id`, `sessionId`, `clientMessageId`, `fromUserId`, `toUserId`, `content`, `isRead`, `createTime`, `isDelete`) VALUES (22, 4, 'portfolio-private-06', 36, 2, '没问题，群聊、私聊和 AI 助手三个核心场景都会覆盖。', 1, '2026-09-24 20:15:00', 0);

-- ----------------------------
-- Table structure for private_chat_session
-- ----------------------------
DROP TABLE IF EXISTS `private_chat_session`;
CREATE TABLE `private_chat_session`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会话id',
  `user1Id` bigint NOT NULL COMMENT '用户1 id (较小值)',
  `user2Id` bigint NOT NULL COMMENT '用户2 id (较大值)',
  `lastMessage` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最后一条消息摘要',
  `lastMessageTime` datetime NULL DEFAULT NULL COMMENT '最后消息时间',
  `createTime` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_users`(`user1Id` ASC, `user2Id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '单人私聊会话' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of private_chat_session
-- ----------------------------
INSERT INTO `private_chat_session` (`id`, `user1Id`, `user2Id`, `lastMessage`, `lastMessageTime`, `createTime`, `updateTime`, `isDelete`) VALUES (1, 1, 2, '可以，我会优先准备首页、队伍和聊天三个状态。', '2026-09-24 17:15:00', '2026-09-24 20:49:52', '2026-09-24 17:15:00', 0);
INSERT INTO `private_chat_session` (`id`, `user1Id`, `user2Id`, `lastMessage`, `lastMessageTime`, `createTime`, `updateTime`, `isDelete`) VALUES (2, 1, 12, '收到，我会用 Spring Boot 性能优化作为示例问题。', '2026-09-24 16:15:00', '2026-09-23 15:00:00', '2026-09-24 16:15:00', 0);
INSERT INTO `private_chat_session` (`id`, `user1Id`, `user2Id`, `lastMessage`, `lastMessageTime`, `createTime`, `updateTime`, `isDelete`) VALUES (3, 1, 28, '可以，记得把关键词检索作为降级兜底。', '2026-09-23 20:12:00', '2026-09-22 20:00:00', '2026-09-23 20:12:00', 0);
INSERT INTO `private_chat_session` (`id`, `user1Id`, `user2Id`, `lastMessage`, `lastMessageTime`, `createTime`, `updateTime`, `isDelete`) VALUES (4, 2, 36, '没问题，群聊、私聊和 AI 助手三个核心场景都会覆盖。', '2026-09-24 20:15:00', '2026-09-24 21:03:19', '2026-09-24 20:15:00', 0);
INSERT INTO `private_chat_session` (`id`, `user1Id`, `user2Id`, `lastMessage`, `lastMessageTime`, `createTime`, `updateTime`, `isDelete`) VALUES (5, 10, 16, NULL, NULL, '2026-09-26 22:15:06', '2026-09-26 22:15:06', 0);
INSERT INTO `private_chat_session` (`id`, `user1Id`, `user2Id`, `lastMessage`, `lastMessageTime`, `createTime`, `updateTime`, `isDelete`) VALUES (6, 1, 48, NULL, NULL, '2026-11-19 11:28:55', '2026-11-19 11:28:55', 0);
INSERT INTO `private_chat_session` (`id`, `user1Id`, `user2Id`, `lastMessage`, `lastMessageTime`, `createTime`, `updateTime`, `isDelete`) VALUES (7, 5, 49, NULL, NULL, '2026-11-19 21:26:37', '2026-11-19 21:26:37', 0);
INSERT INTO `private_chat_session` (`id`, `user1Id`, `user2Id`, `lastMessage`, `lastMessageTime`, `createTime`, `updateTime`, `isDelete`) VALUES (8, 3, 48, NULL, NULL, '2026-11-19 21:57:27', '2026-11-19 21:57:27', 0);

-- ----------------------------
-- Table structure for team
-- ----------------------------
DROP TABLE IF EXISTS `team`;
CREATE TABLE `team`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '队伍名称',
  `description` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '描述',
  `maxNum` int NOT NULL DEFAULT 1 COMMENT '最大人数',
  `expireTime` datetime NULL DEFAULT NULL COMMENT '过期时间',
  `userId` bigint NULL DEFAULT NULL COMMENT '用户id',
  `status` int NOT NULL DEFAULT 0 COMMENT '0 - 公开，1 - 私有，2 - 加密',
  `password` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '密码',
  `createTime` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '队伍' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of team
-- ----------------------------
INSERT INTO `team` (`id`, `name`, `description`, `maxNum`, `expireTime`, `userId`, `status`, `password`, `createTime`, `updateTime`, `isDelete`) VALUES (1, 'CodeMate 全栈共创组', '围绕伙伴匹配、即时通信与 AI 助手协作打磨核心体验', 8, '2027-06-30 23:59:59', 1, 0, NULL, '2026-09-20 09:30:00', '2026-09-24 18:40:00', 0);
INSERT INTO `team` (`id`, `name`, `description`, `maxNum`, `expireTime`, `userId`, `status`, `password`, `createTime`, `updateTime`, `isDelete`) VALUES (2, 'AI 应用实战小队', '用 LangChain4j、FastAPI 与向量检索完成可落地的 AI 应用', 6, '2027-03-31 23:59:59', 12, 0, NULL, '2026-09-18 14:20:00', '2026-09-24 17:20:00', 0);
INSERT INTO `team` (`id`, `name`, `description`, `maxNum`, `expireTime`, `userId`, `status`, `password`, `createTime`, `updateTime`, `isDelete`) VALUES (3, 'Spring Cloud 微服务攻坚组', '拆解网关、注册中心、消息队列与分布式事务的工程实践', 6, '2027-01-31 23:59:59', 9, 2, 'codemate2026', '2026-09-16 19:10:00', '2026-09-24 16:10:00', 0);
INSERT INTO `team` (`id`, `name`, `description`, `maxNum`, `expireTime`, `userId`, `status`, `password`, `createTime`, `updateTime`, `isDelete`) VALUES (4, '前端体验优化组', '聚焦 Vue 3、TypeScript 与移动端交互体验的持续优化', 5, '2026-12-31 23:59:59', 2, 1, NULL, '2026-09-15 10:00:00', '2026-09-23 21:00:00', 0);
INSERT INTO `team` (`id`, `name`, `description`, `maxNum`, `expireTime`, `userId`, `status`, `password`, `createTime`, `updateTime`, `isDelete`) VALUES (5, 'CodeMatec产品设计组', '', 6, '2026-09-30 04:11:00', 16, 0, '', '2026-09-26 22:18:04', '2026-09-26 22:18:04', 0);
INSERT INTO `team` (`id`, `name`, `description`, `maxNum`, `expireTime`, `userId`, `status`, `password`, `createTime`, `updateTime`, `isDelete`) VALUES (6, 'wwrwrr', '2222', 3, '2026-11-19 23:59:00', 49, 2, 'qee', '2026-11-19 20:55:49', '2026-11-19 20:55:49', 0);

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `username` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户昵称',
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `userAccount` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '账号',
  `avatarUrl` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户头像',
  `gender` tinyint NULL DEFAULT NULL COMMENT '性别',
  `userPassword` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码',
  `phone` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '电话',
  `email` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `userStatus` int NOT NULL DEFAULT 0 COMMENT '状态 0 - 正常',
  `createTime` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `isDelete` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除',
  `userRole` int NOT NULL DEFAULT 0 COMMENT '用户角色 0 - 普通用户 1 - 管理员',
  `planetCode` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '星球编号',
  `tags` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '标签 json 列表',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 50 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('曹小岐', 1, '2420160001', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160001', 0, 'c1f51dcb254192df652927458fba5c01', '13824160001', '2420160001@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160001', '[\"Java\",\"SpringBoot\",\"MySQL\",\"Redis\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('曹小丹', 2, '2420160002', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160002', 1, 'c1f51dcb254192df652927458fba5c01', '13824160002', '2420160002@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160002', '[\"Vue3\",\"TypeScript\",\"Pinia\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('陈小雪', 3, '2420160006', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160006', 1, 'c1f51dcb254192df652927458fba5c01', '13824160006', '2420160006@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160006', '[\"HTML5\",\"CSS3\",\"TailwindCSS\",\"JavaScript\",\"Vite\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('陈小辉', 4, '2420160007', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160007', 0, 'c1f51dcb254192df652927458fba5c01', '13824160007', '2420160007@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160007', '[\"Java\",\"MyBatisPlus\",\"MySQL\",\"Docker\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('陈小迪', 5, '2420160009', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160009', 0, 'c1f51dcb254192df652927458fba5c01', '13824160009', '2420160009@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160009', '[\"Python\",\"Django\",\"PostgreSQL\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('丁小翔', 6, '2420160011', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160011', 0, 'c1f51dcb254192df652927458fba5c01', '13824160011', '2420160011@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160011', '[\"C++\",\"STL\",\"CMake\",\"Linux\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('冯小伟', 7, '2420220019', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420220019', 0, 'c1f51dcb254192df652927458fba5c01', '13824220019', '2420220019@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420220019', '[\"Go\",\"Gin\",\"GORM\",\"Docker\",\"Kubernetes\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('甘小茵', 8, '2420160015', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160015', 1, 'c1f51dcb254192df652927458fba5c01', '13824160015', '2420160015@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160015', '[\"React\",\"Next.js\",\"TypeScript\",\"GraphQL\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('高小威', 9, '2420160016', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160016', 0, 'c1f51dcb254192df652927458fba5c01', '13824160016', '2420160016@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160016', '[\"Java\",\"SpringCloud\",\"Nacos\",\"RabbitMQ\",\"Redis\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('何小欢', 10, '2420160018', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160018', 1, 'c1f51dcb254192df652927458fba5c01', '13824160018', '2420160018@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160018', '[\"Vue3\",\"ElementPlus\",\"Axios\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('何小俊', 11, '2420160019', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160019', 0, 'c1f51dcb254192df652927458fba5c01', '13824160019', '2420160019@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160019', '[\"Java\",\"SpringBoot\",\"MyBatis\",\"Redisson\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('黄小宁', 12, '2420160023', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160023', 0, 'c1f51dcb254192df652927458fba5c01', '13824160023', '2420160023@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160023', '[\"Python\",\"PyTorch\",\"NumPy\",\"Pandas\",\"OpenCV\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('黄小文', 13, '2420160025', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160025', 0, 'c1f51dcb254192df652927458fba5c01', '13824160025', '2420160025@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160025', '[\"C++\",\"Linux\",\"Socket\",\"Epoll\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('赖小圳', 14, '2420160031', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160031', 0, 'c1f51dcb254192df652927458fba5c01', '13824160031', '2420160031@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160031', '[\"Java\",\"Kafka\",\"Redis\",\"MySQL\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('梁小豪', 15, '2220160135', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2220160135', 0, 'c1f51dcb254192df652927458fba5c01', '13822160135', '2220160135@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2220160135', '[\"Java\",\"SpringBoot\",\"Vue3\",\"Docker\",\"Nginx\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('廖小文', 16, '2420160041', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160041', 0, 'c1f51dcb254192df652927458fba5c01', '13824160041', '2420160041@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160041', '[\"Vue3\",\"TailwindCSS\",\"Vite\",\"TypeScript\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('李小凯', 17, '2420160034', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160034', 0, 'c1f51dcb254192df652927458fba5c01', '13824160034', '2420160034@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160034', '[\"Java\",\"SpringBoot\",\"MySQL\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('林小翔', 18, '2420160044', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160044', 0, 'c1f51dcb254192df652927458fba5c01', '13824160044', '2420160044@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160044', '[\"Python\",\"Scrapy\",\"Selenium\",\"MongoDB\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('林小煌', 19, '2410310023', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2410310023', 0, 'c1f51dcb254192df652927458fba5c01', '13824310023', '2410310023@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2410310023', '[\"C++\",\"Python\",\"MATLAB\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('林小翔', 20, '2420160045', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160045', 0, 'c1f51dcb254192df652927458fba5c01', '13824160045', '2420160045@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160045', '[\"Go\",\"Docker\",\"Kubernetes\",\"Prometheus\",\"gRPC\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('刘小淼', 21, '2420160047', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160047', 0, 'c1f51dcb254192df652927458fba5c01', '13824160047', '2420160047@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160047', '[\"Java\",\"Netty\",\"WebSocket\",\"Redis\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('刘小涛', 22, '2420160050', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160050', 0, 'c1f51dcb254192df652927458fba5c01', '13824160050', '2420160050@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160050', '[\"Java\",\"SpringCloud\",\"Sentinel\",\"Seata\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('刘小源', 23, '2420160052', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160052', 0, 'c1f51dcb254192df652927458fba5c01', '13824160052', '2420160052@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160052', '[\"Vue3\",\"Vite\",\"Webpack\",\"ESLint\",\"Prettier\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('麦小安', 24, '2420160057', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160057', 0, 'c1f51dcb254192df652927458fba5c01', '13824160057', '2420160057@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160057', '[\"Java\",\"ElasticSearch\",\"Kibana\",\"MySQL\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('欧小凯', 25, '2420160061', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160061', 0, 'c1f51dcb254192df652927458fba5c01', '13824160061', '2420160061@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160061', '[\"React\",\"TypeScript\",\"Redux\",\"Sass\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('孙小铭', 26, '2420160065', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160065', 0, 'c1f51dcb254192df652927458fba5c01', '13824160065', '2420160065@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160065', '[\"Java\",\"SpringSecurity\",\"JWT\",\"OAuth2\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('孙小杨', 27, '2220160069', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2220160069', 0, 'c1f51dcb254192df652927458fba5c01', '13822160069', '2220160069@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2220160069', '[\"C#\",\"Unity3D\",\"Shader\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('谭小博', 28, '2420160068', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160068', 0, 'c1f51dcb254192df652927458fba5c01', '13824160068', '2420160068@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160068', '[\"Python\",\"LangChain\",\"FastAPI\",\"Milvus\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('王小超', 29, '2420160074', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160074', 0, 'c1f51dcb254192df652927458fba5c01', '13824160074', '2420160074@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160074', '[\"Java\",\"MySQL\",\"ShardingSphere\",\"Redis\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('魏小慧', 30, '2420160078', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160078', 1, 'c1f51dcb254192df652927458fba5c01', '13824160078', '2420160078@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160078', '[\"Vue3\",\"Vant\",\"UniApp\",\"Less\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('危小浩', 31, '2420160077', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160077', 0, 'c1f51dcb254192df652927458fba5c01', '13824160077', '2420160077@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160077', '[\"Java\",\"Redisson\",\"Zookeeper\",\"Dubbo\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('吴小竞', 32, '2420160079', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160079', 0, 'c1f51dcb254192df652927458fba5c01', '13824160079', '2420160079@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160079', '[\"Rust\",\"Tokio\",\"WebAssembly\",\"C\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('谢小明', 33, '2420160082', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160082', 0, 'c1f51dcb254192df652927458fba5c01', '13824160082', '2420160082@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160082', '[\"Java\",\"RabbitMQ\",\"SpringBoot\",\"MySQL\",\"Git\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('谢小虎', 34, '2420160083', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160083', 0, 'c1f51dcb254192df652927458fba5c01', '13824160083', '2420160083@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160083', '[\"C\",\"C++\",\"STM32\",\"FreeRTOS\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('熊小臻', 35, '2420160084', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160084', 0, 'c1f51dcb254192df652927458fba5c01', '13824160084', '2420160084@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160084', '[\"Python\",\"FastAPI\",\"Vue3\",\"PostgreSQL\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('徐小聪', 36, '2420160085', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160085', 0, 'c1f51dcb254192df652927458fba5c01', '13824160085', '2420160085@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160085', '[\"Java\",\"SpringBoot\",\"Swagger\",\"MyBatisPlus\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('徐小宇', 37, '2420160086', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160086', 0, 'c1f51dcb254192df652927458fba5c01', '13824160086', '2420160086@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160086', '[\"Node.js\",\"Express\",\"Vue3\",\"MongoDB\",\"TypeScript\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('许小鑫', 38, '2420160088', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160088', 0, 'c1f51dcb254192df652927458fba5c01', '13824160088', '2420160088@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160088', '[\"Java\",\"MySQL\",\"Maven\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('许小炫', 39, '2420160090', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160090', 0, 'c1f51dcb254192df652927458fba5c01', '13824160090', '2420160090@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160090', '[\"Kotlin\",\"Android\",\"Flutter\",\"Dart\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('杨小彬', 40, '2420160092', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160092', 0, 'c1f51dcb254192df652927458fba5c01', '13824160092', '2420160092@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160092', '[\"Java\",\"C++\",\"DataStructures\",\"Algorithms\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('游小宏', 41, '2420160093', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160093', 0, 'c1f51dcb254192df652927458fba5c01', '13824160093', '2420160093@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160093', '[\"Go\",\"Etcd\",\"Redis\",\"ZeroMQ\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('张小涛', 42, '2420160098', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160098', 0, 'c1f51dcb254192df652927458fba5c01', '13824160098', '2420160098@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160098', '[\"Java\",\"SpringCloud\",\"Gateway\",\"OpenFeign\",\"Nacos\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('张小琸', 43, '2420160099', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160099', 1, 'c1f51dcb254192df652927458fba5c01', '13824160099', '2420160099@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160099', '[\"Vue3\",\"TypeScript\",\"ECharts\",\"Figma\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('赵小辰', 44, '2420160101', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160101', 0, 'c1f51dcb254192df652927458fba5c01', '13824160101', '2420160101@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160101', '[\"Python\",\"TensorFlow\",\"ScikitLearn\",\"Jupyter\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('赵小霞', 45, '2420160102', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160102', 1, 'c1f51dcb254192df652927458fba5c01', '13824160102', '2420160102@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160102', '[\"Python\",\"Pytest\",\"Playwright\",\"JMeter\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('周小帅', 46, '2420160104', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160104', 0, 'c1f51dcb254192df652927458fba5c01', '13824160104', '2420160104@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160104', '[\"Java\",\"Docker\",\"Jenkins\",\"Linux\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('朱小豪', 47, '2420160105', 'https://api.dicebear.com/7.x/adventurer/svg?seed=2420160105', 0, 'c1f51dcb254192df652927458fba5c01', '13824160105', '2420160105@stu.edu.cn', 0, '2026-09-24 20:47:22', '2026-09-24 20:47:22', 0, 0, '2420160105', '[\"TypeScript\",\"Vue3\",\"SpringBoot\",\"MySQL\",\"Redis\"]');
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES (NULL, 48, '0411', NULL, NULL, 'c1f51dcb254192df652927458fba5c01', NULL, NULL, 0, '2026-11-19 11:27:11', '2026-11-19 11:27:11', 0, 0, '11', NULL);
INSERT INTO `user` (`username`, `id`, `userAccount`, `avatarUrl`, `gender`, `userPassword`, `phone`, `email`, `userStatus`, `createTime`, `updateTime`, `isDelete`, `userRole`, `planetCode`, `tags`) VALUES ('', 49, '1234', '', 0, 'c1f51dcb254192df652927458fba5c01', '', '', 0, '2026-11-19 20:48:39', '2026-11-19 20:56:35', 0, 0, '12', '[\"Java\",\"Python\",\"Spring Boot\",\"MySQL\",\"后端开发\",\"前端开发\",\"AI\"]');

-- ----------------------------
-- Table structure for user_team
-- ----------------------------
DROP TABLE IF EXISTS `user_team`;
CREATE TABLE `user_team`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `userId` bigint NULL DEFAULT NULL COMMENT '用户id',
  `teamId` bigint NULL DEFAULT NULL COMMENT '队伍id',
  `joinTime` datetime NULL DEFAULT NULL COMMENT '加入时间',
  `createTime` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 39 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户队伍关系' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_team
-- ----------------------------
INSERT INTO `user_team` (`id`, `userId`, `teamId`, `joinTime`, `createTime`, `updateTime`, `isDelete`) VALUES (1, 1, 1, '2026-09-20 09:30:00', '2026-09-20 09:30:00', '2026-09-20 09:30:00', 0);
INSERT INTO `user_team` (`id`, `userId`, `teamId`, `joinTime`, `createTime`, `updateTime`, `isDelete`) VALUES (2, 2, 1, '2026-09-20 10:00:00', '2026-09-20 10:00:00', '2026-09-20 10:00:00', 0);
INSERT INTO `user_team` (`id`, `userId`, `teamId`, `joinTime`, `createTime`, `updateTime`, `isDelete`) VALUES (3, 3, 1, '2026-09-20 10:15:00', '2026-09-20 10:15:00', '2026-09-20 10:15:00', 0);
INSERT INTO `user_team` (`id`, `userId`, `teamId`, `joinTime`, `createTime`, `updateTime`, `isDelete`) VALUES (4, 4, 1, '2026-09-20 10:30:00', '2026-09-20 10:30:00', '2026-09-20 10:30:00', 0);
INSERT INTO `user_team` (`id`, `userId`, `teamId`, `joinTime`, `createTime`, `updateTime`, `isDelete`) VALUES (5, 7, 1, '2026-09-21 09:00:00', '2026-09-21 09:00:00', '2026-09-21 09:00:00', 0);
INSERT INTO `user_team` (`id`, `userId`, `teamId`, `joinTime`, `createTime`, `updateTime`, `isDelete`) VALUES (6, 12, 2, '2026-09-18 14:20:00', '2026-09-18 14:20:00', '2026-09-18 14:20:00', 0);
INSERT INTO `user_team` (`id`, `userId`, `teamId`, `joinTime`, `createTime`, `updateTime`, `isDelete`) VALUES (7, 1, 2, '2026-09-18 15:00:00', '2026-09-18 15:00:00', '2026-09-18 15:00:00', 0);
INSERT INTO `user_team` (`id`, `userId`, `teamId`, `joinTime`, `createTime`, `updateTime`, `isDelete`) VALUES (8, 28, 2, '2026-09-18 15:10:00', '2026-09-18 15:10:00', '2026-09-18 15:10:00', 0);
INSERT INTO `user_team` (`id`, `userId`, `teamId`, `joinTime`, `createTime`, `updateTime`, `isDelete`) VALUES (9, 8, 2, '2026-09-18 16:00:00', '2026-09-18 16:00:00', '2026-09-18 16:00:00', 0);
INSERT INTO `user_team` (`id`, `userId`, `teamId`, `joinTime`, `createTime`, `updateTime`, `isDelete`) VALUES (10, 9, 3, '2026-09-16 19:10:00', '2026-09-16 19:10:00', '2026-09-16 19:10:00', 0);
INSERT INTO `user_team` (`id`, `userId`, `teamId`, `joinTime`, `createTime`, `updateTime`, `isDelete`) VALUES (11, 1, 3, '2026-09-17 09:00:00', '2026-09-17 09:00:00', '2026-09-17 09:00:00', 0);
INSERT INTO `user_team` (`id`, `userId`, `teamId`, `joinTime`, `createTime`, `updateTime`, `isDelete`) VALUES (12, 11, 3, '2026-09-17 09:15:00', '2026-09-17 09:15:00', '2026-09-17 09:15:00', 0);
INSERT INTO `user_team` (`id`, `userId`, `teamId`, `joinTime`, `createTime`, `updateTime`, `isDelete`) VALUES (13, 14, 3, '2026-09-17 09:30:00', '2026-09-17 09:30:00', '2026-09-17 09:30:00', 0);
INSERT INTO `user_team` (`id`, `userId`, `teamId`, `joinTime`, `createTime`, `updateTime`, `isDelete`) VALUES (14, 2, 4, '2026-09-15 10:00:00', '2026-09-15 10:00:00', '2026-09-15 10:00:00', 0);
INSERT INTO `user_team` (`id`, `userId`, `teamId`, `joinTime`, `createTime`, `updateTime`, `isDelete`) VALUES (15, 3, 4, '2026-09-15 10:20:00', '2026-09-15 10:20:00', '2026-09-15 10:20:00', 0);
INSERT INTO `user_team` (`id`, `userId`, `teamId`, `joinTime`, `createTime`, `updateTime`, `isDelete`) VALUES (16, 10, 4, '2026-09-15 11:00:00', '2026-09-15 11:00:00', '2026-09-15 11:00:00', 0);
INSERT INTO `user_team` (`id`, `userId`, `teamId`, `joinTime`, `createTime`, `updateTime`, `isDelete`) VALUES (17, 16, 4, '2026-09-15 11:30:00', '2026-09-15 11:30:00', '2026-09-15 11:30:00', 0);
INSERT INTO `user_team` (`id`, `userId`, `teamId`, `joinTime`, `createTime`, `updateTime`, `isDelete`) VALUES (32, 36, 1, '2026-09-24 21:02:56', '2026-09-24 21:02:56', '2026-09-24 21:02:56', 0);
INSERT INTO `user_team` (`id`, `userId`, `teamId`, `joinTime`, `createTime`, `updateTime`, `isDelete`) VALUES (33, 36, 2, '2026-09-24 21:03:27', '2026-09-24 21:03:27', '2026-09-24 21:03:27', 0);
INSERT INTO `user_team` (`id`, `userId`, `teamId`, `joinTime`, `createTime`, `updateTime`, `isDelete`) VALUES (34, 22, 1, '2026-09-24 21:03:42', '2026-09-24 21:03:42', '2026-09-24 21:03:42', 0);
INSERT INTO `user_team` (`id`, `userId`, `teamId`, `joinTime`, `createTime`, `updateTime`, `isDelete`) VALUES (35, 16, 1, '2026-09-26 22:16:03', '2026-09-26 22:16:02', '2026-09-26 22:16:02', 0);
INSERT INTO `user_team` (`id`, `userId`, `teamId`, `joinTime`, `createTime`, `updateTime`, `isDelete`) VALUES (36, 16, 2, '2026-09-26 22:16:54', '2026-09-26 22:16:53', '2026-09-26 22:16:53', 0);
INSERT INTO `user_team` (`id`, `userId`, `teamId`, `joinTime`, `createTime`, `updateTime`, `isDelete`) VALUES (37, 16, 5, '2026-09-26 22:18:05', '2026-09-26 22:18:04', '2026-09-26 22:18:04', 0);
INSERT INTO `user_team` (`id`, `userId`, `teamId`, `joinTime`, `createTime`, `updateTime`, `isDelete`) VALUES (38, 49, 6, '2026-11-19 20:55:50', '2026-11-19 20:55:49', '2026-11-19 20:55:49', 0);

SET FOREIGN_KEY_CHECKS = 1;
