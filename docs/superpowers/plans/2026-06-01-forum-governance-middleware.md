# Forum Governance Middleware Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Centralize banned-user blocking for forum/report/comment mutations and add audit logging for report review decisions.

**Architecture:** Add a Spring MVC `HandlerInterceptor` that runs after JWT authentication and before controllers for mutating public governance endpoints. Controllers keep authentication/ownership checks, while ban-state enforcement moves into one middleware component; report review actions write `CONTENT_REPORT` operation logs alongside existing content/user linkage logs.

**Tech Stack:** Spring Boot 3.3, Spring MVC `HandlerInterceptor`, Spring Security `Authentication`, Jackson `ObjectMapper`, JUnit 5, Mockito, Maven Surefire.

---

## File Structure

- Create `backend/src/main/java/com/aiblog/config/WebMvcConfig.java`
  - Registers the forum governance interceptor for all MVC requests.
- Create `backend/src/main/java/com/aiblog/security/ForumMutationGuardInterceptor.java`
  - Blocks authenticated banned users from `POST|PUT|DELETE /api/forum/**`, `POST /api/reports`, and `POST /api/comments`.
  - Leaves anonymous requests and non-mutating requests to existing Spring Security/controller logic.
- Create `backend/src/test/java/com/aiblog/security/ForumMutationGuardInterceptorTest.java`
  - Unit tests for blocked banned users, allowed active users, ignored GET requests, ignored admin paths, and anonymous comment submissions.
- Modify `backend/src/main/java/com/aiblog/controller/ForumThreadController.java`
  - Remove repeated `isActiveForumUser` calls; keep login, ownership, and moderator checks.
- Modify `backend/src/main/java/com/aiblog/controller/ForumReplyController.java`
  - Remove repeated `isActiveForumUser` calls; keep login, ownership, and moderator checks.
- Modify `backend/src/main/java/com/aiblog/controller/ForumInteractionController.java`
  - Remove repeated `isActiveForumUser` calls and local banned response helper; keep login and service calls.
- Modify `backend/src/main/java/com/aiblog/controller/ReportController.java`
  - Remove repeated `isActiveForumUser` call; keep login and bad target handling.
- Modify `backend/src/main/java/com/aiblog/controller/CommentController.java`
  - Remove logged-in banned-user check because middleware now handles it; keep guest comment support.
- Modify `backend/src/main/java/com/aiblog/service/ContentReportService.java`
  - Record admin operation logs for approve/reject/close review decisions with target type `CONTENT_REPORT`.
- Modify `backend/src/test/java/com/aiblog/service/ContentReportServiceTest.java`
  - Assert report review logs are written without breaking existing comment hide log assertions.
- Modify `docs/forum-governance-implementation-plan.md`
  - Append execution notes and verification results.

## Task 1: Add Forum Mutation Guard Middleware

**Files:**
- Create: `backend/src/main/java/com/aiblog/security/ForumMutationGuardInterceptor.java`
- Create: `backend/src/main/java/com/aiblog/config/WebMvcConfig.java`
- Test: `backend/src/test/java/com/aiblog/security/ForumMutationGuardInterceptorTest.java`

- [x] **Step 1: Write interceptor tests**

Create `ForumMutationGuardInterceptorTest` with these tests:

```java
@ExtendWith(MockitoExtension.class)
class ForumMutationGuardInterceptorTest {
    @Mock ForumUserService userService;
    ForumMutationGuardInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new ForumMutationGuardInterceptor(userService, new ObjectMapper());
    }

    @Test
    void blocksBannedAuthenticatedForumMutation() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/forum/threads");
        MockHttpServletResponse response = new MockHttpServletResponse();
        SecurityContextHolder.getContext().setAuthentication(auth("alice"));
        ForumUser user = user(7L, "alice");
        when(userService.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userService.isActiveForumUser(7L)).thenReturn(false);

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("账号已被封禁");
    }
}
```

- [x] **Step 2: Run the focused interceptor test to verify it fails**

Run:

```bash
mvn -q -Dtest=ForumMutationGuardInterceptorTest test
```

