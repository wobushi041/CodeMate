# AI 编程匹配助手

AI 编程匹配助手是一个面向编程学习和项目协作的伙伴匹配系统。用户可以维护个人资料和技术标签，系统根据标签相似度推荐合适的伙伴；同时提供队伍管理、实时队伍聊天和 AI 编程助手能力。

项目采用前后端分离架构：前端使用 Vue 3 + Vant 构建移动端体验，后端使用 Spring Boot 提供用户、队伍、匹配、聊天和 AI 对话接口。

## 核心功能

- 用户认证：注册、登录、注销、登录态校验。
- 个人资料：昵称、头像、性别、电话、邮箱、标签维护。
- 用户匹配：基于用户标签计算相似度，支持 Redis 缓存匹配结果。
- 用户推荐：首页推荐列表与匹配模式切换。
- 标签搜索：按技术栈、编程语言、职业方向、年级等标签搜索用户。
- 队伍系统：创建队伍、更新队伍、加入队伍、退出队伍、解散队伍。
- 队伍权限：支持公开、加密、私有队伍。
- 队伍聊天：基于 WebSocket 的队伍消息收发。
- AI 编程助手：基于 LangChain4j + 兼容Openapi的流式问答，支持 Markdown 内容渲染。
- 文件上传：支持本地头像上传，并通过静态资源路径访问。

## 技术栈

### 前端

- Vue 3
- TypeScript
- Vite
- Vant 3
- Vue Router
- Axios
- Tailwind CSS
- marked + DOMPurify

### 后端

- Java 21
- Spring Boot 3.5.3
- Spring MVC / WebFlux
- MyBatis-Plus
- MySQL
- Redis / Redisson
- RabbitMQ
- Spring Session Redis
- LangChain4j
- DeepSeek OpenAI 兼容接口
- Knife4j
- Elasticsearch REST Client

## 项目结构

```text
matchsystemwithlangchain/
├── AGENTS.md
├── README.md
├── uploads/
├── matchsystem-frontend/
│   ├── src/
│   │   ├── components/
│   │   ├── config/
│   │   ├── constants/
│   │   ├── layouts/
│   │   ├── models/
│   │   ├── pages/
│   │   ├── plugins/
│   │   ├── services/
│   │   ├── states/
│   │   └── styles/
│   ├── package.json
│   └── vite.config.ts
└── matchsystem-backend/
    ├── src/main/java/com/wobushi041/matchsystem/
    │   ├── chat/
    │   ├── config/
    │   ├── controller/
    │   ├── mapper/
    │   ├── model/
    │   ├── mq/
    │   ├── runner/
    │   ├── service/
    │   └── utils/
    ├── src/main/resources/
    │   ├── mapper/
    │   ├── sql/
    │   ├── application-template.yml
    │   ├── application.yml
    │   └── system-prompt.txt
    └── pom.xml
```

## 环境要求

| 环境 | 建议版本 | 用途 |
| --- | --- | --- |
| JDK | 21+ | 后端运行与构建 |
| Maven | 3.6+ | 后端依赖管理 |
| Node.js | 18+ | 前端开发环境 |
| MySQL | 8.0+ | 主数据库 |
| Redis | 6.x+ | 缓存、分布式能力、Session |
| RabbitMQ | 3.x+ | 推荐缓存预热延时任务 |
| Elasticsearch | 8.x | 中文分词相似度辅助能力 |

## 后端启动

进入后端目录：

```powershell
cd .\matchsystem-backend
```

初始化数据库：

```powershell
# 在 MySQL 中执行：
# src\main\resources\sql\create_table.sql
```

准备配置：

```powershell
Copy-Item .\src\main\resources\application-template.yml .\src\main\resources\application.yml
```

然后修改 `application.yml` 中的 MySQL、Redis、RabbitMQ、DeepSeek 等配置。

启动后端：

```powershell
mvn spring-boot:run
```

默认后端 API 地址：

```text
http://localhost:8080/api
```

Knife4j 文档地址：

```text
http://localhost:8080/api/doc.html
```

## 前端启动

进入前端目录：

```powershell
cd .\matchsystem-frontend
```

安装依赖：

```powershell
npm install
```

启动开发服务：

```powershell
npm run dev
```

前端默认访问地址：

```text
http://localhost:5173
```

前端接口基础地址配置在：

```text
matchsystem-frontend/src/plugins/myAxios.ts
```

## 关键接口

