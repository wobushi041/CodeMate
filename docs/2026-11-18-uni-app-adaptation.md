# CodeMate uni-app 多端改造计划

- 日期：2026-11-18
- 状态：待评审
- 目标平台：微信小程序、Android/iOS App；现有 H5 继续保留
- 推荐技术栈：uni-app + Vue 3 + TypeScript + Vite + Pinia + wot-design-uni

## 1. 背景与结论

CodeMate 当前由 Vue 3 H5 前端和 Spring Boot 后端组成，业务覆盖用户画像、伙伴匹配、队伍协作、私聊、队伍聊天和 AI 流式对话。

改造采用“保留现有 H5、新增 uni-app 工程、共享后端”的方式，不直接将 `codemate-frontend` 改写为 uni-app。优先完成登录、网络、WebSocket、AI 流式和上传五项技术验证，再逐页迁移，避免页面完成后才发现底层能力不可用。

## 2. 目标与非目标

### 2.1 目标

1. 新增 `codemate-uniapp`，支持微信小程序，并为 App 打包保留统一工程。
2. 复用现有 Spring Boot API、Netty WebSocket、数据库和业务协议。
3. 覆盖现有 16 个页面及核心业务流程。
4. 支持 Token 鉴权，同时保持现有 Web Session Cookie 登录兼容。
5. 支持聊天断线重连、页面切后台恢复和 AI 流式输出。
6. 满足小程序包体积、合法域名、HTTPS/WSS 和真机适配要求。

### 2.2 非目标

1. 不重构现有后端业务模型、队伍流程和匹配算法。
2. 不在第一阶段使用 nvue、uni-app x 或重写为原生小程序。
3. 不要求 H5、小程序和 App 页面代码 100% 共用。
4. 不在本期建设完整运营后台、支付、商业化和全平台推送体系。
5. 不为了“统一”而强行消除所有条件编译。

## 3. 现状与关键约束

| 领域 | 当前实现 | 改造要求 |
| --- | --- | --- |
| 前端工程 | Vue 3 + Vite + vue-router + Vant 3 + Tailwind | 新建 uni-app 工程，页面和 UI 分层迁移 |
| 登录态 | Spring Session + `SESSION` Cookie | 新增 Bearer Token，保留 Web Cookie |
| HTTP | axios，`withCredentials=true` | 使用 `uni.request` 封装统一请求层 |
| 聊天 | 浏览器 WebSocket 连接 Netty `:8091/ws/chat` | 使用 `uni.connectSocket`，生产环境通过 Nginx 反代 WSS |
| 握手鉴权 | `SessionHandshakeAuthHandler` 只读取 Cookie | 增加 Token 鉴权，不破坏现有 Session 鉴权 |
| 聊天稳定性 | 无完整心跳和断线重连 | 增加心跳、重连、重新 JOIN、页面生命周期处理 |
| AI 流式 | `fetch` + `response.body.getReader()` 解析 SSE | H5、微信小程序、App 分端适配 chunked 流式读取 |
| AI 输入 | `GET /ai/chat?message=...` | 增加 POST 流式接口，避免长文本 URL 限制 |
| AI 富文本 | marked + DOMPurify + `v-html` | 替换为跨端 Markdown/富文本渲染方案 |
| 头像 | Vant Uploader + FormData，本地磁盘存储 | 改为 `uni.chooseMedia` + `uni.uploadFile`，生产建议使用对象存储 |
| 路由 | `vue-router` + `BasicLayout` | 改为 `pages.json`、原生 tabBar 和 uni 页面跳转 |
| 静态资源 | assets 约 11.59 MB，字体约 7.5 MB | 大字体不进入主包，图片压缩并优先 CDN 化 |
| 发布 | 本地 API 和 WebSocket 地址 | 生产使用备案域名、HTTPS/WSS 和合法域名配置 |

## 4. 总体方案

### 4.1 工程结构

```text
CodeMate/
├─ codemate-frontend/          # 保留现有 H5
├─ codemate-uniapp/            # 新增多端工程
│  ├─ src/
│  │  ├─ pages/                # tab 主页面
│  │  ├─ subpages/             # 登录、聊天、编辑等分包页面
│  │  ├─ components/
│  │  ├─ api/                  # 业务 API 定义
│  │  ├─ request/              # HTTP 请求和异常处理
│  │  ├─ realtime/             # WebSocket 与 AI 流式适配
│  │  ├─ store/                # Pinia 用户状态
│  │  ├─ types/
│  │  ├─ utils/
│  │  ├─ static/
│  │  ├─ App.vue
│  │  ├─ main.ts
│  │  ├─ pages.json
│  │  └─ manifest.json
│  ├─ package.json
│  └─ vite.config.ts
└─ codemate-backend/           # 复用并做兼容性适配
```