Expected: compilation fails because `ForumMutationGuardInterceptor` does not exist.

- [x] **Step 3: Implement `ForumMutationGuardInterceptor`**

Create a component that checks only the intended mutations:

```java
@Component
public class ForumMutationGuardInterceptor implements HandlerInterceptor {
    private static final Set<String> FORUM_MUTATION_METHODS = Set.of("POST", "PUT", "DELETE");
    private final ForumUserService userService;
    private final ObjectMapper objectMapper;

    public ForumMutationGuardInterceptor(ForumUserService userService, ObjectMapper objectMapper) {
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (!requiresActiveForumUser(request)) return true;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) return true;
        return userService.findByUsername(auth.getName())
                .map(ForumUser::getId)
                .map(userService::isActiveForumUser)
                .filter(Boolean::booleanValue)
                .map(active -> true)
                .orElseGet(() -> block(response));
    }
}
```

The `requiresActiveForumUser` helper must return `true` for:

```java
POST|PUT|DELETE /api/forum/**
POST /api/reports
POST /api/comments
```

The `block` helper writes HTTP 403 JSON:

```json
{"message":"账号已被封禁，暂不能进行互动"}
```

- [x] **Step 4: Register the interceptor**

Create `WebMvcConfig`:

```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final ForumMutationGuardInterceptor forumMutationGuardInterceptor;

    public WebMvcConfig(ForumMutationGuardInterceptor forumMutationGuardInterceptor) {
        this.forumMutationGuardInterceptor = forumMutationGuardInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(forumMutationGuardInterceptor);
    }
}
```

- [x] **Step 5: Run focused middleware tests**

Run:

```bash
mvn -q -Dtest=ForumMutationGuardInterceptorTest test
```

Expected: all interceptor tests pass.

## Task 2: Remove Duplicate Controller Ban Checks

**Files:**
- Modify: `backend/src/main/java/com/aiblog/controller/ForumThreadController.java`
- Modify: `backend/src/main/java/com/aiblog/controller/ForumReplyController.java`
- Modify: `backend/src/main/java/com/aiblog/controller/ForumInteractionController.java`
- Modify: `backend/src/main/java/com/aiblog/controller/ReportController.java`
- Modify: `backend/src/main/java/com/aiblog/controller/CommentController.java`

- [x] **Step 1: Remove repeated checks from forum thread mutations**

In `ForumThreadController`, delete the three `userService.isActiveForumUser(userId)` blocks from create/update/delete methods. Keep `resolveUserId`, `hasModerationRole`, and existing 401/403 ownership responses.

- [x] **Step 2: Remove repeated checks from forum reply mutations**

In `ForumReplyController`, delete the three `userService.isActiveForumUser(userId)` blocks from create/update/delete methods. Keep `resolveUserId`, `hasModerationRole`, and existing 401/403 ownership responses.

- [x] **Step 3: Remove repeated checks from forum interaction mutations**

In `ForumInteractionController`, delete `if (!userService.isActiveForumUser(userId)) return banned(...)` from like/unlike/favorite/unfavorite. Delete the unused `banned` helper. Keep `unauthorized`.

- [x] **Step 4: Remove repeated checks from report and comment submission**

In `ReportController`, delete the `isActiveForumUser` block from `submit`. In `CommentController`, remove `ForumUserService`, `Authentication`, `Map`, `resolveUserId`, and the logged-in banned-user block; guests can still submit comments.

- [x] **Step 5: Compile after cleanup**

Run:

```bash
mvn -q -DskipTests compile
```

Expected: compile succeeds and no controller has duplicate ban checks.

## Task 3: Add Audit Logs for Report Review Decisions

**Files:**
- Modify: `backend/src/main/java/com/aiblog/service/ContentReportService.java`
- Modify: `backend/src/test/java/com/aiblog/service/ContentReportServiceTest.java`

- [x] **Step 1: Extend service tests for report review logs**

Update `approveCanHideContentAndBanTargetAuthor` to verify an operation log with:

