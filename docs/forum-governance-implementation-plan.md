# 后台论坛治理与互动能力实施计划

**计划日期**：2026-05-31  
**实施启动**：2026-06-01  
**目标周期**：3 周 MVP + 1 周联调验收  
**默认团队**：后端 1 人、前端 1 人、测试/产品 1 人  

## 1. 项目目标

建设一套后台内容治理与用户治理能力，覆盖：

- 后台论坛帖子管理
- 后台论坛回复管理
- 用户管理与封禁
- 帖子点赞与收藏
- 评论、帖子、回复等内容的统一举报审核
- 管理员操作留痕，支持追责和回溯

一期重点先打通主链路：**内容可管、用户可封、互动可计数、举报可审核、操作可追溯**。

## 2. 一期范围

### 2.1 后台帖子管理

后台能力：

- 帖子列表
- 按标题、作者、状态、时间、举报状态筛选
- 查看帖子详情
- 隐藏 / 恢复帖子
- 删除帖子，优先采用软删除
- 批量隐藏 / 删除
- 查看帖子点赞数、收藏数、回复数、举报数
- 查看帖子操作记录

帖子状态建议：

```text
NORMAL   正常
HIDDEN   已隐藏
DELETED  已删除
PENDING  待审核，可选
```

### 2.2 后台回复管理

后台能力：

- 回复列表
- 按帖子、作者、状态、时间、举报状态筛选
- 查看回复内容和上下文
- 隐藏 / 恢复回复
- 删除回复，优先采用软删除
- 批量处理
- 查看操作记录

回复状态建议：

```text
NORMAL   正常
HIDDEN   已隐藏
DELETED  已删除
```

### 2.3 用户管理 / 封禁

后台能力：

- 用户列表
- 按用户名、手机号/邮箱、状态、注册时间筛选
- 查看用户详情
- 查看用户发帖、回复、评论、举报、被举报记录
- 封禁用户
- 解封用户
- 支持永久封禁和限时封禁
- 填写封禁原因
- 管理员操作日志

用户状态建议：

```text
NORMAL   正常
BANNED   已封禁
MUTED    禁言，可选
DELETED  注销/删除，可选
```

封禁字段建议：

```text
ban_status
ban_reason
ban_start_time
ban_end_time
ban_operator_id
```

MVP 可以先实现 `NORMAL / BANNED` 两种状态。

### 2.4 帖子点赞 / 收藏

用户侧能力：

- 点赞帖子
- 取消点赞
- 收藏帖子
- 取消收藏
- 查询当前用户是否已点赞 / 已收藏
- 帖子列表和详情返回点赞数、收藏数

后台展示：

- 帖子管理列表展示点赞数、收藏数
- 帖子详情展示互动数据

实现要求：

- 点赞和收藏接口必须幂等
- 同一用户对同一帖子只能点赞一次
- 同一用户对同一帖子只能收藏一次
- 取消操作不能导致计数小于 0

推荐表：

```text
forum_post_like
- id
- post_id
- user_id
- created_at
- unique(post_id, user_id)

forum_post_favorite
- id
- post_id
- user_id
- created_at
- unique(post_id, user_id)
```

帖子表建议冗余计数：

```text
like_count
favorite_count
reply_count
report_count
```

### 2.5 统一举报审核

统一举报对象：

```text
POST     论坛帖子
REPLY    论坛回复
COMMENT  评论
```

举报状态：

```text
PENDING   待审核
APPROVED  举报成立
REJECTED  举报不成立
CLOSED    已关闭/无需处理
```

举报原因：

```text
SPAM       垃圾广告
ABUSE      辱骂攻击
PORN       色情低俗
POLITICS   敏感内容，可按业务决定
ILLEGAL    违法违规
COPYRIGHT  侵权
OTHER      其他
```

举报表建议：

