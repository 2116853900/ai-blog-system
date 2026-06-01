# AI 信息站

一个 AI 信息博客：收录 **AI Skill 推荐**、**MCP 推荐**、**相关教程**、**公益 API 中转站分享**。

- 前端：Vue 3 + Vite + TypeScript（暗色模式、响应式、Markdown 渲染）
- 后端：Spring Boot 3 + Spring Security + JWT
- 数据库：MySQL 8
- 功能：全站搜索与标签筛选、公益 API 站点在线状态定时检测、访客评论/投稿、论坛发帖/回帖、管理员后台审核与内容管理

## 环境要求

- JDK 21+（已在 JDK 25 验证）
- Maven 3.9+
- Node.js 18+ / npm
- MySQL 8（默认连接 `localhost:3306`）

## 后端启动

1. 配置数据库账号（二选一）：
   - 修改 `backend/src/main/resources/application.yml` 中的 `username` / `password`，或
   - 设置环境变量 `DB_USERNAME` / `DB_PASSWORD`（推荐）。
   - 数据库 `ai_blog` 会自动创建（`createDatabaseIfNotExist=true`）。
2. 启动：
   ```bash
   cd backend
   mvn spring-boot:run
   ```
   首次启动会自动建表，并写入默认管理员与示例数据。
3. 默认管理员：`admin` / `admin123`（可用环境变量 `ADMIN_USERNAME` / `ADMIN_PASSWORD` 覆盖，**首次登录后请尽快修改**）。

后端运行在 `http://localhost:8080`。

### 主要接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET  | `/api/posts` `/api/posts/{slug}` | 教程列表 / 详情 |
| GET  | `/api/skills?q=&tag=&category=` | Skill 列表（搜索/筛选）|
| GET  | `/api/mcps?q=&tag=&category=` | MCP 列表 |
| GET  | `/api/api-stations?q=&tag=` | 公益 API 站点列表 |
| GET  | `/api/comments?type=&refId=` | 某内容下已审核评论 |
| POST | `/api/comments` | 提交评论（待审核）|
| POST | `/api/submissions` | 投稿（待审核）|
| GET/POST/PUT/DELETE | `/api/forum/**` | 论坛板块、帖子、回复；帖子列表支持 `q` 关键词搜索 |
| POST | `/api/auth/register` | 论坛用户注册 |
| GET  | `/api/auth/me` | 当前登录用户信息 |
| PUT  | `/api/auth/profile` | 修改论坛用户昵称、头像和简介 |
| PUT  | `/api/auth/password` | 修改论坛用户密码 |
| POST | `/api/auth/login` | 管理员登录，返回 JWT |
| *    | `/api/admin/**` | 后台接口（需 `Authorization: Bearer <token>`）|

公益 API 站点状态由 `StatusCheckService` 每 10 分钟自动检测一次（`app.status-check.cron` 可配），也可在后台手动触发。

## 前端启动

```bash
cd frontend
npm install
npm run dev
```

前端运行在 `http://localhost:5173`，已通过 Vite 代理把 `/api` 转发到后端 `:8080`。

- 公开站点：`/`、`/skills`、`/mcps`、`/tutorials`、`/api-stations`、`/forum`、`/submit`、`/login`、`/account`
- 后台：`/admin/login` 登录后进入 `/admin`

## 端到端验证

1. 启动后端与前端。
2. 浏览首页四大板块、搜索筛选、暗色切换、移动端布局。
3. 打开某教程，提交一条评论。
4. `/submit` 提交一条投稿。
5. 登录后台 `/admin/login`：
   - 新建教程并发布 → 前台 `/tutorials` 可见。
   - 评论审核：通过刚才的评论 → 教程页可见。
   - 投稿审核：通过投稿 → 对应板块出现新条目。
   - API 站点：新增站点 → 点「检测」→ 状态徽章更新。

## 生产构建

- 前端：`npm run build`（产物在 `frontend/dist`）
- 后端：`mvn clean package`（产物 `backend/target/*.jar`，`java -jar` 运行）

> 注意：公益 API 中转站内容仅供学习交流，请遵守各站点使用规则。示例数据中的站点地址为占位，请在后台替换为真实地址。