```java
assertThat(log.getOperatorUsername()).isEqualTo("admin");
assertThat(log.getAction()).isEqualTo("APPROVE_CONTENT_REPORT");
assertThat(log.getTargetType()).isEqualTo("CONTENT_REPORT");
assertThat(log.getTargetId()).isEqualTo(100L);
assertThat(log.getDetail()).isEqualTo("审核成立");
```

Add a reject test:

```java
@Test
void rejectRecordsReportReviewOperation() {
    ContentReport report = new ContentReport();
    report.setId(102L);
    report.setTargetType(ContentReport.TargetType.POST);
    report.setTargetId(11L);
    when(reportRepo.findById(102L)).thenReturn(Optional.of(report));
    ReportReviewRequest request = new ReportReviewRequest();
    request.setReviewNote("证据不足");

    service.reject(102L, request, "reviewer");

    ArgumentCaptor<AdminOperationLog> logCaptor = ArgumentCaptor.forClass(AdminOperationLog.class);
    verify(operationLogRepo).save(logCaptor.capture());
    assertThat(logCaptor.getValue().getAction()).isEqualTo("REJECT_CONTENT_REPORT");
}
```

- [x] **Step 2: Run focused service test to verify failure**

Run:

```bash
mvn -q -Dtest=ContentReportServiceTest test
```

Expected: fails because review logging is not implemented.

- [x] **Step 3: Implement report review operation logging**

In `ContentReportService.review`, after saving the reviewed report, call:

```java
recordOperation(reviewerUsername, actionFor(status), saved.getId(), saved.getReviewNote(), "CONTENT_REPORT");
```

Add helper methods:

```java
private String actionFor(ContentReport.ReportStatus status) {
    return switch (status) {
        case APPROVED -> "APPROVE_CONTENT_REPORT";
        case REJECTED -> "REJECT_CONTENT_REPORT";
        case CLOSED -> "CLOSE_CONTENT_REPORT";
        case PENDING -> "REVIEW_CONTENT_REPORT";
    };
}

private void recordOperation(String operatorUsername, String action, Long targetId, String detail, String targetType) {
    AdminOperationLog log = new AdminOperationLog();
    log.setOperatorUsername(operatorUsername == null ? "unknown" : operatorUsername);
    log.setAction(action);
    log.setTargetType(targetType);
    log.setTargetId(targetId);
    log.setDetail(truncate(detail, 1000));
    operationLogRepo.save(log);
}
```

Keep the existing comment hide log behavior by delegating to the new overloaded helper with target type `COMMENT`.

- [x] **Step 4: Run focused service tests**

Run:

```bash
mvn -q -Dtest=ContentReportServiceTest test
```

Expected: service tests pass.

## Task 4: Verify and Document

**Files:**
- Modify: `docs/forum-governance-implementation-plan.md`

- [x] **Step 1: Run full backend tests**

Run:

```bash
mvn -q test
```

Expected: all tests pass.

- [x] **Step 2: Run backend compile**

Run:

```bash
mvn -q -DskipTests compile
```

Expected: compile succeeds.

- [x] **Step 3: Run frontend build**

Run:

```bash
npm run build
```

Expected: production build succeeds.

- [x] **Step 4: Append execution record**

Append a new section to `docs/forum-governance-implementation-plan.md` describing:

```markdown
## 18. 2026-06-01 治理中间件与审核日志执行记录

- 新增论坛治理中间件，统一拦截已封禁用户的论坛、举报、评论写操作。
- 清理帖子、回复、互动、举报、评论控制器中的重复封禁判断。
- 举报审核通过、驳回、关闭会写入 `CONTENT_REPORT` 管理员操作日志。
- 补充中间件测试和举报审核日志测试。

验证结果：

- 后端测试：`mvn -q test` 通过。
- 后端编译：`mvn -q -DskipTests compile` 通过。
- 前端构建：`npm run build` 通过。
```

## Self-Review

- Spec coverage: This plan implements the requested middleware-based enrichment and keeps functionality focused on governance reliability. It also improves auditability of report review decisions.
- Placeholder scan: No `TBD`, generic "handle edge cases", or unspecified file paths remain.
- Type consistency: Class names, package names, method names, target types, and log actions match the existing Spring Boot project structure.
