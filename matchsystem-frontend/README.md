# CodeMate 客户端工程 (matchsystem-frontend)

<div align="center">

![Vue](https://img.shields.io/badge/Vue-3.2.x-brightgreen.svg?style=flat-square)
![TypeScript](https://img.shields.io/badge/TypeScript-4.5+-blue.svg?style=flat-square)
![Vite](https://img.shields.io/badge/Vite-2.9+-purple.svg?style=flat-square)
![Vant](https://img.shields.io/badge/Vant-3.4+-orange.svg?style=flat-square)
![Tailwind CSS](https://img.shields.io/badge/Tailwind-3.4+-38bdf8.svg?style=flat-square)

**CodeMate 智能编程协同平台移动端优先的前端工程**

</div>

---

## 📌 工程概览

`matchsystem-frontend` 是 **CodeMate 智能编程协同平台** 的官方客户端工程，采用 **Vue 3（Composition API） + TypeScript + Vite** 构建，结合 **Vant UI** 与 **Tailwind CSS**，致力于为编程学习者与团队开发者打造流畅、高效且易用的移动端与响应式交互体验。

工程集成了：
- **心动匹配与智能推荐**：双模式切换，毫秒级召回志同道合的技术搭档。
- **队伍协同中心**：支持公开、加密与私有队伍的创建、搜索、加入与成员管理。
- **Netty 实时聊天室**：基于 WebSocket 的多人即时在线沟通与消息状态感知。
- **AI 结对编程助手**：基于 SSE（Server-Sent Events）实现流式逐字打字机效果与安全 Markdown 代码高亮渲染。
- **个性化技术画像**：技术标签选择与自定义、个人资料编辑与头像上传持久化。

---

## 🛠 技术栈

| 模块 | 技术选型 | 版本 | 用途与特性 |
| :--- | :--- | :--- | :--- |
| **前端核心** | Vue 3 | ^3.2.25 | 响应式核心、组合式 API（`<script setup>`） |
| **开发语言** | TypeScript | ^4.5.4 | 类型推断与接口契约校验，增强代码健壮性 |
| **构建工具** | Vite | ^2.9.5 | 极速冷启动与 ESM 原生热更新（HMR） |
| **UI 组件库** | Vant 3 | ^3.4.8 | 针对移动端优化的高品质 Vue 3 组件库 |
| **原子化样式** | Tailwind CSS | ^3.4.19 | 快速构建响应式与高一致性 UI |
| **路由管理** | Vue Router | 4.x | SPA 单页路由管理与页面导航 |
| **网络请求** | Axios | ^0.27.2 | 封装全局拦截器，默认携带 Cookie 会话凭证 |
| **Markdown 渲染** | marked + DOMPurify | ^4.3.0 / ^2.5.8 | AI 助手流式回复解析与 XSS 安全防御 |

---

## 📂 项目结构

```text
src/
├── assets/                    # 静态图片、图标与媒体资源
├── components/                # 全局通用业务组件
│   ├── TeamCardList.vue       # 队伍卡片列表组件（展示成员数、状态、操作按钮）
│   └── UserCardList.vue       # 用户技术画像卡片列表组件
├── config/                    # 路由与全局规则配置
│   └── route.ts               # Vue Router 路由表配置
├── constants/                 # 前端全局业务常量
│   └── team.ts                # 队伍状态枚举（公开、私有、加密）
├── layouts/                   # 页面母版布局
│   └── BasicLayout.vue        # 基础骨架（顶部导航栏、内容区、底部 TabBar）
├── models/                    # TypeScript 类型定义契约
│   ├── team.d.ts              # 队伍实体与 DTO 类型声明
│   └── user.d.ts              # 用户信息与状态类型声明
├── pages/                     # 业务页面视图组件
│   ├── AiChatPage.vue         # CodeMate AI 结对编程助手（SSE 流式会话）
│   ├── Index.vue              # 首页（推荐模式与心动匹配模式切换）
│   ├── SearchPage.vue         # 标签检索与条件过滤页面
│   ├── SearchResultPage.vue   # 标签搜索匹配结果列表页
│   ├── TeamAddPage.vue        # 创建队伍页面
│   ├── TeamChatPage.vue       # Netty 实时多人队伍聊天室页面
│   ├── TeamPage.vue           # 队伍广场（检索、筛选与操作）
│   ├── TeamUpdatePage.vue     # 编辑与更新队伍信息
│   ├── UserEditPage.vue       # 个人单项信息修改页面
│   ├── UserLoginPage.vue      # 账号密码登录页面
│   ├── UserPage.vue           # 个人主页与功能导航入口
│   ├── UserRegisterPage.vue   # 新用户注册页面
│   ├── UserTeamCreatePage.vue # 我创建的队伍列表
│   ├── UserTeamJoinPage.vue   # 我加入的队伍列表
│   └── UserUpdatePage.vue     # 用户综合资料设置与头像上传
├── plugins/                   # 第三方库二次封装
│   └── myAxios.ts             # Axios 实例、baseURL 配置与响应拦截统一错误处理
├── services/                  # 远程 API 请求封装
│   └── user.ts                # 用户相关网络请求函数
├── states/                    # 客户端轻量状态共享
│   └── user.ts                # 当前登录用户响应式缓存
└── styles/                    # 样式规则
    └── tailwind.css           # Tailwind 基础与通用样式指令
```

---

## 🧭 页面路由一览

| 访问路由 | 关联页面组件 | 页面功能说明 | 访问控制 |
| :--- | :--- | :--- | :--- |
| `/` | `Index.vue` | 平台主页：支持“默认推荐”与“心动匹配”模式切换 | 需登录 |
| `/team` | `TeamPage.vue` | 找队伍广场：支持队伍搜索、按公开/加密分类筛选 | 公开浏览 |
| `/team/add` | `TeamAddPage.vue` | 创建队伍：设定队伍名、最大人数、过期时间及密码 | 需登录 |
| `/team/update` | `TeamUpdatePage.vue` | 队伍更新：修改队伍描述、密码与开放状态 | 仅队长 |
| `/team/chat` | `TeamChatPage.vue` | **Netty 实时聊天室**：队伍内多人长连接会话与历史回放 | 队伍成员 |
| `/search` | `SearchPage.vue` | 技能标签搜索：技术栈树形分类、标签筛选与已选折叠 | 公开/登录 |
| `/user/list` | `SearchResultPage.vue` | 搜索结果展示：根据已选标签展示匹配用户列表 | 需登录 |
| `/user` | `UserPage.vue` | 个人中心：查看技术画像、快捷入口及注销操作 | 需登录 |
| `/user/login` | `UserLoginPage.vue` | 用户登录：支持账号密码登录与自动重定向跳转 | 公开 |
| `/user/register` | `UserRegisterPage.vue` | 用户注册：新开发者账号创建 | 公开 |
| `/user/update` | `UserUpdatePage.vue` | 资料编辑：头像实时上传与预览、技能标签更新 | 需登录 |
| `/user/team/join` | `UserTeamJoinPage.vue` | 我的队伍：查看当前用户已加入的所有队伍列表 | 需登录 |
| `/user/team/create` | `UserTeamCreatePage.vue` | 我的管理：查看当前用户作为队长创建的队伍 | 需登录 |
| `/ai/chat` | `AiChatPage.vue` | **AI 编程助手**：SSE 流式实时问答、代码格式化渲染 | 需登录 |

---

## 💡 核心功能交互与实现

### 1. Netty 队伍实时聊天室 (`TeamChatPage.vue`)
- **长连接与协议复用**：建立 `WebSocket` 连接至后端 Netty 服务（`ws://localhost:8081/ws/chat`），复用浏览器已有的 Cookie 凭据，自动完成 Spring Session Redis 握手鉴权。
- **消息收发与心跳保持**：实现定时心跳（Ping/Pong）以防通道假死；监听服务端推送的消息广播，实时追加渲染至聊天界面。
- **历史记录分页与降级无感**：进入聊天室时自动调用 `/api/chat/team/messages` 接口拉取历史消息，透明整合后端 MySQL 与 Redis 降级消息数据，按时间线平滑渲染。

### 2. AI 结对编程助手 (`AiChatPage.vue`)
- **SSE 流式数据消费**：使用 Fetch API / EventSource 接收后端 WebFlux 派发的 `ServerSentEvent` 事件流，实现逐字打印效果。
- **安全富文本渲染**：借助 `marked` 将 Markdown 语法即时转为 HTML，并经由 `DOMPurify` 严格过滤潜在的脚本注入，确保代码块高亮安全可信。

### 3. 网络请求与全局鉴权拦截 (`plugins/myAxios.ts`)
- 配置统一的 `withCredentials = true`，确保跨域请求自动携带分布式 Session Cookie。
- 封装统一响应拦截器：当检测到后端返回未登录错误码（`40100`）时，友好引导跳转至登录页并保留来源地址，便于登录后无感跳回。

---

## 🚀 本地开发与启动

### 1. 安装依赖

确保已安装 **Node.js 18+**，在 `matchsystem-frontend` 目录下执行：
```powershell
npm install
```

### 2. 配置后端 API 地址

如需修改后端地址或端口，可在 `src/plugins/myAxios.ts` 与 `src/pages/TeamChatPage.vue` 中配置：
- HTTP API 地址：默认为 `http://localhost:8080/api`
- WebSocket 地址：默认为 `ws://localhost:8081/ws/chat`

### 3. 运行开发服务器

```powershell
npm run dev
```
启动完成后即可在浏览器中访问：`http://localhost:5173`。

### 4. 生产构建与预览

```powershell
# 1. 构建打包生成 dist 产物
npm run build

# 2. 本地预览构建产物
npm run preview
```

---

## 🎨 样式与组件设计规范

1. **移动端优先**：全站优先遵循移动端屏幕交互逻辑，配合 Vant 移动端组件库，桌面端居中自适应展示。
2. **样式隔离与原子化**：通用卡片、间距、文字层级全面采用 Tailwind CSS 进行快速统一排版，避免全局污染。
3. **视觉一致性**：保持主色调、标签徽章（Tag）、按钮高亮及加载动效的视觉统一。
