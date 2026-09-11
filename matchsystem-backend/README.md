# CodeMate 服务端工程 (matchsystem-backend)

<div align="center">

![Java](https://img.shields.io/badge/Java-21-orange.svg?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-green.svg?style=flat-square)
![Netty](https://img.shields.io/badge/Netty-4.1.118-blue.svg?style=flat-square)
![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5.9-blueviolet.svg?style=flat-square)
![Redis](https://img.shields.io/badge/Redis-6.x+-red.svg?style=flat-square)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.x+-orange.svg?style=flat-square)
![LangChain4j](https://img.shields.io/badge/LangChain4j-1.1.0-brightgreen.svg?style=flat-square)

**CodeMate 智能编程协同平台核心后端服务**

</div>

---

## 📌 工程概览

`matchsystem-backend` 是 **CodeMate 智能编程协同平台** 的服务端工程，基于 **Java 21** 与 **Spring Boot 3.5.3** 构建，深度整合 **Netty 4.1.118** 高性能异步网络框架，提供支撑平台业务流转的核心能力：
1. **多维特征组队匹配**：融合动态规划编辑距离、向量余弦相似度与 Elasticsearch IK 语义分词。
2. **高并发多人聊天室**：基于 Netty 主从 Reactor 线程模型构建 WebSocket 服务，打通 Spring Session Redis 鉴权，支持双通道容灾。
3. **缓存预热与一致性体系**：基于 RabbitMQ 延迟队列与 Redisson 分布式锁构建热点数据自动刷新闭环。
4. **AI 结对编程助手**：基于 LangChain4j 实现本地知识库 RAG 增强检索与 SSE 响应式流式对话。
5. **资产存储服务**：提供用户头像上传校验、防重命名与静态文件映射。

---

## 🛠 技术栈清单

| 类别 | 技术组件 | 版本 | 核心职责 |
| :--- | :--- | :--- | :--- |
| **基础语言与环境** | OpenJDK | 21 (LTS) | 语言运行时，采用虚拟线程特性与现代语法 |
| **应用框架** | Spring Boot | 3.5.3 | 核心 IoC 容器、MVC 架构与组件自动装配 |
| **网络长连接通信** | Netty | 4.1.118 | 构建独立高并发 WebSocket 聊天室服务（8081 端口） |
| **响应式支持** | Spring WebFlux | 3.5.3 | SSE（Server-Sent Events）流式事件推送 |
| **ORM / 数据访问** | MyBatis-Plus | 3.5.9 | 数据持久化、通用 CRUD 与物理分页插件 |
| **数据库** | MySQL | 8.0+ | 核心关系型数据持久存储 |
| **分布式缓存/锁** | Redis & Redisson | 6.x+ / 3.17.7 | Spring Session 集中共享、热点缓存、分布式锁与降级队列 |
| **消息队列** | RabbitMQ | 3.x+ | 延时消息路由、定时缓存预热与任务解耦 |
| **大模型编排** | LangChain4j | 1.1.0 | 模型适配（DeepSeek / OpenAI 兼容）、RAG 知识检索 |
| **语义切词** | Elasticsearch IK | 8.x | 中文技术标签切词分析（IK Analyzer） |
| **文档与调试** | Knife4j | 4.6.0 | OpenAPI 3.0 规范交互式接口文档 |

---

## 📂 源码模块结构

```text
src/main/java/com/wobushi041/matchsystem/
├── MatchsystemApplication.java            # Spring Boot 启动主类
├── ai/                                    # LangChain4j 与 AI 协同模块
│   ├── AiChatService.java                 # AI 对话与 RAG 业务接口
│   ├── AiChatServiceFactory.java          # 对话服务工厂类
│   ├── guardrail/                         # 输入安全护栏与敏感词拦截
│   ├── listener/                          # 大模型交互事件监听器
│   ├── model/                             # 模型参数配置
│   └── rag/                               # 知识库切块与向量召回配置
├── chat/                                  # Netty WebSocket 聊天室核心包
│   ├── ChatAttributes.java                # Channel 属性常量（UserId, TeamId 等）
│   ├── ChatChannelManager.java            # 队伍长连接通道群组管理器
│   ├── ChatConnectionHandler.java         # 业务处理 Handler（入队、广播、心跳）
│   ├── ChatInboundMessage.java            # 前端入站消息模型
│   ├── ChatMessageResponse.java           # 广播出站消息模型
│   ├── NettyWebSocketProperties.java      # Netty 端口与路径配置类
│   ├── NettyWebSocketServer.java          # Netty 服务端（实现 SmartLifecycle 随容器启停）
│   └── SessionHandshakeAuthHandler.java   # HTTP 握手拦截器（直查 Redis 鉴权 Session）
├── common/                                # 统一公共模块
│   ├── BaseResponse.java                  # 统一 API 响应包装类
│   ├── ErrorCode.java                     # 系统错误码枚举
│   └── ResultUtils.java                   # 结果快速构造工具
├── config/                                # 组件配置中心
│   ├── Knife4jConfig.java                 # 接口文档分组与元数据
│   ├── MyBatisPlusConfig.java             # 分页拦截器配置
│   ├── RabbitMqConfig.java                # 延时队列、交换机与 Binding 配置
│   ├── RedissonConfig.java                # Redisson 单机/集群客户端配置
│   ├── RedisTemplateConfig.java           # RedisTemplate 序列化配置
│   └── WebMvcConfig.java                  # 跨域 CORS 与静态资源映射
├── controller/                            # RESTful 控制器
│   ├── AiController.java                  # AI 编程助手接口（流式/同步）
│   ├── ChatController.java                # 历史聊天记录拉取接口
│   ├── FileController.java                # 文件与头像上传接口
│   ├── TeamController.java                # 队伍增删改查与加入/退出接口
│   └── UserController.java                # 用户认证、推荐、标签匹配接口
├── exception/                             # 异常体系
│   ├── BusinessException.java             # 业务运行时异常
│   └── GlobalExceptionHandler.java        # 全局异常捕获器
├── mapper/                                # MyBatis 数据访问接口
│   ├── ChatMessageMapper.java             # 聊天消息 Mapper
│   ├── TeamMapper.java                    # 队伍 Mapper
│   ├── UserMapper.java                    # 用户 Mapper
│   └── UserTeamMapper.java                # 用户-队伍关联关系 Mapper
├── model/                                 # 领域对象模型
│   ├── domain/                            # 数据库持久化实体类 (User, Team, UserTeam, ChatMessage)
│   ├── dto/                               # 数据传输对象 (UserDTO, TeamQueryDTO 等)
│   ├── enums/                             # 队伍状态、用户角色等枚举
│   ├── request/                           # 控制层入参请求封装 (UserLoginRequest, TeamAddRequest)
│   └── vo/                                # 视图展示包装对象 (UserVO, TeamUserVO)
├── mq/                                    # 消息队列组件
│   ├── CacheWarmupConsumer.java           # 延时预热消息监听与消费
│   └── CacheWarmupProducer.java           # 预热任务延时投递器
├── runner/                                # 系统引导任务
│   └── CacheWarmupBootstrapRunner.java    # 启动时冷启动数据预热器
├── service/                               # 业务接口与实现
│   ├── impl/                              # 业务实现类集合
│   ├── AiChatService.java                 # AI 业务接口
│   ├── ChatMessageService.java            # 消息持久化与降级拉取接口
│   ├── RecommendCacheService.java         # 推荐缓存读写服务
│   ├── TeamService.java                   # 队伍全生命周期业务
│   ├── UserService.java                   # 用户认证与特征匹配
│   └── UserTeamService.java               # 用户-队伍关系服务
└── utils/                                 # 算法与通用工具类
    └── AlgorithmUtils.java                # 动态规划编辑距离、余弦相似度计算
```

---

## 💡 核心机制与深度实现

### 1. Netty 多人聊天室与协议级鉴权
- **统一登录态鉴权**：系统使用 Spring Session Redis 实现分布式会话。当客户端通过 WebSocket 请求升级长连接时，`SessionHandshakeAuthHandler` 拦截 HTTP 握手帧，从 `Cookie` 头提取 `SESSION` 值，直接查询 Redis 中存储的会话反序列化用户上下文；未登录则直接返回 401 拦截握手。
- **Channel 组与心跳管理**：长连接建立后，`ChatChannelManager` 将对应 `Channel` 绑定用户 ID、用户昵称、头像及所属队伍 TeamId，通过 Netty 自带的 `ChannelGroup` 维护当前队伍的活跃连接列表。
- **高可用双通道持久化与容灾**：
  1. 用户发送消息后，由 Netty 立即广播给当前队伍内所有在线成员；
  2. 异步调用 `ChatMessageService` 写入 MySQL；
  3. 若数据库写入发生瞬时异常，自动触发兜底逻辑将消息写入 Redis List 缓存；
  4. 客户端拉取历史记录时，接口自动合并 MySQL 与 Redis 降级数据，保障消息零丢失。

### 2. 多维相似度组队匹配算法
- **标签清洗与向量化**：支持用户自定义技能与方向标签（如 `["Java", "Spring Boot", "算法竞赛"]`），利用 Elasticsearch IK 分词对中文语义标签进行深层切词与停用词过滤。
- **复合契合度评分**：针对标签集合计算：
  $$\text{Score} = \alpha \times (1 - \frac{\text{Levenshtein}(A, B)}{\max(|A|, |B|)}) + \beta \times \text{CosineSimilarity}(V_A, V_B)$$
- **变更异步刷新**：用户画像技术标签发生变更时，异步调度匹配重算任务并更新 Redis 缓存键 `user:match:{userId}`，避免高峰期匹配计算拖垮数据库。

### 3. 延时队列驱动的缓存预热闭环
- **冷启动保障**：`CacheWarmupBootstrapRunner` 在服务启动就绪后自动拉取主站活跃用户，完成首轮推荐数据缓存加载。
- **延时环流调度**：`CacheWarmupProducer` 向 RabbitMQ 发送设置了延迟 TTL 的预热消息。
- **分布式锁防重**：`CacheWarmupConsumer` 接收到消息后，先通过 `Redisson` 竞争分布式锁 `lock:cache_warmup`，获取锁成功的节点执行数据重新加载并发送下一轮延时消息，未抢到锁的节点平滑跳过，保证集群环境下预热任务唯一执行。

### 4. LangChain4j + RAG AI 编程助手
- **知识库向量切块**：将编程协作规范、队伍指南与常见技术问题进行语义分块并嵌入。
- **流式响应输出**：集成 DeepSeek / OpenAI 兼容接口，控制层采用 WebFlux `Flux<ServerSentEvent<String>>` 封装模型流式事件，前端无延迟逐字渲染。
- **输入护栏 Guardrail**：在 Prompt 构建前增加安全防护拦截机制，防范提示词注入与越权行为。

---

## 📑 接口全景清单

### 1. 用户模块 (`/api/user`)

| 方法 | 接口路径 | 说明 | 权限 |
| :--- | :--- | :--- | :--- |
| `POST` | `/register` | 新用户注册（账号、密码、校验码） | 公开 |
| `POST` | `/login` | 账号密码登录，下发分布式 Session | 公开 |
| `POST` | `/logout` | 注销登录，销毁 Session | 需登录 |
| `GET` | `/current` | 获取当前登录用户的详细信息与标签 | 需登录 |
| `GET` | `/recommend` | 分页获取系统推荐用户（多级缓存支持） | 需登录 |
| `GET` | `/match` | 标签多维相似度匹配召回 Top-K 开发者 | 需登录 |
| `GET` | `/search/tags` | 根据指定标签列表精确检索用户 | 需登录 |
| `POST` | `/update` | 更新当前用户昵称、个人简介、标签、头像等 | 需登录 |
| `GET` | `/search` | 条件查询用户列表（分页） | 管理员 |
| `POST` | `/delete` | 逻辑删除指定用户 | 管理员 |

### 2. 队伍模块 (`/api/team`)

| 方法 | 接口路径 | 说明 | 权限 |
| :--- | :--- | :--- | :--- |
| `POST` | `/add` | 创建队伍（公开/加密/私有，设最大人数与过期时间） | 需登录 |
| `POST` | `/update` | 修改队伍基本信息（仅队长或管理员可操作） | 需登录 |
| `GET` | `/get` | 根据 ID 查询指定队伍详情 | 公开 |
| `GET` | `/list` | 组合条件分页检索队伍列表 | 公开 |
| `POST` | `/join` | 加入队伍（加密队伍需验证队伍密码） | 需登录 |
| `POST` | `/quit` | 退出队伍（队长退出自动将队长顺延给先加入的成员） | 需登录 |
| `POST` | `/delete` | 解散队伍（仅队长可操作，自动解绑所有关联成员） | 需登录 |
| `GET` | `/list/my/create` | 查询当前用户所创建的队伍列表 | 需登录 |
| `GET` | `/list/my/join` | 查询当前用户已加入的队伍列表 | 需登录 |

### 3. 聊天与消息模块 (`/api/chat` & Netty)

| 协议 | 接口路径 | 说明 | 权限 |
| :--- | :--- | :--- | :--- |
| `WS` | `ws://localhost:8081/ws/chat` | Netty WebSocket 实时双向通信通道 | Session 握手鉴权 |
| `GET` | `/api/chat/team/messages` | 分页拉取队伍聊天历史记录（MySQL + Redis 降级数据） | 需登录 |

### 4. AI 编程协同模块 (`/api/ai`)

| 方法 | 接口路径 | 说明 | 权限 |
| :--- | :--- | :--- | :--- |
| `POST` | `/chat/stream` | Server-Sent Events (SSE) 结对编程助手流式对话 | 需登录 |
| `POST` | `/chat` | 同步阻塞式对话请求 | 需登录 |

### 5. 文件与资产模块 (`/api/file`)

| 方法 | 接口路径 | 说明 | 权限 |
| :--- | :--- | :--- | :--- |
| `POST` | `/upload/avatar` | 用户个人头像上传（支持 jpg/png/webp，限制大小） | 需登录 |

---

## 🚀 本地开发与启动

### 1. 依赖与配置准备

1. **数据库初始化**：
   在 MySQL 中执行脚本：`src/main/resources/sql/create_table.sql`。
2. **复制配置模板**：
   ```powershell
   Copy-Item .\src\main\resources\application-template.yml .\src\main\resources\application.yml
   ```
3. **编辑 `application.yml` 关键参数**：
   - `spring.datasource`：MySQL 用户名、密码及连接串。
   - `spring.data.redis`：Redis 主机、端口、密码与 Database。
   - `spring.rabbitmq`：RabbitMQ 主机、端口、用户名与密码。
   - `langchain4j.open-ai`：配置 DeepSeek / OpenAI 兼容 Base-Url 与 API-Key。
   - `netty.websocket.port`：默认 `8081`。

### 2. 启动服务

在后端根目录下执行：
```powershell
mvn spring-boot:run
```
启动成功后可看到控制台输出：
- Spring Boot HTTP Web 服务监听于：`8080` 端口
- Netty WebSocket 服务就绪于：`8081` 端口
- Knife4j 接口交互文档：`http://localhost:8080/api/doc.html`

---

## 📦 生产打包与运行

```powershell
# 1. 编译并打包 JAR
mvn clean package -DskipTests

# 2. 运行生产 JAR
java -jar -Dspring.profiles.active=prod .\target\matchsystem-0.0.1-SNAPSHOT.jar
```
生产环境部署推荐通过环境变量传入数据库及中间件密码：
```powershell
java -Dspring.datasource.password="your_pwd" -Dspring.data.redis.password="your_pwd" -jar .\target\matchsystem-0.0.1-SNAPSHOT.jar
```