```text
content_report
- id
- target_type
- target_id
- reporter_id
- reason_type
- reason_text
- content_snapshot
- status
- reviewer_id
- review_result
- review_note
- reviewed_at
- created_at
- updated_at
```

`content_snapshot` 需要保存举报时的内容快照，避免后续内容被修改后无法追溯。

后台审核能力：

- 举报列表
- 按对象类型、举报原因、状态、时间筛选
- 查看举报详情
- 查看被举报内容快照
- 查看当前原始内容
- 查看举报人和被举报人
- 审核通过
- 审核驳回
- 关闭举报
- 审核通过后联动处理内容：隐藏帖子 / 隐藏回复 / 隐藏评论
- 可选联动处理用户：封禁 / 禁言

## 3. 明日启动清单

明天优先做第 1 天工作，不直接铺开所有页面，先确认数据模型和主流程边界。

### 上午

1. 拉取最新代码，确认后端、前端、本地数据库可正常启动。
2. 梳理现有表：用户、帖子、回复、评论、点赞、收藏、举报、权限相关表。
3. 确认是否已有软删除字段、状态字段、管理员权限字段。
4. 确认帖子、回复、评论在用户侧查询时是否已经过滤隐藏/删除内容。

### 下午

1. 出数据库变更草案，至少覆盖：内容状态、点赞收藏表、统一举报表、封禁字段、操作日志表。
2. 确认后台接口路径和权限点命名。
3. 优先实现或改造后端基础模型与 migration。
4. 开始实现帖子管理列表接口和详情接口。

### 第一天完成标准

- 数据库改造方案确认。
- 核心枚举确认：内容状态、举报状态、举报对象类型、用户封禁状态。
- 后台接口路径确认。
- 帖子管理列表接口具备初版返回数据。
- 明确第二天要接的前端页面字段。

## 4. 里程碑

| 阶段 | 时间 | 目标 | 验收标准 |
|---|---:|---|---|
| M1 | 第 1-2 天 | 方案和数据结构确认 | 表结构、状态流转、权限点确认 |
| M2 | 第 3-6 天 | 后端基础能力完成 | 帖子/回复/用户管理 API 可用 |
| M3 | 第 7-9 天 | 点赞收藏完成 | 用户可点赞、收藏，计数正确 |
| M4 | 第 10-14 天 | 统一举报审核完成 | 举报、审核、联动处理闭环跑通 |
| M5 | 第 15-18 天 | 后台页面完成 | 管理端可完成主要操作 |
| M6 | 第 19-20 天 | 测试和修复 | 无 P0/P1 问题，核心流程通过 |

## 5. 后端任务拆解

| 任务 | 预估 | 依赖 | 完成标准 |
|---|---:|---|---|
| 梳理现有帖子、回复、评论、用户表 | 4h | 无 | 明确字段缺口和改表方案 |
| 增加内容状态字段 | 4h | 表结构确认 | 帖子、回复、评论支持隐藏/删除状态 |
| 增加后台操作日志表 | 4h | 权限体系 | 管理员操作可记录 |
| 帖子管理 API | 8h | 状态字段 | 列表、详情、隐藏、恢复、删除可用 |
| 回复管理 API | 8h | 状态字段 | 列表、详情、隐藏、恢复、删除可用 |
| 用户管理 API | 8h | 用户表 | 列表、详情、封禁、解封可用 |
| 封禁校验接入业务接口 | 6h | 封禁字段 | 被封禁用户不能发帖、回复、评论、点赞、收藏 |
| 点赞 API | 6h | 帖子表 | 点赞/取消点赞幂等，计数正确 |
| 收藏 API | 6h | 帖子表 | 收藏/取消收藏幂等，计数正确 |
| 举报提交 API | 6h | 举报表 | 用户可举报帖子、回复、评论 |
| 举报审核 API | 8h | 举报表 | 审核通过/驳回/关闭可用 |
| 举报联动内容处理 | 6h | 内容管理 API | 审核通过后可隐藏对应内容 |
| 举报联动用户封禁 | 4h | 用户封禁 API | 审核时可选择封禁用户 |
| 权限点接入 | 4h | 后台权限 | 非管理员不能访问治理接口 |
| 单元/接口测试 | 8h | 全部后端接口 | 核心接口测试通过 |