### 4.2 页面映射

| 现有页面 | uni-app 目标页面 | 包策略 |
| --- | --- | --- |
| `Index.vue` | `pages/index/index` | 主包 tab |
| `MatchPage.vue` | `pages/match/index` | 主包 tab |
| `TeamPage.vue` | `pages/team/index` | 主包 tab |
| `AiChatPage.vue` | `pages/ai/index` | 主包 tab，体积超限时可拆包 |
| `UserPage.vue` | `pages/user/index` | 主包 tab |
| 登录、注册 | `subpages/auth/login`、`register` | 分包 |
| 私聊、队伍聊天 | `subpages/chat/private`、`team` | 分包 |
| 创建、更新队伍 | `subpages/team/add`、`update` | 分包 |
| 搜索、用户列表 | `subpages/search/index`、`result` | 分包 |
| 资料编辑、我的队伍 | `subpages/user/*` | 分包 |

原生 tabBar 顺序保持：主页、匹配、队伍、AI 助手、我的。

### 4.3 复用边界

可直接迁移或少量修改：数据类型、接口路径和参数、标签处理、时间格式化、队伍状态判断、卡片业务逻辑、AI 打字机队列思路。

必须替换：Vant H5、vue-router、axios、浏览器 WebSocket、流式 fetch、DOM/BOM、localStorage、`v-html`、Lucide Vue 图标、H5 滚动和视口布局。

## 5. 分阶段实施计划

### 阶段 0：发布前置与技术验证

#### 任务 0.1：确认非技术前置条件

- 确认小程序主体、类目和资质可办理。
- 确认域名、ICP 备案、HTTPS 证书和生产服务器方案。
- 确认 UGC 文本/图片内容安全审核方案。
- 确认 App 开发者账号、隐私政策和上架责任方。

验收：形成明确结论；若主体或类目不可行，暂停小程序全面迁移，仅保留技术 Demo。

#### 任务 0.2：建立四个最小验证 Demo

1. 登录获取 Token，并请求当前用户接口。
2. WebSocket 完成连接、JOIN、消息收发和断线重连。
3. AI 接口在目标端逐段返回文本。
4. 选择图片、上传头像并正确显示。

验收：微信开发者工具和至少一台真机通过；App 目标确定后再验证 Android 真机。任何一项不通过，先解决再进入页面迁移。

### 阶段 1：后端兼容与网络基础

#### 任务 1.1：实现 Token 双轨鉴权

建议改动位置：

- `UserController`：登录响应按客户端类型返回 Token，或提供独立 Token 登录接口。
- `UserServiceImpl.getLoginUserFromRequest`：优先识别 Bearer Token，无 Token 时回退原 Session。
- 新增 Token 生成、存储、过期和注销机制；可复用 Redis，避免另建会话体系。
- `SessionHandshakeAuthHandler`：支持从握手请求头或受控参数解析 Token。

要求：

- Web 原有 Cookie 登录不受影响。
- Token 不写入 URL 日志；生产 WebSocket 优先通过请求头传递。
- 40100 语义保持一致。
- 不在仓库提交真实密钥和配置。

验收：同一账号可分别通过 Cookie 和 Token 访问受保护接口；错误、过期、注销 Token 均不能访问。

#### 任务 1.2：统一生产网关

规划：

```text
https://api.example.com/api/*  -> Spring Boot
wss://api.example.com/ws/chat  -> Netty 127.0.0.1:8091
```

Nginx WebSocket 配置需支持 Upgrade/Connection 请求头和合理读超时。头像经反向代理访问时，正确处理转发协议和 Host，避免生成内网 URL。

验收：HTTPS API、WSS、头像 URL 均可从公网访问；小程序不直接使用 `:8080`、`:8091` 端口。

#### 任务 1.3：聊天可靠性改造

- 定义客户端 ping、服务端 pong 或应用层心跳。
- Netty 增加空闲连接检测。
- 客户端实现指数退避重连，限制最大间隔。
- 重连后重新发送 JOIN。
- 使用连接实例 ID 防止旧 Socket 回调修改新连接状态。
- 页面 `onShow` 检查连接，`onHide`/`onUnload` 按策略断开或保活。
- 处理重复消息和 `clientMessageId`。

