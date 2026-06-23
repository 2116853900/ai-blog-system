# AI 信息站 — 功能缺口分析与补充路线图

**日期：** 2026-06-23  
**策略：** 先制定计划、分阶段验收，再按优先级实施（与 `planning-and-task-breakdown` / `using-agent-skills` 一致）。

## 概述

项目已具备较完整的内容目录、论坛、收藏/关注/通知、资源评分、API 健康大盘、后台审核与审计日志等能力。本次盘点聚焦 **用户可感知的产品缺口**、**已有 API 未接好的前台**、**发现与信任信号**、**运营与工程化**，避免重复建设已有模块。

## 现状摘要（已具备）

| 域 | 能力 |
|----|------|
| 内容 | 教程 / Skill / MCP / API 站列表与详情、标签筛选、延伸阅读 |
| 发现 | 全站搜索、热门标签、`/stats` 平台洞察 |
| 社区 | 论坛发帖回帖、采纳、点赞收藏、帖子关注与未读、通知中心（账户页内） |
| 互动 | 评论（待审）、投稿（Skill/MCP/API）、举报、资源收藏 |
| 信任 | 详情页 `ResourceReviewPanel`、后端评分汇总 API |
| 运维 | API 定时检测、健康大盘/趋势、后台总览、操作审计日志 |
| 工程 | 后端 `mvn test`、前端 `typecheck`+`build`、Vite 代理 |

## 缺口分类

### P0 — 体验闭环（后端已有或改动小）

1. **顶栏通知触达**  
   - 缺口：登录后未读通知仅在 `/account` 内可见，导航无角标/入口。  
   - 建议：NavBar 拉取 `unreadNotificationCount`，显示角标并链到账户「通知」Tab。  
   - 验收：登录用户有未读时顶栏可见数字；点击跳转账户并定位通知。

2. **首页信息密度**  
   - 缺口：首页仅「最新教程」4 条，未用 `/api/stats` 的 `recentItems`、`hotThreads`、API 健康摘要。  
   - 建议：Home 调用 `publicApi.stats()`，增加「近期更新」「热门讨论」「API 状态一览」区块（链到现有路由）。  
   - 验收：无登录可看到三块数据或合理空态；链接可点通。

3. **列表页信任信号**  
   - 缺口：Skill/MCP/API/教程列表卡片未展示社区评分（`resourceReviewSummary` 或列表 DTO 内嵌）。  
   - 建议：优先在列表接口增加轻量 `averageRating`/`reviewCount`（避免 N+1）；或首屏仅详情已有评分。  
   - 验收：列表卡片显示星级或「暂无评价」。

4. **账户「我的评价」**  
   - 缺口：用户只能在各详情页改自己的评价，账户中心无汇总。  
   - 建议：后端 `GET /api/account/resource-reviews` 分页；账户页新 Tab「我的评价」。  
   - 验收：登录用户可看到历史评价并跳转对应资源。

### P1 — 发现与投稿

5. **搜索增强（轻量）**  
   - 缺口：无搜索建议、无按类型筛选结果页。  
   - 建议：Search 页增加类型 Tab 过滤（基于现有 `groups`）；热门标签链到搜索（可复用 stats/tags）。  
   - 验收：同一关键词可只看 SKILL/MCP 等分组。

6. **教程投稿类型**  
   - 缺口：`Submit.vue` 仅 SKILL/MCP/API，无教程投稿。  
   - 建议：若 `Submission` 实体支持 POST 类型则扩展表单；否则文档说明「教程仅后台发布」。  
   - 验收：与后端 `SubmissionType` 一致且无 400。

7. **静态页与页脚**  
   - 缺口：无关于站、使用说明、隐私/免责声明独立页；页脚信息少。  
   - 建议：`/about` 单页 + 页脚链接（可不接后端）。  
   - 验收：路由可访问、移动端可读。

### P2 — SEO、订阅与工程

8. **基础 SEO**  
   - 缺口：仅全局 `description`，详情页无动态 title/OG；无 `sitemap.xml` / `robots.txt`。  
   - 建议：详情页 `useHead` 或 `document.title`；后端或构建时生成 sitemap（公开 slug/id 列表）。  
   - 验收：教程详情 title 含文章名；`/robots.txt` 可访问。

