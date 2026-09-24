# CodeMate 后端

CodeMate 的 Spring Boot 服务，负责用户、匹配、队伍、消息与 AI 助手接口，并通过 Netty 提供聊天 WebSocket。项目整体介绍见[仓库根 README](../README.md)。

## 技术与服务

| 组件 | 职责 |
| --- | --- |
| Java 21、Spring Boot 3.5.3、MyBatis-Plus | HTTP 接口与数据访问 |
| MySQL | 用户、队伍和聊天消息等持久化数据 |
| Redis、Spring Session、Redisson | 会话、缓存与分布式协调 |
| RabbitMQ | 异步消息与缓存预热任务 |
| Netty | 私聊和队伍聊天的 WebSocket 连接 |
| Elasticsearch IK | 技术标签分词 |
| LangChain4j | 聊天模型接入与知识检索增强；AI 回复通过 SSE 输出 |

HTTP 服务默认监听 `8080`，接口前缀为 `/api`；WebSocket 服务默认监听 `8091`，路径为 `/ws/chat`。这些值来自 [`application-template.yml`](src/main/resources/application-template.yml)，以实际部署配置为准。

## 本地启动

以下步骤以 Windows PowerShell、`codemate-backend` 目录为起点。

1. 准备 JDK 21、MySQL、Redis 和 RabbitMQ。在 MySQL 客户端执行 [`src/main/resources/sql/create_table.sql`](src/main/resources/sql/create_table.sql)，脚本会创建并选中 `codemate` 数据库。
2. 如本地尚无 `src/main/resources/application.yml`，复制 [`application-template.yml`](src/main/resources/application-template.yml) 为 `application.yml`。按环境填写 MySQL、Redis、RabbitMQ 等连接信息；使用 AI 功能时配置大模型 API Key 与 Ollama 向量模型，使用标签分词时准备 Elasticsearch IK。不要覆盖已有本地配置，也不要提交实际密钥。
3. 使用项目自带的 Maven Wrapper 启动：

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

服务启动后，API 基础地址为 `http://localhost:8080/api`，Knife4j 接口文档地址为 `http://localhost:8080/api/doc.html`。需要联调页面时，再按[前端 README](../codemate-frontend/README.md)启动客户端。

## 主要接口

下表路径均以 `/api` 为前缀。实际入参、响应与权限要求以控制器和接口文档为准；需要登录的请求须携带有效会话。

| 模块 | 方法与路径 | 用途 |
| --- | --- | --- |
| 用户 | `POST /user/register`、`POST /user/login` | 注册、登录 |
| 用户 | `GET /user/recommend`、`GET /user/match` | 推荐与匹配 |
| 队伍 | `GET /team/list`、`POST /team/add`、`POST /team/join` | 浏览、创建、加入队伍 |
| 聊天 | `GET /chat/teams/{teamId}/messages` | 队伍聊天记录 |
| 私聊 | `POST /chat/private/start`、`GET /chat/private/messages` | 创建会话、查看消息 |
| AI | `GET /ai/chat?message=...` | SSE 流式对话 |
| AI | `POST /ai/chat` | 同步对话 |
| 文件 | `POST /file/upload/avatar` | 上传头像 |

实时聊天连接地址为 `ws://localhost:8091/ws/chat`，不属于上述 HTTP `/api` 路径。

## 源码位置

```text
src/main/
├─ java/com/wobushi041/codemate/
│  ├─ controller/   # HTTP 接口
│  ├─ service/      # 业务逻辑
│  ├─ chat/         # Netty WebSocket
│  ├─ ai/           # AI 对话与知识检索
│  ├─ mapper/       # 数据访问
│  └─ config/       # 基础组件配置
└─ resources/
   ├─ application-template.yml
   └─ sql/create_table.sql
```

## 相关文档

- [项目总览](../README.md)
- [前端说明](../codemate-frontend/README.md)