## 6. 前端后台任务拆解

| 页面/模块 | 预估 | 功能 |
|---|---:|---|
| 帖子管理列表 | 6h | 搜索、筛选、分页、状态展示、批量操作 |
| 帖子详情抽屉/页面 | 4h | 内容、作者、互动数、举报数、操作日志 |
| 回复管理列表 | 6h | 搜索、筛选、分页、隐藏、删除 |
| 用户管理列表 | 6h | 搜索、筛选、状态展示、封禁入口 |
| 用户详情页 | 6h | 基础信息、内容记录、封禁记录 |
| 封禁弹窗 | 4h | 永久/限时封禁、原因填写 |
| 举报审核列表 | 8h | 类型、原因、状态、时间筛选 |
| 举报详情页/抽屉 | 8h | 内容快照、原始内容、举报人、被举报人、审核操作 |
| 审核操作弹窗 | 6h | 通过、驳回、关闭、联动隐藏、联动封禁 |
| 前端权限与菜单接入 | 4h | 后台菜单和按钮权限控制 |
| 联调修复 | 8h | 接口字段、状态展示、异常提示修复 |

## 7. API 建议

### 7.1 帖子管理

```http
GET    /admin/forum/posts
GET    /admin/forum/posts/{id}
POST   /admin/forum/posts/{id}/hide
POST   /admin/forum/posts/{id}/restore
DELETE /admin/forum/posts/{id}
POST   /admin/forum/posts/batch-hide
POST   /admin/forum/posts/batch-delete
```

### 7.2 回复管理

```http
GET    /admin/forum/replies
GET    /admin/forum/replies/{id}
POST   /admin/forum/replies/{id}/hide
POST   /admin/forum/replies/{id}/restore
DELETE /admin/forum/replies/{id}
```

### 7.3 用户管理

```http
GET  /admin/users
GET  /admin/users/{id}
POST /admin/users/{id}/ban
POST /admin/users/{id}/unban
GET  /admin/users/{id}/contents
GET  /admin/users/{id}/reports
```

### 7.4 点赞收藏

```http
POST   /forum/posts/{id}/like
DELETE /forum/posts/{id}/like
POST   /forum/posts/{id}/favorite
DELETE /forum/posts/{id}/favorite
GET    /forum/posts/{id}/interaction
```

### 7.5 举报

```http
POST /reports
GET  /admin/reports
GET  /admin/reports/{id}
POST /admin/reports/{id}/approve
POST /admin/reports/{id}/reject
POST /admin/reports/{id}/close
```

## 8. 权限点建议

```text
forum:post:view
forum:post:manage
forum:reply:view
forum:reply:manage
user:view
user:ban
report:view
report:review
```

如果现有系统权限粒度较粗，可以先使用：

```text
ADMIN
SUPER_ADMIN
```

后续再改为权限点。

## 9. 举报审核流转

```text
用户举报
  -> 生成举报记录 PENDING
  -> 管理员查看举报详情
  -> 选择审核结果

举报成立 APPROVED
  -> 可联动隐藏内容
  -> 可联动封禁/禁言用户
  -> 写入审核记录和管理员操作日志

举报不成立 REJECTED
  -> 保留举报记录
  -> 不处理内容

无需处理 CLOSED
  -> 用于重复举报、无效举报、历史内容已处理等场景
```

## 10. 验收标准

### 10.1 内容管理

- 管理员可以查看帖子和回复列表。
- 管理员可以按状态、作者、时间筛选。
- 管理员可以隐藏、恢复、删除帖子和回复。
- 被隐藏内容在用户侧不可见，后台仍可见。
- 删除采用软删除，可追溯。