9. **RSS（可选）**  
   - 缺口：无 RSS/Atom 供订阅最新教程或全站更新。  
   - 建议：`GET /api/feed.xml` 或静态生成最近 N 篇教程。  
   - 验收：Feed 校验通过、含链接与日期。

10. **CI 流水线**  
    - 缺口：`.github` 仅有 modernize hooks，无 PR 自动 `mvn test` + `npm run build`。  
    - 建议：`ci-cd-and-automation` 技能 — workflow on push/PR。  
    - 验收：PR 上绿勾。

11. **E2E 冒烟（可选）**  
    - 缺口：无 Playwright；README 端到端靠人工。  
    - 建议：`webapp-testing` — 首页、搜索、登录、后台登录 3～5 条用例。  
    - 验收：本地 `npx playwright test` 通过。

### 明确不做 / 延后（除非产品明确要求）

- 多管理员 RBAC、OAuth 第三方登录、邮件验证  
- 标签中心化治理（别名、合并）— 工作量大，单独立项  
- JWT Refresh / 登出黑名单 — 安全增强，非功能缺失  
- 微服务拆分 — 与当前单体目标不符  

## 架构决策

- **继续模块化单体**：新接口沿用 `controller` → `service` → `repository`，账户类接口挂 `/api/account/**`。  
- **垂直切片实施**：每项功能尽量「API + 前台同一 PR」，每阶段跑 `mvn test` 与 `npm run build`。  
- **不破坏进行中能力**：资源评分、审计日志、健康趋势等已落地代码仅扩展，不重写。

## 任务列表（实施顺序）

### Phase 1：P0 体验闭环（推荐先做）

| # | 任务 | 范围 | 验收 |
|---|------|------|------|
| 1.1 | 顶栏未读通知角标 | `NavBar.vue`, `auth` 或轻量 composable | 有未读显示数字 |
| 1.2 | 首页 stats 区块 | `Home.vue`, `publicApi.stats` | 三块内容或空态 |
| 1.3 | 账户「我的评价」 | 新 Controller 方法 + `Account.vue` + `api/index.ts` | 分页列表可跳转 |
| 1.4 | 列表评分展示 | 后端列表 DTO 或批量 summary + 列表 Vue | 卡片显示评分 |

**Checkpoint Phase 1**  
- [ ] `cd backend && mvn -q test`  
- [ ] `cd frontend && npm run build`  
- [ ] 手动：登录 → 通知角标 → 首页 → 列表评分 → 我的评价  

### Phase 2：P1 发现与内容

| # | 任务 | 范围 | 验收 |
|---|------|------|------|
| 2.1 | 搜索按类型筛选 | `Search.vue` | Tab 过滤 groups |
| 2.2 | 关于页 + 页脚 | 新路由 `About.vue`, `App.vue` footer | 链接可用 |
| 2.3 | 投稿类型对齐 | `Submit.vue` + `Submission` 枚举 | 与后端一致 |

**Checkpoint Phase 2**  
- [ ] 构建通过 + 搜索/投稿/关于页手测  

### Phase 3：P2 运营与工程

| # | 任务 | 范围 | 验收 |
|---|------|------|------|
| 3.1 | 详情动态 title | 各 Detail 视图 | 浏览器标题正确 |
| 3.2 | robots + sitemap | 后端或 `public/` | 爬虫可抓 |
| 3.3 | GitHub Actions CI | `.github/workflows/ci.yml` | PR 测试构建 |
| 3.4 | RSS（可选） | 新 FeedController | XML 有效 |

## 风险与缓解

| 风险 | 影响 | 缓解 |
|------|------|------|
| 列表加评分导致查询变慢 | 中 | 列表 DTO 单次聚合或缓存 summary |
| 首页 stats 接口慢 | 低 | 已有 `PublicStatsService`，前端 skeleton |
| 与历史 plan 重复 | 低 | 本计划引用 `2026-06-02-platform-architecture-gap-closure` 已做项，只做增量 |

## 待你确认（实施前）

1. **优先做哪一 Phase？** 默认建议 **Phase 1（P0）** 全部实施。  
2. **列表评分**：接受「列表接口扩展字段」还是「仅首页/洞察页展示」？  
3. **教程投稿**：是否需要用户投稿教程，还是维持仅管理员发教程？  

---

**下一步（agent）：** 在你确认 Phase 与上述问题后，按 `incremental-implementation` 逐项提交代码并跑 Checkpoint 验证。