| 模块 | 接口 | 说明 |
| --- | --- | --- |
| 用户 | `POST /api/user/register` | 注册 |
| 用户 | `POST /api/user/login` | 登录 |
| 用户 | `POST /api/user/logout` | 退出登录 |
| 用户 | `GET /api/user/current` | 获取当前登录用户 |
| 用户 | `POST /api/user/update` | 更新个人资料 |
| 匹配 | `GET /api/user/match` | 根据标签匹配用户，优先读取 Redis |
| 匹配 | `GET /api/user/match/withoutRedis` | 跳过 Redis 读取，重新查库计算并写入缓存 |
| 推荐 | `GET /api/user/recommend` | 推荐用户分页列表 |
| 标签 | `GET /api/user/search/tags` | 根据标签搜索用户 |
| 队伍 | `POST /api/team/add` | 创建队伍 |
| 队伍 | `POST /api/team/update` | 更新队伍 |
| 队伍 | `GET /api/team/list` | 查询队伍 |
| 队伍 | `POST /api/team/join` | 加入队伍 |
| 队伍 | `POST /api/team/quit` | 退出队伍 |
| 队伍 | `POST /api/team/delete` | 解散队伍 |
| 文件 | `POST /api/file/upload/avatar` | 上传头像 |

## 匹配与缓存说明

普通匹配接口 `GET /api/user/match` 会优先读取 Redis 缓存，缓存 key 形如：

```text
user:match:{userId}
```

当用户修改自己的标签后，前端会先调用 `POST /api/user/update` 完成资料更新；如果更新字段是 `tags`，再异步调用 `GET /api/user/match/withoutRedis?num=10`。

`matchUsersWithoutRedis` 不读取旧缓存，会重新查询数据库、计算匹配结果，并覆盖 Redis 中的匹配缓存。这样用户保存标签时能立即得到成功反馈，缓存刷新在后台请求中完成。

## 前端页面

| 路由 | 页面 | 说明 |
| --- | --- | --- |
| `/` | `Index.vue` | 首页推荐与匹配模式 |
| `/team` | `TeamPage.vue` | 队伍列表 |
| `/team/add` | `TeamAddPage.vue` | 创建队伍 |
| `/team/update` | `TeamUpdatePage.vue` | 更新队伍 |
| `/team/chat` | `TeamChatPage.vue` | 队伍聊天室 |
| `/search` | `SearchPage.vue` | 标签搜索 |
| `/user/list` | `SearchResultPage.vue` | 搜索结果 |
| `/user` | `UserPage.vue` | 我的页面 |
| `/user/update` | `UserUpdatePage.vue` | 个人资料编辑 |
| `/user/login` | `UserLoginPage.vue` | 登录 |
| `/user/register` | `UserRegisterPage.vue` | 注册 |
| `/user/team/join` | `UserTeamJoinPage.vue` | 我加入的队伍 |
| `/user/team/create` | `UserTeamCreatePage.vue` | 我创建的队伍 |
| `/ai/chat` | `AiChatPage.vue` | AI 编程助手 |

## 构建部署

前端构建：

```powershell
cd .\matchsystem-frontend
npm run build
```

构建产物位于：

```text
matchsystem-frontend/dist
```

后端打包：

```powershell
cd .\matchsystem-backend
mvn clean package
```

运行 Jar：

```powershell
java -jar .\target\matchsystem-0.0.1-SNAPSHOT.jar
```

头像等上传文件默认保存在项目的 `uploads/` 目录。部署到服务器时，需要保证该目录可写，并配置静态资源访问路径。

## 常见问题

### 修改标签后为什么匹配结果没有马上变化？

普通 `/api/user/match` 会优先读取 Redis。标签更新成功后，需要调用 `/api/user/match/withoutRedis` 重新查库计算并刷新缓存。当前前端已经在标签保存成功后异步触发该刷新请求。

### WebSocket 连接失败怎么办？

先确认后端服务已启动、登录态 Cookie 正常携带，并检查前端 WebSocket 地址是否和后端端口一致。若线上部署，还需要确认反向代理支持 WebSocket 升级。

### RabbitMQ 启动后缓存预热不工作怎么办？

检查 RabbitMQ 地址、账号密码、交换机配置，以及延迟消息插件是否启用。推荐缓存预热依赖 RabbitMQ 延时任务。

### 头像上传后无法访问怎么办？

确认后端上传目录存在且有写入权限，并检查静态资源映射是否指向实际的 `uploads/` 目录。

