# CodeMate 智能编程协同平台

<div align="center">

![Java](https://img.shields.io/badge/Java-21-orange.svg?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-green.svg?style=flat-square)
![Netty](https://img.shields.io/badge/Netty-4.1.118-blue.svg?style=flat-square)
![Vue](https://img.shields.io/badge/Vue-3.x-brightgreen.svg?style=flat-square)
![TypeScript](https://img.shields.io/badge/TypeScript-4.5+-blue.svg?style=flat-square)
![Redis](https://img.shields.io/badge/Redis-6.x+-red.svg?style=flat-square)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.x+-orange.svg?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-lightgrey.svg?style=flat-square)

**面向编程学习者与开发者的智能组队、即时通讯与 AI 协同编程平台**

[系统特性](#-核心特性) • [技术架构](#-系统架构) • [技术栈选型](#-技术选型) • [目录结构](#-目录结构) • [快速开始](#-快速开始) • [核心模块设计](#-核心模块设计) • [关键接口速览](#-关键接口速览)

</div>

---

## 📌 项目简介

**CodeMate 智能编程协同平台** 是一款专为编程学习者、竞赛团队与开源协作者打造的高性能综合协作系统。平台以“**人 - 队 - 智能体**”协同为核心，提供**基于多维特征画像的队友智能匹配引擎**、基于 **Netty** 构建的**高并发多人实时聊天室**，以及集成 **LangChain4j + RAG** 的 **AI 结对编程助手**，全方位提升开发者寻找伙伴、技术沟通与代码协作的效率。

系统采用现代前后端分离架构与反应式网络通信模型，针对高并发读写、分布式一致性、跨协议网络鉴权与服务容灾兜底进行了系统级深度优化。

---

## ✨ 核心特性

- 🎯 **多维智能匹配引擎**：融合**动态规划编辑距离（Levenshtein Distance）**、**余弦相似度（Cosine Similarity）**、**Jaccard 相似度** 与 **Elasticsearch IK 中文语义分词**，多维度精准量化开发者技术栈契合度并提供 Top-K 组队推荐。
- 💬 **Netty 高性能实时聊天室**：基于 Netty 主从 Reactor 多线程模型构建独立 WebSocket 协同服务，支持队伍维度实时会话、分布式 Session 握手鉴权、Channel 属性绑定与生命周期托管，具备“**广播 + 异步持久化 + Redis 降级兜底**”的高可用机制。
- ⚡ **高并发缓存与预热闭环**：采用 Redis 多级缓存与“逻辑过期 + 提前刷新 + TTL 物理兜底”防击穿设计；集成 **RabbitMQ 延迟队列** 构建异步缓存预热闭环，保障高峰期推荐数据毫秒级响应。
- 🔒 **分布式并发与数据一致性**：引入 **Redisson 分布式锁** 保证多节点部署下组队写操作与缓存定时刷新任务的互斥执行，结合死信队列（DLX）构筑系统容错屏障。
- 🤖 **AI 编程助手（RAG + SSE）**：基于 **LangChain4j** 构建 RAG 知识检索增强体系，实现领域文档切块入库、向量召回与 Prompt 动态注入，结合 Server-Sent Events（SSE）实现低延迟流式逐字输出与输入护栏（Guardrail）。
- 👥 **全生命周期队伍管理**：支持公开、加密（密码保护）及私有队伍形态；提供队伍创建、转让、加入、退出、解散及成员权限校验等完整业务流。
- 🖼️ **文件与用户资产服务**：支持用户头像本地存储、UUID 命名规避冲突、文件格式与大小安全校验，并提供静态资源动态映射服务。

---

## 🏗 系统架构

```text
                     +-----------------------------------+
                     |         CodeMate Frontend         |
                     |  Vue 3 + TypeScript + Vant 3 + UI |
                     +-----------------+-----------------+
                                       |
                   HTTP / SSE (8080)   |   WebSocket (8081)
                   +-------------------+-------------------+
                   |                                       |
                   v                                       v
     +---------------------------+           +---------------------------+
     |   Spring Boot Web MVC     |           |    Netty WebSocket Server |
     |  (User, Team, AI, File)   |           |  (Reactor, ChannelManager)|
     +-------------+-------------+           +-------------+-------------+
                   |                                       |
                   |--- Spring Session (Redis) ------------| (跨协议握手鉴权打通)
                   |                                       |
                   v                                       v
     +---------------------------+           +---------------------------+
     |        业务与缓存层        |           |         实时广播与兜底     |
     | - Redis 多级缓存/分布式锁   |           | - 队伍消息内存级广播       |
     | - RabbitMQ 延时预热任务   |           | - MySQL 异步持久化        |
     | - LangChain4j + DeepSeek  |           | - Redis 降级消息列表队列   |
     +-------------+-------------+           +-------------+-------------+
                   |                                       |
                   +-------------------+-------------------+
                                       |
                                       v
     +-------------------------------------------------------------------+
     |                            数据持久层                              |
     |              MySQL 8.0 (持久化) + Redis (缓存/Session)             |
     |              Elasticsearch 8.x (IK分词) / 本地文件对象存储          |
     +-------------------------------------------------------------------+
```

---

## 🛠 技术选型

### 后端技术栈 (codemate-backend)

| 技术 / 组件 | 版本 | 说明 |
| :--- | :--- | :--- |
| **Java** | 21 (LTS) | 核心开发语言，运用虚拟线程与现代语法特性 |
| **Spring Boot** | 3.5.3 | 核心基础服务框架 |
| **Netty** | 4.1.118 | 异步高性能网络通信框架（实现多人长连接聊天室） |
| **MyBatis-Plus** | 3.5.9 | 数据访问持久层增强框架与分页插件 |
| **MySQL** | 8.0+ | 核心关系型数据库 |
| **Redis & Redisson**| 6.x+ / 3.17.7 | 分布式缓存、分布式互斥锁、Session 共享与容灾队列 |
| **RabbitMQ** | 3.x+ | 异步消息中间件与缓存预热延时死信环 |
| **LangChain4j** | 1.1.0 | 大模型编排框架，支持 Prompt 模版与 RAG 向量工程 |
| **Elasticsearch** | 8.x | 标签语义分析切词（IK Analyzer） |
| **Knife4j** | 4.6.0 | OpenAPI 3.0 规范接口聚合文档 |

### 前端技术栈 (codemate-frontend)

| 技术 / 组件 | 版本 | 说明 |
| :--- | :--- | :--- |
| **Vue** | 3.2.x | 渐进式响应式前端框架（Composition API） |
| **TypeScript** | 4.5+ | 静态类型推断与编译期类型保护 |
| **Vite** | 2.9+ | 现代化极速前端构建与热更新构建工具 |
| **Vant UI** | 3.4+ | 移动端交互友好 UI 组件库 |
| **Tailwind CSS** | 3.4+ | 高效原子化样式引擎 |
| **Axios** | 0.27+ | HTTP 异步请求封装与统一响应拦截器 |
| **marked + DOMPurify** | 4.x / 2.x | Markdown 渲染与防 XSS 跨站脚本安全清洗 |

---

## 📂 目录结构

```text
CodeMate/
├── codemate-backend/                        # 后端服务工程 (Spring Boot + Netty)
│   ├── src/main/java/com/wobushi041/codemate/
│   │   ├── CodemateApplication.java         # Spring Boot 启动引导入口
│   │   ├── ai/                              # LangChain4j RAG 与 AI 流式问答模块
│   │   ├── chat/                            # Netty WebSocket 服务器与长连接管理器
│   │   ├── common/                          # 统一结果封装、错误码与通用常量
│   │   ├── config/                          # Redis、RabbitMQ、Redisson、MVC 配置
│   │   ├── controller/                      # RESTful 业务控制器 (User, Team, Chat, AI, File)
│   │   ├── exception/                       # 全局异常捕获与业务异常定义
│   │   ├── mapper/                          # MyBatis-Plus 数据访问层
│   │   ├── model/                           # 领域模型 (Entity, DTO, VO, Request)
│   │   ├── mq/                              # RabbitMQ 延时消息生产与消费者
│   │   ├── runner/                          # 系统启动缓存预热引导器
│   │   ├── service/                         # 核心业务逻辑层与事务实现
│   │   └── utils/                           # 算法工具集 (编辑距离 / 余弦 / IK分词)
│   └── src/main/resources/
│       ├── application-template.yml         # 核心配置模板
│       └── sql/create_table.sql             # 数据库建表与初始数据脚本
├── codemate-frontend/                       # 前端交互工程 (Vue 3 + TypeScript)
│   ├── src/
│   │   ├── components/                      # 公共 UI 组件 (用户卡片、队伍卡片等)
│   │   ├── config/                          # 路由规则表配置 (route.ts)
│   │   ├── constants/                       # 前端全局业务常量
│   │   ├── layouts/                         # 全局页面布局容器
│   │   ├── models/                          # TypeScript 类型声明与接口契约
│   │   ├── pages/                           # 页面视图 (匹配/队伍/聊天室/AI/个人中心)
│   │   ├── plugins/                         # 网络请求客户端与拦截器 (myAxios)
│   │   ├── services/                        # 接口调用层
│   │   └── styles/                          # 全局样式及 Tailwind 配置
│   ├── package.json                         # 前端项目配置与依赖表
│   └── vite.config.ts                       # Vite 构建与服务配置
├── uploads/                                 # 本地静态文件与头像持久化目录
└── README.md                                # 项目综合总览文档
```

---

## 🚀 快速开始

### 1. 环境准备
确保本机环境已安装并正常运行以下中间件：
- **JDK 21+**
- **Node.js 18+** 及 **npm / pnpm**
- **MySQL 8.0+**
- **Redis 6.0+**
- **RabbitMQ 3.8+**（建议开启 `rabbitmq_delayed_message_exchange` 插件）

### 2. 数据库初始化
在 MySQL 中创建数据库并执行建表脚本：
```sql
-- 标准推荐库名 codemate（亦兼容已有 matchsystem 数据库）
CREATE DATABASE IF NOT EXISTS codemate DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE codemate;
-- 执行 codemate-backend/src/main/resources/sql/create_table.sql 脚本
```

### 3. 后端服务启动
```powershell
# 1. 进入后端目录
cd .\codemate-backend

# 2. 根据模板创建实际配置文件
Copy-Item .\src\main\resources\application-template.yml .\src\main\resources\application.yml

# 3. 编辑 application.yml，填入 MySQL、Redis、RabbitMQ 连接参数及大模型 API Key
# 4. 启动后端 Spring Boot + Netty 服务
mvn spring-boot:run
```
- **后端 REST API 端口**：`8080`（访问前缀 `/api`）
- **Netty WebSocket 端口**：`8081`（长连接路径 `/ws/chat`）
- **Knife4j 交互式接口文档**：`http://localhost:8080/api/doc.html`

### 4. 前端工程启动
```powershell
# 1. 打开新终端进入前端工程
cd .\codemate-frontend

# 2. 安装前端项目依赖
npm install

# 3. 启动 Vite 开发服务器
npm run dev
```
- **前端本地访问地址**：`http://localhost:5173`

---

## 💡 核心模块设计

### 1. Netty 多人聊天室架构与协议级鉴权打通
- **生命周期随容器联动**：`NettyWebSocketServer` 实现 Spring `SmartLifecycle` 接口，在容器启动后以异步非阻塞模式拉起 Boss/Worker 线程池，并在上下文销毁时平滑关闭长连接通道。
- **握手鉴权与 Session 共享**：编写 `SessionHandshakeAuthHandler` 拦截 WebSocket HTTP 握手升级请求，读取客户端请求头 Cookie 中的 Session ID，直连 Redis 解析 Spring Session 获取当前登录用户，未登录拒绝握手，实现长短连接鉴权无缝统一。
- **高可用降级与容灾**：队伍聊天消息首先在内存中由 `ChatChannelManager` 进行组内广播，随后异步写入 MySQL。当数据库负载突增写入失败时，消息自动暂存入 Redis List；拉取历史消息时自动合并 MySQL 与 Redis 降级数据，确保实时性与可靠性兼备。

### 2. 多维智能组队匹配引擎
- **特征向量归一化**：将用户填写的技术标签分拆过滤，借助 Elasticsearch IK 分词器进行细粒度语义切词。
- **复合相似度算法**：综合计算两两用户标签集的**最小编辑距离（Levenshtein Distance）**与**词频余弦夹角相似度**，综合衡量技术栈重合度与互补度，支持 Top-K 毫秒级推荐。
- **双重刷新防击穿**：用户修改技术标签即刻异步触发重算，并同步更新 Redis 匹配结果缓存（`user:match:{userId}`），解决高频检索下的计算性能瓶颈。

### 3. 缓存预热与延时刷新闭环
- **启动预热保护**：通过 `CommandLineRunner` 在应用启动完成时自动检索热点活跃用户，预先加载推荐与匹配数据入 Redis。
- **延时队列解耦**：通过 RabbitMQ 延时消息队列周期性投递预热任务，配合 Redisson 分布式锁保障多副本部署下仅由单一实例执行刷新，实现零波动的无感缓存续期。

### 4. AI 结对编程助手与 RAG
- **智能工程化**：基于 LangChain4j 接入大语言模型（如 DeepSeek 等），将编程知识库切块向量化。
- **低延迟流式交互**：借助 Spring WebFlux 与 Server-Sent Events（SSE），实现逐字输出流式对话与上下文多轮保留，为开发者提供实时的代码答疑与组队建议。

---

## 📑 关键接口速览

| 业务分类 | 通信协议 | 接口路径 | 权限要求 | 功能描述 |
| :--- | :--- | :--- | :--- | :--- |
| **用户认证** | HTTP POST | `/api/user/login` | 公开 | 用户名密码登录并创建分布式 Session |
| **用户认证** | HTTP POST | `/api/user/register` | 公开 | 新用户注册 |
| **用户认证** | HTTP POST | `/api/user/logout` | 需登录 | 注销当前用户登录状态 |
| **用户画像** | HTTP GET | `/api/user/current` | 需登录 | 获取当前登录用户详细信息与标签 |
| **用户推荐** | HTTP GET | `/api/user/recommend` | 需登录 | 分页获取推荐开发者列表（优先缓存） |
| **用户匹配** | HTTP GET | `/api/user/match` | 需登录 | 基于技术标签的多维相似度 Top-K 匹配 |
| **队伍管理** | HTTP POST | `/api/team/add` | 需登录 | 创建队伍（公开 / 加密 / 私有） |
| **队伍管理** | HTTP POST | `/api/team/join` | 需登录 | 申请加入队伍（支持密码校验） |
| **队伍管理** | HTTP POST | `/api/team/quit` | 需登录 | 退出队伍（队长退出自动顺延转让） |
| **实时聊天** | WebSocket | `ws://localhost:8081/ws/chat` | 握手鉴权 | Netty 多人实时协同与即时消息广播通道 |
| **聊天历史** | HTTP GET | `/api/chat/team/messages` | 需登录 | 分页拉取队伍历史消息（含 Redis 降级消息） |
| **AI 协同** | HTTP/SSE | `/api/ai/chat/stream` | 需登录 | 编程助手 SSE 流式问答（RAG 知识库增强） |
| **文件存储** | HTTP POST | `/api/file/upload/avatar` | 需登录 | 用户头像上传与本地持久化 |

---

## 📄 开源许可证

本项目遵循 [MIT License](LICENSE) 开源协议。欢迎贡献代码与提 Issue 交流！
