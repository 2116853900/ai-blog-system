# Admin Operation Log Center Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a global admin audit log center so administrators can search and inspect all existing operation logs across comments, reports, forum posts, replies, and users.

**Architecture:** Keep audit read logic in an application service (`AdminOperationLogService`) with a JPA `Specification` query boundary. Expose a single admin-only REST endpoint under `/api/admin/operation-logs`, then consume it from a dedicated Vue admin page using the existing `adminApi` module and admin layout.

**Tech Stack:** Spring Boot 3.3, Spring Data JPA `JpaSpecificationExecutor`, Java 21, Vue 3, Vite, TypeScript, Axios.

---

### Current Architecture Notes

- Backend uses controller/service/repository/entity layering, not strict clean architecture. Keep the new behavior aligned with that structure.
- `AdminOperationLog` and `AdminOperationLogRepository` already exist.
- Existing services already record operation logs for comment moderation, content reports, forum post moderation, forum reply moderation, and forum user governance.
- Existing frontend detail drawers already display per-target operation logs, but there is no global audit page.

### Missing Function Chosen

Add a global admin audit log center:

- Filter by operator username, action, target type, target id, and creation time range.
- Page newest logs first.
- Add a protected backend route: `GET /api/admin/operation-logs`.
- Add a frontend admin route: `/admin/operation-logs`.
- Add a sidebar entry: `审计日志`.

### Task 1: Backend Query Service

**Files:**
- Modify: `backend/src/main/java/com/aiblog/repository/AdminOperationLogRepository.java`
- Create: `backend/src/main/java/com/aiblog/service/AdminOperationLogService.java`
- Test: `backend/src/test/java/com/aiblog/service/AdminOperationLogServiceTest.java`

- [ ] **Step 1: Extend repository with specification support**

```java
package com.aiblog.repository;

import com.aiblog.entity.AdminOperationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface AdminOperationLogRepository extends JpaRepository<AdminOperationLog, Long>,
        JpaSpecificationExecutor<AdminOperationLog> {
    List<AdminOperationLog> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, Long targetId);
}
```

- [ ] **Step 2: Create service**

```java
package com.aiblog.service;

import com.aiblog.entity.AdminOperationLog;
import com.aiblog.repository.AdminOperationLogRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminOperationLogService {

    private final AdminOperationLogRepository operationLogRepo;

    public AdminOperationLogService(AdminOperationLogRepository operationLogRepo) {
        this.operationLogRepo = operationLogRepo;
    }

    public Page<AdminOperationLog> search(String operatorUsername,
                                          String action,
                                          String targetType,
                                          Long targetId,
                                          Instant createdFrom,
                                          Instant createdTo,
                                          Pageable pageable) {
        return operationLogRepo.findAll(buildSpec(
                clean(operatorUsername),
                clean(action),
                clean(targetType),
                targetId,
                createdFrom,
                createdTo), pageable);
    }

    private Specification<AdminOperationLog> buildSpec(String operatorUsername,
                                                       String action,
                                                       String targetType,
                                                       Long targetId,
                                                       Instant createdFrom,
                                                       Instant createdTo) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (operatorUsername != null) {
                predicates.add(cb.like(cb.lower(root.get("operatorUsername")), "%" + operatorUsername.toLowerCase() + "%"));
            }
            if (action != null) {
                predicates.add(cb.like(cb.lower(root.get("action")), "%" + action.toLowerCase() + "%"));
            }
            if (targetType != null) {
                predicates.add(cb.equal(root.get("targetType"), targetType.toUpperCase()));
            }
            if (targetId != null) {
                predicates.add(cb.equal(root.get("targetId"), targetId));
            }
            if (createdFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
            }
            if (createdTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), createdTo));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
```

- [ ] **Step 3: Add service unit test**

```java
package com.aiblog.service;

import com.aiblog.entity.AdminOperationLog;
import com.aiblog.repository.AdminOperationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOperationLogServiceTest {

    @Mock
    private AdminOperationLogRepository operationLogRepo;

    private AdminOperationLogService service;

    @BeforeEach
    void setUp() {
        service = new AdminOperationLogService(operationLogRepo);
    }

    @Test
    void searchDelegatesToRepositoryWithPageable() {
        var pageable = PageRequest.of(1, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        var log = new AdminOperationLog();
        log.setId(10L);
        when(operationLogRepo.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(log), pageable, 1));

        var result = service.search(" admin ", "hide", "comment", 33L,
                Instant.parse("2026-06-01T00:00:00Z"),
                Instant.parse("2026-06-02T00:00:00Z"),
                pageable);

        assertThat(result.getContent()).containsExactly(log);
        verify(operationLogRepo).findAll(any(Specification.class), eq(pageable));
    }
}
```

### Task 2: Backend Admin Controller

**Files:**
- Create: `backend/src/main/java/com/aiblog/controller/admin/AdminOperationLogController.java`

- [ ] **Step 1: Create controller**

```java
package com.aiblog.controller.admin;

import com.aiblog.entity.AdminOperationLog;
import com.aiblog.service.AdminOperationLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/admin/operation-logs")
public class AdminOperationLogController {

    private final AdminOperationLogService operationLogService;

    public AdminOperationLogController(AdminOperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @GetMapping
    public Page<AdminOperationLog> list(@RequestParam(required = false) String operatorUsername,
                                        @RequestParam(required = false) String action,
                                        @RequestParam(required = false) String targetType,
                                        @RequestParam(required = false) Long targetId,
                                        @RequestParam(required = false) Instant createdFrom,
                                        @RequestParam(required = false) Instant createdTo,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return operationLogService.search(
                operatorUsername,
                action,
                targetType,
                targetId,
                createdFrom,
                createdTo,
                PageRequest.of(normalizePage(page), normalizeSize(size), Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    private int normalizePage(int page) {
        return Math.max(0, page);
    }

    private int normalizeSize(int size) {
        return Math.max(1, Math.min(100, size));
    }
}
```

### Task 3: Frontend API and Route

**Files:**
- Modify: `frontend/src/api/index.ts`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/views/admin/AdminLayout.vue`
- Create: `frontend/src/views/admin/AdminOperationLogs.vue`

- [ ] **Step 1: Add API client method**

```ts
operationLogs: (params?: {
  operatorUsername?: string
  action?: string
  targetType?: string
  targetId?: number
  createdFrom?: string
  createdTo?: string
  page?: number
  size?: number
}) => http.get<Page<AdminOperationLog>>('/admin/operation-logs', { params }).then(r => r.data),
```

- [ ] **Step 2: Add admin route**

```ts
{ path: 'operation-logs', name: 'admin-operation-logs', component: () => import('../views/admin/AdminOperationLogs.vue'), meta: { requiresAdmin: true } }
```

- [ ] **Step 3: Add sidebar item**

```ts
{ to: '/admin/operation-logs', label: '🧾 审计日志' }
```

- [ ] **Step 4: Create list page**

The page uses existing admin table conventions:

- filter toolbar with operator, action, target type, target id, created from, created to
- paginated table
- detail column with truncated detail text
- responsive horizontal table wrapper

### Task 4: Verification

**Files:**
- No source files created.

- [ ] **Step 1: Run backend tests**

Run:

```bash
cd backend
mvn -q test
```

Expected: all tests pass.

- [ ] **Step 2: Run frontend production build**

Run:

```bash
cd frontend
npm run build
```

Expected: TypeScript and Vite build pass.

- [ ] **Step 3: Review git diff**

Run:

```bash
git diff -- backend frontend docs/superpowers/plans/2026-06-02-admin-operation-log-center.md
```

Expected: diff contains only the audit log center plan and implementation.