### 10.2 用户封禁

- 管理员可以封禁和解封用户。
- 被封禁用户不能发帖、回复、评论、点赞、收藏。
- 封禁原因、时间、操作人可追溯。
- 限时封禁到期后可恢复，或登录/操作时自动判断。

### 10.3 点赞收藏

- 用户可以点赞/取消点赞帖子。
- 用户可以收藏/取消收藏帖子。
- 重复点击不会产生重复数据。
- 计数准确，不出现负数。
- 帖子列表和详情展示当前用户互动状态。

### 10.4 举报审核

- 用户可以举报帖子、回复、评论。
- 后台可以统一查看所有举报。
- 后台可以审核通过、驳回、关闭举报。
- 审核通过时可以联动隐藏内容。
- 审核通过时可以联动封禁用户。
- 举报记录保留内容快照和审核记录。

## 11. 风险与处理

| 风险 | 影响 | 处理 |
|---|---|---|
| 现有帖子、评论、回复模型不统一 | 举报审核复杂 | 用 `target_type + target_id` 做统一举报表 |
| 删除后无法追溯 | 审核和申诉困难 | 默认软删除，保留内容快照 |
| 点赞收藏计数不准 | 数据展示错误 | 唯一索引 + 事务更新计数 |
| 用户封禁遗漏业务入口 | 被封禁用户仍可操作 | 在发帖、回复、评论、点赞、收藏接口统一校验 |
| 审核联动过重 | 操作容易误伤 | 审核结果和内容处理动作分开记录 |
| 管理员误操作 | 内容治理风险 | 加操作确认、操作日志，危险操作只给高级权限 |

## 12. 推荐实施顺序

1. 先做数据库字段和权限点。
2. 做帖子/回复/用户后台 API。
3. 做点赞收藏，保证用户侧互动数据完整。
4. 做统一举报表和举报提交接口。
5. 做举报后台审核接口。
6. 做后台页面。
7. 做封禁、隐藏、举报之间的联动。
8. 做测试、操作日志和边界修复。

## 13. 暂不纳入 MVP 的能力

- 用户申诉流程。
- 自动风控和敏感词命中。
- 举报自动合并规则。
- 举报人信用分。
- 内容审核队列分派。
- 多级审核。

这些能力可以在一期主链路稳定后作为二期治理能力扩展。

## 14. 2026-06-01 执行记录

本次已完成第 1 天后端基础工作：

- 切换到 `forum-governance-continue` 工作分支，保留昨日未提交改动。
- 补充治理基础数据模型：
  - `forum_post_like`
  - `forum_post_favorite`
  - `content_report`
  - `admin_operation_log`
- 补充帖子举报计数字段 `reportCount`。
- 补充论坛用户封禁追踪字段：
  - `banReason`
  - `banStartTime`
  - `banEndTime`
  - `banOperatorUsername`
- 评论增加内容状态字段，公开侧只返回已审核且正常状态的评论。
- 公开论坛帖子列表、详情、关联帖子查询改为只返回可见状态，隐藏/删除帖子不再对用户侧可见。
- 新增后台帖子治理接口：
  - `GET /api/admin/forum/posts`
  - `GET /api/admin/forum/posts/{id}`
  - `GET /api/admin/forum/posts/{id}/operation-logs`
  - `POST /api/admin/forum/posts/{id}/hide`
  - `POST /api/admin/forum/posts/{id}/restore`
  - `DELETE /api/admin/forum/posts/{id}`
  - `POST /api/admin/forum/posts/batch-hide`
  - `POST /api/admin/forum/posts/batch-delete`
