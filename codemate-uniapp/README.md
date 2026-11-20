# CodeMate uni-app

CodeMate 的微信小程序 / App / H5 多端前端工程，独立于原 `codemate-frontend`，共用现有后端接口。

## 已实现模块

- 工程：Vue 3 + TypeScript + Vite + Pinia + wot-design-uni
- 版本：DCloud 统一为 `3.0.0-5030420261029002`，Vue `3.4.21`、Vite `5.2.8`、Pinia `2.2.2`，依赖通过 `package-lock.json` 锁定
- 鉴权：uni-app 独立登录、注册、Bearer Token、40100 处理、登录守卫和 redirect
- 首页：推荐伙伴分页、分类筛选、排序、搜索入口和发起私聊
- 匹配：匹配列表、上一位/下一位、重新匹配和发起私聊
- 队伍：公开/加密列表、搜索、创建、更新、加入、退出、解散
- 我的队伍：我创建的、我加入的
- 搜索：分类标签多选、标签搜索、推荐/标签结果和私聊入口
- 个人中心：资料展示、资料编辑、标签编辑、头像选择上传、退出登录
- 聊天：私聊、队伍聊天、历史记录、消息去重、断线重连、页面恢复重连
- AI：现有 GET SSE 接口、H5 流式 fetch、非 H5 chunked 读取、UTF-8 跨包解码、逐字渲染与页面退出取消
- Markdown：标题、列表、普通段落和代码块的跨端展示

## IDE 配置

- IntelliJ IDEA：安装并启用 `Vue.js` 插件，重启 IDE 后重新打开工程；`.vue` 文件应由 Vue 插件解析，不能按普通 TypeScript 文件检查。安装依赖后，将项目 TypeScript 指向 `codemate-uniapp/node_modules/typescript`。
- VS Code：打开 `codemate-uniapp` 目录，安装工作区推荐的 `Vue.volar` 扩展，并使用工作区 TypeScript。

## 安装与运行

```powershell
cd E:\MyFinishProject\CodeMate\codemate-uniapp
npm ci
npm run type-check
npm run dev:mp-weixin
```

微信开发者工具导入：

```text
codemate-uniapp/dist/dev/mp-weixin
```

在 `src/manifest.json` 的 `mp-weixin.appid` 中填写自己的小程序 AppID。

其他命令：

```powershell
npm run type-check
npm run dev:h5
npm run build:mp-weixin
npm run build:h5
npm run build:app
```

App 真机运行和打包使用 HBuilderX 打开本工程。

## 环境地址

修改 `.env.development`：

```text
VITE_API_BASE_URL=http://localhost:8080/api
VITE_WS_BASE_URL=ws://localhost:8091/ws/chat
```

`localhost` 只适合本机 H5 或部分模拟器。手机真机调试时，应改为电脑局域网 IP 或测试域名；生产环境必须使用 HTTPS/WSS，并配置微信 request、socket、uploadFile、downloadFile 合法域名。

## 接口约定

登录：

```http
POST /api/user/uniapp/login
```

登录后的业务请求：

```http
Authorization: Bearer <token>
```

AI 沿用现有接口：

```http
GET /api/ai/chat?message=...
```

WebSocket 沿用：

```text
/ws/chat
JOIN / PRIVATE_JOIN
CHAT / PRIVATE_CHAT
```

## 目录

```text
src/api/               业务 API
src/components/        伙伴、队伍、聊天、Markdown 等共享组件
src/constants/         标签分类
src/pages/             5 个 tab 主页面
src/pages/auth/        登录、注册主包页面
src/subpages/          聊天、搜索、队伍和用户子页面
src/realtime/          WebSocket 客户端和 AI 流式适配
src/request/           请求封装与统一错误处理
src/store/             Pinia 用户状态
src/types/             用户、队伍、聊天类型
src/utils/             存储、格式化、登录守卫和查询参数处理
```

## H5 联调状态

- `npm run type-check`、`npm run build:h5` 与 `npm run build:mp-weixin` 均已通过。
- 已在浏览器验证登录、首页推荐与分页、标签搜索和结果页、队伍列表、个人中心、资料编辑页、我的队伍页、创建队伍页及 AI SSE 流式回复。
- UI 已按原 H5 组件结构及 `mallchat.css` / `labverse.css` 全局主题完成复刻：米白 `#faf9f6` 页面、白色卡片、近黑 `#242321` 主按钮、金色 `#bd9226` 强调、浅色 `#eae7e0` 边框，以及 60px 悬浮胶囊 Tabbar；覆盖共享组件、五个主页面、登录注册、标签搜索、资料编辑、队伍表单和聊天。
- 当前 DCloud 版本要求分包页面使用 `{ path, style }` 对象且不能与主包重复注册，`src/pages.json` 已按此修正。
- 私聊会话创建和私聊历史接口已验证；队伍聊天历史接口对非成员会正确返回无权限。
- 未自动提交资料修改、头像上传、队伍创建/更新/加入/退出/解散，也未向其他用户发送聊天消息。
- WebSocket 握手当前返回 `HTTP Authentication failed; no valid credentials available`，需要后端后续支持 Token 握手。
- `/team/list/my/join` 在当前无加入记录时仍返回公开队伍，且不附带可靠的加入状态和人数，导致“我加入的队伍”无法仅靠前端准确展示，需要后端后续修复。

## 当前边界

1. 未修改后端。普通 HTTP 接口使用已增加的 Token 鉴权；Netty WebSocket 握手若仍只读取 Session Cookie，则小程序/App 聊天需要在后端恢复工作后补充 Token 握手支持。
2. AI 使用现有 GET SSE，未改成计划中的 POST 流式接口。
3. Markdown 是基础跨端展示组件，不包含完整 HTML sanitize、代码高亮和复杂表格渲染。
4. 微信小程序已完成生产构建，仍需在微信开发者工具中配置 AppID、合法域名并进行模拟器和真机验证；App 尚未执行真机打包验证。
5. UI 复刻保留原 H5 的组件结构和视觉规范，但 DOM、Vant、lucide 与 vue-router 已替换为 uni-app 跨端等价实现，并非在小程序中直接运行 Web 组件。