验收：断网、切后台、锁屏和 Nginx 空闲超时场景下可恢复；不出现重复连接和重复消息。

#### 任务 1.4：AI 流式接口兼容

- 新增 `POST /ai/chat/stream`（名称可调整），请求体携带 `message`。
- 保留统一 `data:` SSE 格式和原有用户标签上下文逻辑。
- 保留旧 GET 接口直到 H5 完成迁移。
- 明确空回复、异常、取消请求和连接中断处理。

验收：长文本不经过 URL；H5 原有功能不回归；小程序验证 Demo 能连续显示流式内容。

#### 任务 1.5：上传兼容

- 上传接口支持 Bearer Token。
- 保留文件类型、2 MB 大小和路径安全校验。
- 生产头像优先迁移 OSS/COS；暂不迁移时保证公网 URL 正确。
- 配置 uploadFile/downloadFile 合法域名。

验收：小程序和 App 上传后返回可直接加载的 HTTPS 图片地址。

### 阶段 2：uni-app 工程骨架

#### 任务 2.1：创建工程

创建 `codemate-uniapp`，使用 Vue 3 + TypeScript + Vite，接入 Pinia 和 wot-design-uni。保留 CLI 小程序构建能力，使用 HBuilderX 完成 App 真机调试和打包。

验收：空工程可运行到微信开发者工具；Vue 版本、组件库和 TypeScript 配置一致。

#### 任务 2.2：环境与配置管理

至少区分：

- 本地开发：H5/模拟器使用本机服务。
- 真机调试：使用局域网地址或测试域名。
- 生产环境：统一 HTTPS/WSS 域名。

API、WS、上传地址集中管理，禁止在页面散落 `localhost`。

验收：无需修改页面代码即可切换环境；仓库不包含生产密钥。

#### 任务 2.3：统一请求层

实现：

- `request<T>()` 包装 `uni.request`。
- 自动附加 Token。
- 统一处理 `{ code, data, description }`。
- 40100 清理登录态并跳转登录页。
- 网络错误、业务错误和取消请求分别处理。
- 支持 GET/POST/上传所需参数。
- 登录 redirect 使用站内页面白名单。

验收：页面不直接调用 `uni.request`；未登录、Token 过期和网络失败行为一致。

#### 任务 2.4：路由、tabBar 和登录守卫

- 按页面映射配置 `pages.json`。
- 配置 5 个 tab 和子包。
- 登录、注册不进入 tabBar。
- 保存用户状态到 Pinia，必要时持久化 Token。
- 将原路由 meta 的标题、tab 显示规则迁移到页面配置。

验收：未登录访问受保护页面会跳登录；登录后返回原目标；tab 与非 tab 页面跳转方式正确。

#### 任务 2.5：UI 与资源基线

- 建立颜色、字号、间距、圆角等主题变量。
- 使用 rpx/跨端安全区方案，不照搬 `100dvh`。
- Lucide 图标替换为 iconfont 或压缩图片。
- 不迁移 7.5 MB 本地字体；标题字体使用系统字体或经过子集化的网络字体。
- 图片压缩，非必要资源不进入主包。

验收：主包体积留有余量；常用页面在 iOS/Android 尺寸下无安全区遮挡。

### 阶段 3：核心业务页面迁移

按依赖顺序逐页迁移，每页完成“接口 + 状态 + 空态 + 错误态 + 加载态”闭环：

1. 登录、注册。
2. 首页。
3. 匹配页。
4. 队伍列表、搜索及搜索结果。
5. 我的页面。
6. 创建、更新、加入、退出队伍。
7. 我创建的队伍、我加入的队伍。
8. 资料编辑、标签选择、头像上传。

迁移要求：

- 业务数据访问统一经过 `api/`。
- Vant 组件逐一替换，不同时维护两套组件体系。
- `window.location` 和 localStorage 全部替换。
- 保留原有参数校验和登录态处理。
- 每次仅迁移直接相关样式，不顺手重构 H5。

验收：核心流程在小程序真机可操作；接口失败有提示，不出现静默空白页。

### 阶段 4：聊天与 AI 页面

#### 任务 4.1：私聊和队伍聊天

