# CodeMate 前端

CodeMate 的 Vue 3 客户端，面向移动端提供伙伴发现、队伍管理、即时聊天和 AI 编程助手。项目总览与后端启动说明见[仓库根 README](../README.md)。

## 主要页面

| 页面 | 路由 | 用途 |
| --- | --- | --- |
| 首页与匹配 | `/`、`/match` | 浏览推荐用户和匹配结果 |
| 搜索 | `/search`、`/user/list` | 按技术标签查找用户 |
| 队伍 | `/team`、`/team/add`、`/team/update` | 浏览、创建和管理队伍 |
| 聊天 | `/team/chat`、`/chat/private` | 队伍聊天和私聊 |
| AI 助手 | `/ai/chat` | 流式问答与 Markdown 回复展示 |
| 个人中心 | `/user`、`/user/update` | 查看与编辑个人资料 |
| 账号 | `/user/login`、`/user/register` | 登录与注册 |

路由定义以 [`src/config/route.ts`](src/config/route.ts) 为准。

## 技术与目录

- Vue 3、TypeScript、Vue Router 和 Vite 构成页面与路由基础。
- Vant、Tailwind CSS 与 [`src/styles/labverse.css`](src/styles/labverse.css) 提供组件和视觉样式。
- Axios 处理常规 HTTP 请求；聊天页面通过 WebSocket 收发消息；AI 页面使用 SSE 接收流式回复，并使用 `marked` 与 `DOMPurify` 处理 Markdown 展示。

```text
src/
├─ pages/        # 页面视图
├─ components/   # 可复用组件
├─ layouts/      # 页面布局
├─ config/       # 路由配置
├─ services/     # API 请求封装
├─ plugins/      # Axios 实例
├─ styles/       # 全局样式
└─ assets/       # 图片与字体
```

## 本地开发

准备 Node.js 和 npm，并先按[根 README](../README.md#本地启动)启动后端。在 `codemate-frontend` 目录运行：

```powershell
npm install
npm run dev
```

在终端输出的 Vite 地址打开页面；不要假定开发服务器始终使用同一个端口。其他可用脚本见 [`package.json`](package.json)：`npm run build` 生成生产产物，`npm run preview` 预览构建结果。

开发环境的 HTTP API 默认指向 `http://localhost:8080/api`，配置位置是 [`src/plugins/myAxios.ts`](src/plugins/myAxios.ts)。队伍聊天和私聊页面连接 `ws://localhost:8091/ws/chat`。部署时需将 HTTP API 的生产占位地址及两个聊天页面的 WebSocket 地址调整为实际服务地址；跨域会话还需确保 Cookie 配置与部署域名匹配。

## 相关文档

- [项目总览](../README.md)
- [后端说明](../codemate-backend/README.md)