- 后台帖子列表初版支持标题/内容关键词、作者、作者 ID、状态、是否被举报、创建时间过滤。
- 隐藏、恢复、删除帖子会维护板块帖子计数，并写入管理员操作日志。
- 新增后台帖子管理页面 `/admin/forum-posts`，接入筛选、分页、详情、隐藏、恢复、软删除和批量处理。
- 后台菜单已加入“论坛帖子”入口。
- 新增后台回复治理接口：
  - `GET /api/admin/forum/replies`
  - `GET /api/admin/forum/replies/{id}`
  - `GET /api/admin/forum/replies/{id}/operation-logs`
  - `POST /api/admin/forum/replies/{id}/hide`
  - `POST /api/admin/forum/replies/{id}/restore`
  - `DELETE /api/admin/forum/replies/{id}`
  - `POST /api/admin/forum/replies/batch-hide`
  - `POST /api/admin/forum/replies/batch-delete`
- 新增后台回复管理页面 `/admin/forum-replies`，接入帖子 ID、作者、状态、举报状态筛选、详情、隐藏、恢复、软删除和批量处理。
- 新增后台用户治理接口：
  - `GET /api/admin/users`
  - `GET /api/admin/users/{id}`
  - `GET /api/admin/users/{id}/operation-logs`
  - `POST /api/admin/users/{id}/ban`
  - `POST /api/admin/users/{id}/unban`
- 新增后台用户管理页面 `/admin/users`，接入用户筛选、详情、封禁和解封。
- 封禁校验已接入发帖、编辑帖子、删除帖子、发表回复、编辑回复、删除回复入口。
- 限时封禁支持在登录或操作时自动到期解封。

验证结果：

- 后端：`mvn -q -DskipTests compile` 通过。
- 前端：`npm run build` 通过。

下一步建议：

1. 补点赞/收藏接口与计数事务。
2. 补统一举报提交接口和后台审核接口。
3. 将举报审核通过后的联动隐藏内容、联动封禁用户接入现有治理服务。

## 15. 2026-06-01 互动与举报执行记录

本次在 `forum-interactions-reports` 分支继续补齐互动和举报主链路：

- 新增下一阶段计划文档：`docs/superpowers/plans/2026-06-01-forum-interactions-reports.md`。
- 新增帖子点赞 / 取消点赞接口：
  - `GET /api/forum/threads/{threadId}/interaction`
  - `POST /api/forum/threads/{threadId}/like`
  - `DELETE /api/forum/threads/{threadId}/like`
- 新增帖子收藏 / 取消收藏接口：
  - `POST /api/forum/threads/{threadId}/favorite`
  - `DELETE /api/forum/threads/{threadId}/favorite`
- 点赞和收藏操作保持幂等，取消操作不会让计数小于 0。
- 被封禁用户不能点赞、收藏、举报；已登录但被封禁的用户也不能提交评论。
- 帖子详情页新增点赞、收藏和举报入口。
- 新增用户举报提交接口：
  - `POST /api/reports`
- 举报提交会保存举报时内容快照，并按目标类型写入：
  - 帖子标题和正文快照
  - 回复正文快照
  - 评论作者和正文快照
- 举报帖子和回复时会递增对应 `reportCount`。
- 新增后台举报审核接口：
  - `GET /api/admin/reports`
  - `GET /api/admin/reports/{id}`
  - `POST /api/admin/reports/{id}/approve`
  - `POST /api/admin/reports/{id}/reject`
  - `POST /api/admin/reports/{id}/close`
- 后台举报审核支持按对象类型、原因、状态和时间筛选。
- 审核通过时支持联动隐藏帖子、回复或评论。
- 审核通过时支持联动封禁被举报作者。
- 新增后台举报审核页面 `/admin/reports`，接入列表、筛选、详情、快照展示和审核操作。

验证结果：

- 后端：`mvn -q -DskipTests compile` 通过。
- 前端：`npm run build` 通过。

下一步建议：

1. 增加接口级测试，覆盖点赞收藏幂等、举报提交、审核联动隐藏和封禁。
2. 给用户详情补发帖、回复、举报、被举报记录标签页。
3. 视需要补评论后台隐藏/恢复入口，使评论治理和帖子/回复治理完全一致。
