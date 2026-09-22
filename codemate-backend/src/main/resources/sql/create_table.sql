-- 创建库（标准推荐库名 codemate；若继续使用原有库可保留 matchsystem）
create database if not exists codemate;

-- 切换库
use codemate;

create table if not exists team
(
    id          bigint                             not null auto_increment comment 'id'
        primary key,
    name        varchar(256)                       not null comment '队伍名称',
    description varchar(1024)                      null comment '描述',
    maxNum      int      default 1                 not null comment '最大人数',
    expireTime  datetime                           null comment '过期时间',
    userId      bigint                             null comment '用户id',
    status      int      default 0                 not null comment '0 - 公开，1 - 私有，2 - 加密',
    password    varchar(512)                       null comment '密码',
    createTime  datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint  default 0                 not null comment '是否删除'
)
    comment = '队伍';

create table if not exists user
(
    username     varchar(256)                       null comment '用户昵称',
    id           bigint auto_increment comment 'id'
        primary key,
    userAccount  varchar(256)                       null comment '账号',
    avatarUrl    varchar(1024)                      null comment '用户头像',
    gender       tinyint                            null comment '性别',
    userPassword varchar(512)                       not null comment '密码',
    phone        varchar(128)                       null comment '电话',
    email        varchar(512)                       null comment '邮箱',
    userStatus   int      default 0                 not null comment '状态 0 - 正常',
    createTime   datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime   datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    isDelete     tinyint  default 0                 not null comment '是否删除',
    userRole     int      default 0                 not null comment '用户角色 0 - 普通用户 1 - 管理员',
    planetCode   varchar(512)                       null comment '星球编号',
    tags         varchar(1024)                      null comment '标签 json 列表'
)
    comment = '用户';

create table if not exists user_team
(
    id         bigint auto_increment comment 'id'
        primary key,
    userId     bigint                             null comment '用户id',
    teamId     bigint                             null comment '队伍id',
    joinTime   datetime                           null comment '加入时间',
    createTime datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除'
)
    comment = '用户队伍关系';

create table if not exists chat_message
(
    id              bigint auto_increment comment 'id'
        primary key,
    teamId          bigint                             not null comment '队伍id/聊天室id',
    clientMessageId varchar(64)                        null comment '客户端消息id',
    fromUserId      bigint                             not null comment '发送者id',
    fromUsername    varchar(256)                       null comment '发送者昵称',
    content         varchar(2048)                      not null comment '消息内容',
    createTime      datetime default CURRENT_TIMESTAMP null comment '创建时间',
    isDelete        tinyint  default 0                 not null comment '是否删除'
)
    comment = '聊天消息';

create table if not exists private_chat_session
(
    id              bigint auto_increment comment '会话id'
        primary key,
    user1Id         bigint                             not null comment '用户1 id (较小值)',
    user2Id         bigint                             not null comment '用户2 id (较大值)',
    lastMessage     varchar(2048)                      null comment '最后一条消息摘要',
    lastMessageTime datetime                           null comment '最后消息时间',
    createTime      datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime      datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete        tinyint  default 0                 not null comment '是否删除',
    constraint uk_users
        unique (user1Id, user2Id)
)
    comment = '单人私聊会话';

create table if not exists private_chat_message
(
    id              bigint auto_increment comment '消息id'
        primary key,
    sessionId       bigint                             not null comment '所属会话id',
    clientMessageId varchar(64)                        null comment '客户端消息防重id',
    fromUserId      bigint                             not null comment '发送者id',
    toUserId        bigint                             not null comment '接收者id',
    content         varchar(2048)                      not null comment '消息内容',
    isRead          tinyint  default 0                 not null comment '是否已读 0-未读 1-已读',
    createTime      datetime default CURRENT_TIMESTAMP null comment '发送时间',
    isDelete        tinyint  default 0                 not null comment '是否删除'
)
    comment = '单人私聊消息';