- 使用统一 Socket 管理模块，不在两个页面重复实现连接细节。
- 使用 `scroll-view` 和 `scroll-into-view` 实现消息滚动。
- 处理键盘弹出、输入框遮挡、安全区和消息分页。
- 历史消息与实时消息按 ID 去重。
- 显示连接中、已加入、重连中和失败状态。

验收：连续收发、历史加载、断网恢复、切后台恢复均正常。

#### 任务 4.2：AI 流式对话

- 将流式读取封装为统一适配器，页面只消费文本片段。
- 保留现有打字机效果，但限制高频渲染次数。
- 支持停止生成、失败提示和重试。
- 替换 marked + DOMPurify + `v-html`。
- Markdown 至少支持段落、列表、标题、行内代码和代码块；代码块复制可按需增加。

验收：长回复不卡顿、不乱码，中文和 emoji 不截断；代码块可读；页面退出后请求不会继续更新状态。

### 阶段 5：分端发布与验收

#### 微信小程序

- 配置 request、socket、uploadFile、downloadFile 合法域名。
- 检查主包和分包体积，移除未使用组件和资源。
- 接入必要的内容安全检测和违规处置流程。
- 检查微信登录/手机号能力与现有账号体系的绑定要求。
- 完成体验版真机回归后提审。

#### App

- 配置应用图标、权限、模块、隐私政策和版本号。
- 验证相机、相册、通知、后台切换和网络恢复。
- 需要离线触达时再接 UniPush/厂商通道，不提前增加复杂度。
- 使用正式基座打包，不使用自定义调试基座发布。
- 分别验证 Android 和 iOS 后再上架。

#### 回归清单

- 登录、注销、Token 过期。
- 匹配、搜索、队伍完整流程。
- 私聊和队伍聊天。
- AI 长文本流式回复。
- 头像选择、上传和显示。
- 弱网、断网、切后台、锁屏。
- 键盘、滚动、安全区和不同屏幕尺寸。
- 现有 H5 核心流程无回归。

## 6. 推荐提交拆分

1. `chore: scaffold codemate uni-app project`
2. `feat: add token authentication compatibility`
3. `feat: add uni-app request and auth guard`
4. `feat: configure uni-app pages and tabbar`
5. `feat: migrate match and team pages`
6. `feat: migrate profile and avatar upload`
7. `feat: add resilient chat socket client`
8. `feat: migrate private and team chat`
9. `feat: add cross-platform AI streaming`
10. `chore: configure miniapp release and app packaging`

每个提交保持可审查，不把整个工程迁移压成一次大提交。

## 7. 工期估算（单人全栈，不含资质和备案等待）

| 阶段 | 估算 |
| --- | --- |
| 技术验证与后端兼容 | 1–2 周 |
| 工程骨架、请求层、登录和路由 | 1 周 |
| 核心业务页面 | 3–4 周 |
| 私聊与队伍聊天 | 1–2 周 |
| AI 流式与 Markdown | 1–2 周 |
| 真机联调、分包和发布 | 1–2 周 |
| 合计 | 约 7–11 周 |

## 8. 风险与止损点

| 风险 | 影响 | 应对 |
| --- | --- | --- |
| 小程序主体/社交类目不满足 | 无法上架 | 阶段 0 先确认，未通过则不投入完整迁移 |
| AI chunked 真机表现不一致 | 核心体验受损 | 最早验证；必要时改用 WebSocket 推送或原生插件 |
| Cookie 鉴权硬搬 | 登录和聊天随机失效 | 使用 Token 双轨，不依赖小程序 Cookie 行为 |
| WebSocket 无心跳 | 反代或切后台后断连 | 上线前完成心跳、重连和重新 JOIN |
| 字体和图片进入主包 | 超包体积 | 删除大字体、压缩图片、CDN 和分包 |
| 强求单套代码 | 条件分支失控 | 共享业务逻辑，允许网络和 UI 分端实现 |
| 同时重写 H5 | 回归面过大 | 保留 H5，新功能按优先级逐步对齐 |

## 9. 第一步执行范围

计划批准后，第一轮只完成以下内容：

1. 创建 `codemate-uniapp` 基础工程。
2. 配置 Vue 3、TypeScript、Pinia 和 wot-design-uni。
3. 建立环境变量、请求层和 Token 存储。
4. 配置 5 个 tab 空页面及登录/注册分包页面。
5. 编写后端 Token 双轨鉴权的最小兼容改动。
6. 提供微信开发者工具运行说明和后续验证清单。

第一轮不迁移聊天、AI 和复杂页面样式，不删除或改写现有 H5 工程。
