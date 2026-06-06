# Platform Architecture Gap Closure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the public search access mismatch and add a user-facing resource review/rating capability that improves discovery quality for tutorials, Skills, MCPs, and API stations.

**Architecture:** Keep the current Spring Boot layered modular monolith, but align it around bounded contexts: identity, content catalog, community, moderation, discovery, and platform operations. The first execution batch stays inside the existing package style (`controller`, `service`, `repository`, `entity`, `dto`) to avoid a broad restructuring, while documenting boundaries for future module extraction.

**Tech Stack:** Java 21, Spring Boot 3.3, Spring Security, Spring Data JPA, MySQL 8, Vue 3, Vite, TypeScript, Pinia, Axios.

---

## Current Findings

The project is a Vue/Spring Boot AI information station with these existing capabilities:

- Content catalog: tutorials (`Post`), Skills, MCPs, API stations.
- Discovery: global search, tag/category filters, detail pages, linked forum discussions.
- Community: forum categories, threads, replies, likes, favorites, notifications, public profiles.
- Moderation: comments, submissions, reports, forum governance, user bans, operation logs in progress.
- Platform: JWT auth, rate limiting, local/Redis cache, API station status checks, actuator/Prometheus.

Verification already run before writing this plan:

- Backend: `mvn -q test` from `backend` passed.
- Frontend: `npm run build` from `frontend` passed.

Important working tree note:

- Existing uncommitted changes are present around admin operation logs:
  - `backend/src/main/java/com/aiblog/repository/AdminOperationLogRepository.java`
  - `backend/src/main/java/com/aiblog/controller/admin/AdminOperationLogController.java`
  - `backend/src/main/java/com/aiblog/service/AdminOperationLogService.java`
  - `backend/src/test/java/com/aiblog/service/AdminOperationLogServiceTest.java`
  - `frontend/src/api/index.ts`
  - `frontend/src/router/index.ts`
  - `frontend/src/views/admin/AdminLayout.vue`
  - `frontend/src/views/admin/AdminOperationLogs.vue`

Do not overwrite those changes. Treat them as user-owned work already in progress.

## Target Architecture

Use a modular-monolith architecture before considering service extraction:

- Identity and access: `AuthController`, `ForumUserService`, `JwtAuthFilter`, `SecurityConfig`.
- Content catalog: `Post`, `Skill`, `Mcp`, `ApiStation`, resource detail/list/admin CRUD.
- Discovery: `GlobalSearchService`, search API, future suggestions/facets.
- Community: forum categories, threads, replies, likes, favorites, notifications.
- Moderation and audit: reports, comment review, forum governance, operation logs.
- Platform operations: cache, rate limits, API status checks, metrics.

Dependency rule for future refactors:

- Controllers parse HTTP and map responses only.
- Services own business decisions and transaction boundaries.
- Repositories only persist/query entities.
- DTOs should prevent frontend contracts from depending directly on mutable JPA entity shape when the endpoint is not a simple internal admin CRUD endpoint.
- Cross-context operations, such as report approval hiding forum content, should go through service methods rather than repositories directly.

## Missing Or Weak Areas

Priority 1 defects:

- Public global search is documented and wired in the frontend, but `SecurityConfig` does not permit anonymous `GET /api/search`. Unauthenticated users can be blocked by `anyRequest().authenticated()`.

Priority 1 product gap:

- Resources have admin recommendation levels and user favorites, but no structured community review/rating. Comments exist, but comments do not create sortable trust signals such as average rating, review count, or a user's own review.

Priority 2 gaps for separate future plans:

- Search lacks autocomplete, popular keywords, and facet counts.
- Tags/categories are plain strings, so there is no central taxonomy management, aliasing, or de-duplication.
- JWT is stateless without refresh token rotation or logout/token revocation.
- Frontend has build checks but no Playwright smoke tests for the primary user/admin flows.
- API station status history exists, but there is no public uptime summary or incident-style reliability view.
- Admin operation logs are being added, but audit coverage should eventually be generalized across all admin mutations.

## Execution Scope For This Plan

This plan implements only:

1. Public search security regression fix.
2. Resource reviews and ratings for `POST`, `SKILL`, `MCP`, and `API`.

Keep all other gaps as separate plans after this batch is complete.

---

### Task 1: Fix Public Search Access

**Files:**

- Create: `backend/src/test/java/com/aiblog/config/SearchSecurityConfigTest.java`
- Modify: `backend/src/main/java/com/aiblog/config/SecurityConfig.java`

- [ ] **Step 1: Write the failing security regression test**

Create `backend/src/test/java/com/aiblog/config/SearchSecurityConfigTest.java`:

```java
package com.aiblog.config;

import com.aiblog.controller.SearchController;
import com.aiblog.dto.GlobalSearchResponse;
import com.aiblog.security.JwtAuthFilter;
import com.aiblog.security.JwtUtil;
import com.aiblog.service.GlobalSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class SearchSecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GlobalSearchService searchService;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    void searchEndpointIsPublicForAnonymousUsers() throws Exception {
        when(searchService.search("mcp", 5)).thenReturn(new GlobalSearchResponse("mcp", List.of()));

        mockMvc.perform(get("/api/search").param("q", "mcp").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("mcp"))
                .andExpect(jsonPath("$.totalCount").value(0));
    }
}
```

- [ ] **Step 2: Run the failing test**

Run:

```bash
cd backend
mvn -q -Dtest=SearchSecurityConfigTest test
```

Expected before the fix: FAIL with HTTP 401 or 403 for anonymous `GET /api/search`.

- [ ] **Step 3: Permit public GET search in Spring Security**

Modify the public GET matcher in `backend/src/main/java/com/aiblog/config/SecurityConfig.java`.

Replace the current public GET matcher block:

```java
.requestMatchers(HttpMethod.GET, "/api/posts/**", "/api/skills/**",
        "/api/mcps/**", "/api/api-stations/**", "/api/comments/**",
        "/api/forum/categories/**", "/api/forum/threads/**",
        "/api/resource-favorites/**",
        "/api/users/**").permitAll()
```

With:

```java
.requestMatchers(HttpMethod.GET, "/api/search", "/api/search/**",
        "/api/posts/**", "/api/skills/**",
        "/api/mcps/**", "/api/api-stations/**", "/api/comments/**",
        "/api/forum/categories/**", "/api/forum/threads/**",
        "/api/resource-favorites/**",
        "/api/users/**").permitAll()
```

- [ ] **Step 4: Verify the targeted backend test passes**

Run:

```bash
cd backend
mvn -q -Dtest=SearchSecurityConfigTest test
```

Expected: PASS.

- [ ] **Step 5: Verify the full backend suite**

Run:

```bash
cd backend
mvn -q test
```

Expected: PASS.

---

### Task 2: Add Resource Review Domain Model

**Files:**

- Create: `backend/src/main/java/com/aiblog/entity/ResourceReview.java`
- Create: `backend/src/main/java/com/aiblog/repository/ResourceReviewRepository.java`
- Create: `backend/src/main/java/com/aiblog/dto/ResourceReviewRequest.java`
- Create: `backend/src/main/java/com/aiblog/dto/ResourceReviewResponse.java`
- Create: `backend/src/main/java/com/aiblog/dto/ResourceReviewSummaryResponse.java`

- [ ] **Step 1: Create the JPA entity**

Create `backend/src/main/java/com/aiblog/entity/ResourceReview.java`:

```java
package com.aiblog.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "resource_review",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_resource_review_user_ref",
                        columnNames = {"user_id", "ref_type", "ref_id"})
        },
        indexes = {
                @Index(name = "idx_resource_review_ref", columnList = "ref_type,ref_id,status"),
                @Index(name = "idx_resource_review_user", columnList = "user_id,created_at")
        })
public class ResourceReview {

    public enum RefType { POST, SKILL, MCP, API }
    public enum ReviewStatus { NORMAL, HIDDEN, DELETED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ref_type", nullable = false, length = 20)
    private RefType refType;

    @Column(name = "ref_id", nullable = false)
    private Long refId;

    @Column(nullable = false)
    private Integer rating;

    @Column(length = 1000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewStatus status = ReviewStatus.NORMAL;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public RefType getRefType() { return refType; }
    public void setRefType(RefType refType) { this.refType = refType; }
    public Long getRefId() { return refId; }
    public void setRefId(Long refId) { this.refId = refId; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public ReviewStatus getStatus() { return status; }
    public void setStatus(ReviewStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
```

- [ ] **Step 2: Create the repository**

Create `backend/src/main/java/com/aiblog/repository/ResourceReviewRepository.java`:

```java
package com.aiblog.repository;

import com.aiblog.entity.ResourceReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ResourceReviewRepository extends JpaRepository<ResourceReview, Long> {

    Optional<ResourceReview> findByUserIdAndRefTypeAndRefId(
            Long userId,
            ResourceReview.RefType refType,
            Long refId);

    Page<ResourceReview> findByRefTypeAndRefIdAndStatusOrderByCreatedAtDesc(
            ResourceReview.RefType refType,
            Long refId,
            ResourceReview.ReviewStatus status,
            Pageable pageable);

    long countByRefTypeAndRefIdAndStatus(
            ResourceReview.RefType refType,
            Long refId,
            ResourceReview.ReviewStatus status);

    @Query("""
            select coalesce(avg(r.rating), 0)
            from ResourceReview r
            where r.refType = :refType
              and r.refId = :refId
              and r.status = com.aiblog.entity.ResourceReview$ReviewStatus.NORMAL
            """)
    double averageRating(ResourceReview.RefType refType, Long refId);
}
```

- [ ] **Step 3: Create request and response DTOs**

Create `backend/src/main/java/com/aiblog/dto/ResourceReviewRequest.java`:

```java
package com.aiblog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ResourceReviewRequest {

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    @Size(max = 1000)
    private String content;

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
```

Create `backend/src/main/java/com/aiblog/dto/ResourceReviewResponse.java`:

```java
package com.aiblog.dto;

import com.aiblog.entity.ResourceReview;

import java.time.Instant;

public class ResourceReviewResponse {
    private Long id;
    private Long userId;
    private ResourceReview.RefType refType;
    private Long refId;
    private Integer rating;
    private String content;
    private Instant createdAt;
    private Instant updatedAt;

    public static ResourceReviewResponse from(ResourceReview review) {
        ResourceReviewResponse response = new ResourceReviewResponse();
        response.setId(review.getId());
        response.setUserId(review.getUserId());
        response.setRefType(review.getRefType());
        response.setRefId(review.getRefId());
        response.setRating(review.getRating());
        response.setContent(review.getContent());
        response.setCreatedAt(review.getCreatedAt());
        response.setUpdatedAt(review.getUpdatedAt());
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public ResourceReview.RefType getRefType() { return refType; }
    public void setRefType(ResourceReview.RefType refType) { this.refType = refType; }
    public Long getRefId() { return refId; }
    public void setRefId(Long refId) { this.refId = refId; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
```

Create `backend/src/main/java/com/aiblog/dto/ResourceReviewSummaryResponse.java`:

```java
package com.aiblog.dto;

public class ResourceReviewSummaryResponse {
    private double averageRating;
    private long reviewCount;
    private ResourceReviewResponse myReview;

    public ResourceReviewSummaryResponse(double averageRating, long reviewCount, ResourceReviewResponse myReview) {
        this.averageRating = Math.round(averageRating * 10.0) / 10.0;
        this.reviewCount = reviewCount;
        this.myReview = myReview;
    }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
    public long getReviewCount() { return reviewCount; }
    public void setReviewCount(long reviewCount) { this.reviewCount = reviewCount; }
    public ResourceReviewResponse getMyReview() { return myReview; }
    public void setMyReview(ResourceReviewResponse myReview) { this.myReview = myReview; }
}
```

- [ ] **Step 4: Run compilation to catch entity/query errors**

Run:

```bash
cd backend
mvn -q -DskipTests compile
```

Expected: PASS.

---

### Task 3: Add Resource Review Service And API

**Files:**

- Create: `backend/src/main/java/com/aiblog/service/ResourceReviewService.java`
- Create: `backend/src/main/java/com/aiblog/controller/ResourceReviewController.java`
- Modify: `backend/src/main/java/com/aiblog/config/SecurityConfig.java`
- Create: `backend/src/test/java/com/aiblog/service/ResourceReviewServiceTest.java`

- [ ] **Step 1: Create the service**

Create `backend/src/main/java/com/aiblog/service/ResourceReviewService.java`:

```java
package com.aiblog.service;

import com.aiblog.dto.ResourceReviewRequest;
import com.aiblog.dto.ResourceReviewResponse;
import com.aiblog.dto.ResourceReviewSummaryResponse;
import com.aiblog.entity.Post;
import com.aiblog.entity.ResourceReview;
import com.aiblog.repository.ApiStationRepository;
import com.aiblog.repository.McpRepository;
import com.aiblog.repository.PostRepository;
import com.aiblog.repository.ResourceReviewRepository;
import com.aiblog.repository.SkillRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResourceReviewService {

    private final ResourceReviewRepository reviewRepo;
    private final PostRepository postRepo;
    private final SkillRepository skillRepo;
    private final McpRepository mcpRepo;
    private final ApiStationRepository apiRepo;

    public ResourceReviewService(ResourceReviewRepository reviewRepo,
                                 PostRepository postRepo,
                                 SkillRepository skillRepo,
                                 McpRepository mcpRepo,
                                 ApiStationRepository apiRepo) {
        this.reviewRepo = reviewRepo;
        this.postRepo = postRepo;
        this.skillRepo = skillRepo;
        this.mcpRepo = mcpRepo;
        this.apiRepo = apiRepo;
    }

    @Transactional(readOnly = true)
    public ResourceReviewSummaryResponse summary(ResourceReview.RefType refType, Long refId, Long userId) {
        validateTarget(refType, refId);
        ResourceReviewResponse myReview = userId == null ? null : reviewRepo
                .findByUserIdAndRefTypeAndRefId(userId, refType, refId)
                .filter(review -> review.getStatus() == ResourceReview.ReviewStatus.NORMAL)
                .map(ResourceReviewResponse::from)
                .orElse(null);
        return new ResourceReviewSummaryResponse(
                reviewRepo.averageRating(refType, refId),
                reviewRepo.countByRefTypeAndRefIdAndStatus(refType, refId, ResourceReview.ReviewStatus.NORMAL),
                myReview);
    }

    @Transactional(readOnly = true)
    public Page<ResourceReviewResponse> list(ResourceReview.RefType refType, Long refId, Pageable pageable) {
        validateTarget(refType, refId);
        return reviewRepo
                .findByRefTypeAndRefIdAndStatusOrderByCreatedAtDesc(
                        refType, refId, ResourceReview.ReviewStatus.NORMAL, pageable)
                .map(ResourceReviewResponse::from);
    }

    @Transactional
    public ResourceReviewResponse upsert(ResourceReview.RefType refType,
                                         Long refId,
                                         Long userId,
                                         ResourceReviewRequest request) {
        validateTarget(refType, refId);
        ResourceReview review = reviewRepo
                .findByUserIdAndRefTypeAndRefId(userId, refType, refId)
                .orElseGet(ResourceReview::new);
        review.setUserId(userId);
        review.setRefType(refType);
        review.setRefId(refId);
        review.setRating(request.getRating());
        review.setContent(clean(request.getContent()));
        review.setStatus(ResourceReview.ReviewStatus.NORMAL);
        return ResourceReviewResponse.from(reviewRepo.save(review));
    }

    @Transactional
    public boolean deleteOwn(ResourceReview.RefType refType, Long refId, Long userId) {
        validateTarget(refType, refId);
        return reviewRepo.findByUserIdAndRefTypeAndRefId(userId, refType, refId)
                .map(review -> {
                    review.setStatus(ResourceReview.ReviewStatus.DELETED);
                    reviewRepo.save(review);
                    return true;
                })
                .orElse(false);
    }

    private void validateTarget(ResourceReview.RefType refType, Long refId) {
        boolean exists = switch (refType) {
            case POST -> postRepo.findById(refId).filter(Post::isPublished).isPresent();
            case SKILL -> skillRepo.existsById(refId);
            case MCP -> mcpRepo.existsById(refId);
            case API -> apiRepo.existsById(refId);
        };
        if (!exists) {
            throw new IllegalArgumentException("资源不存在");
        }
    }

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
```

- [ ] **Step 2: Create the controller**

Create `backend/src/main/java/com/aiblog/controller/ResourceReviewController.java`:

```java
package com.aiblog.controller;

import com.aiblog.dto.ResourceReviewRequest;
import com.aiblog.entity.ForumUser;
import com.aiblog.entity.ResourceReview;
import com.aiblog.service.ForumUserService;
import com.aiblog.service.ResourceReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
public class ResourceReviewController {

    private final ResourceReviewService reviewService;
    private final ForumUserService userService;

    public ResourceReviewController(ResourceReviewService reviewService, ForumUserService userService) {
        this.reviewService = reviewService;
        this.userService = userService;
    }

    @GetMapping("/api/resource-reviews/{refType}/{refId}/summary")
    public ResponseEntity<?> summary(@PathVariable ResourceReview.RefType refType,
                                     @PathVariable Long refId,
                                     Authentication auth) {
        try {
            return ResponseEntity.ok(reviewService.summary(refType, refId, resolveUserId(auth)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/api/resource-reviews/{refType}/{refId}")
    public ResponseEntity<?> list(@PathVariable ResourceReview.RefType refType,
                                  @PathVariable Long refId,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size) {
        try {
            Page<?> reviews = reviewService.list(
                    refType,
                    refId,
                    PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size))));
            return ResponseEntity.ok(reviews);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/api/account/resource-reviews/{refType}/{refId}")
    public ResponseEntity<?> upsert(@PathVariable ResourceReview.RefType refType,
                                    @PathVariable Long refId,
                                    @Valid @RequestBody ResourceReviewRequest request,
                                    Authentication auth) {
        Long userId = requireForumUserId(auth);
        try {
            return ResponseEntity.ok(reviewService.upsert(refType, refId, userId, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/api/account/resource-reviews/{refType}/{refId}")
    public ResponseEntity<?> deleteOwn(@PathVariable ResourceReview.RefType refType,
                                       @PathVariable Long refId,
                                       Authentication auth) {
        Long userId = requireForumUserId(auth);
        try {
            return reviewService.deleteOwn(refType, refId, userId)
                    ? ResponseEntity.noContent().build()
                    : ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private Long resolveUserId(Authentication auth) {
        if (auth == null) return null;
        return userService.findByUsername(auth.getName()).map(ForumUser::getId).orElse(null);
    }

    private Long requireForumUserId(Authentication auth) {
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return userService.findByUsername(auth.getName())
                .map(ForumUser::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅论坛用户可评价资源"));
    }
}
```

- [ ] **Step 3: Permit public review reads**

In `backend/src/main/java/com/aiblog/config/SecurityConfig.java`, add public review read routes to the public GET matcher from Task 1:

```java
"/api/resource-reviews/**",
```

The full public GET matcher should include:

```java
.requestMatchers(HttpMethod.GET, "/api/search", "/api/search/**",
        "/api/posts/**", "/api/skills/**",
        "/api/mcps/**", "/api/api-stations/**", "/api/comments/**",
        "/api/forum/categories/**", "/api/forum/threads/**",
        "/api/resource-favorites/**", "/api/resource-reviews/**",
        "/api/users/**").permitAll()
```

- [ ] **Step 4: Write focused service tests**

Create `backend/src/test/java/com/aiblog/service/ResourceReviewServiceTest.java`:

```java
package com.aiblog.service;

import com.aiblog.dto.ResourceReviewRequest;
import com.aiblog.entity.ResourceReview;
import com.aiblog.entity.Skill;
import com.aiblog.repository.ApiStationRepository;
import com.aiblog.repository.McpRepository;
import com.aiblog.repository.PostRepository;
import com.aiblog.repository.ResourceReviewRepository;
import com.aiblog.repository.SkillRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResourceReviewServiceTest {

    @Test
    void summaryIncludesAverageCountAndCurrentUserReview() {
        ResourceReviewRepository reviewRepo = mock(ResourceReviewRepository.class);
        SkillRepository skillRepo = mock(SkillRepository.class);
        when(skillRepo.existsById(9L)).thenReturn(true);
        ResourceReview review = review(3L, 7L, ResourceReview.RefType.SKILL, 9L, 5, "好用");
        when(reviewRepo.findByUserIdAndRefTypeAndRefId(7L, ResourceReview.RefType.SKILL, 9L))
                .thenReturn(Optional.of(review));
        when(reviewRepo.averageRating(ResourceReview.RefType.SKILL, 9L)).thenReturn(4.6);
        when(reviewRepo.countByRefTypeAndRefIdAndStatus(
                ResourceReview.RefType.SKILL, 9L, ResourceReview.ReviewStatus.NORMAL)).thenReturn(12L);

        ResourceReviewService service = service(reviewRepo, skillRepo);

        var summary = service.summary(ResourceReview.RefType.SKILL, 9L, 7L);

        assertThat(summary.getAverageRating()).isEqualTo(4.6);
        assertThat(summary.getReviewCount()).isEqualTo(12);
        assertThat(summary.getMyReview().getRating()).isEqualTo(5);
    }

    @Test
    void upsertCreatesReviewWhenCurrentUserHasNoReview() {
        ResourceReviewRepository reviewRepo = mock(ResourceReviewRepository.class);
        SkillRepository skillRepo = mock(SkillRepository.class);
        when(skillRepo.existsById(9L)).thenReturn(true);
        when(reviewRepo.findByUserIdAndRefTypeAndRefId(7L, ResourceReview.RefType.SKILL, 9L))
                .thenReturn(Optional.empty());
        when(reviewRepo.save(any(ResourceReview.class))).thenAnswer(invocation -> {
            ResourceReview review = invocation.getArgument(0);
            review.setId(101L);
            review.setCreatedAt(Instant.parse("2026-06-02T12:00:00Z"));
            review.setUpdatedAt(Instant.parse("2026-06-02T12:00:00Z"));
            return review;
        });
        ResourceReviewRequest request = new ResourceReviewRequest();
        request.setRating(4);
        request.setContent("  安装简单  ");

        ResourceReviewService service = service(reviewRepo, skillRepo);

        var response = service.upsert(ResourceReview.RefType.SKILL, 9L, 7L, request);

        assertThat(response.getId()).isEqualTo(101L);
        assertThat(response.getRating()).isEqualTo(4);
        assertThat(response.getContent()).isEqualTo("安装简单");
    }

    private ResourceReviewService service(ResourceReviewRepository reviewRepo, SkillRepository skillRepo) {
        return new ResourceReviewService(
                reviewRepo,
                mock(PostRepository.class),
                skillRepo,
                mock(McpRepository.class),
                mock(ApiStationRepository.class));
    }

    private ResourceReview review(Long id,
                                  Long userId,
                                  ResourceReview.RefType refType,
                                  Long refId,
                                  int rating,
                                  String content) {
        ResourceReview review = new ResourceReview();
        review.setId(id);
        review.setUserId(userId);
        review.setRefType(refType);
        review.setRefId(refId);
        review.setRating(rating);
        review.setContent(content);
        review.setStatus(ResourceReview.ReviewStatus.NORMAL);
        review.setCreatedAt(Instant.parse("2026-06-02T10:00:00Z"));
        review.setUpdatedAt(Instant.parse("2026-06-02T10:00:00Z"));
        return review;
    }
}
```

- [ ] **Step 5: Verify backend tests**

Run:

```bash
cd backend
mvn -q -Dtest=ResourceReviewServiceTest,SearchSecurityConfigTest test
mvn -q test
```

Expected: both commands PASS.

---

### Task 4: Add Frontend Review API And Types

**Files:**

- Modify: `frontend/src/api/types.ts`
- Modify: `frontend/src/api/index.ts`

- [ ] **Step 1: Add TypeScript types**

Append these interfaces after `ResourceFavoriteItem` in `frontend/src/api/types.ts`:

```ts
export interface ResourceReview {
  id: number
  userId: number
  refType: ResourceFavoriteRefType
  refId: number
  rating: number
  content?: string
  createdAt: string
  updatedAt: string
}

export interface ResourceReviewSummary {
  averageRating: number
  reviewCount: number
  myReview?: ResourceReview | null
}
```

- [ ] **Step 2: Import the new types in the API module**

Modify the type import block in `frontend/src/api/index.ts` to include:

```ts
ResourceReview,
ResourceReviewSummary,
```

- [ ] **Step 3: Add public and account API methods**

In `frontend/src/api/index.ts`, add these methods to `publicApi` after `resourceFavoriteInteraction`:

```ts
resourceReviewSummary: (refType: ResourceFavoriteRefType, refId: number) =>
  http.get<ResourceReviewSummary>(`/resource-reviews/${refType}/${refId}/summary`).then(r => r.data),
resourceReviews: (refType: ResourceFavoriteRefType, refId: number, params?: { page?: number; size?: number }) =>
  http.get<Page<ResourceReview>>(`/resource-reviews/${refType}/${refId}`, { params }).then(r => r.data),
```

Add these methods to `accountApi` after `unfavoriteResource`:

```ts
upsertResourceReview: (
  refType: ResourceFavoriteRefType,
  refId: number,
  body: { rating: number; content?: string }
) => http.post<ResourceReview>(`/account/resource-reviews/${refType}/${refId}`, body).then(r => r.data),
deleteResourceReview: (refType: ResourceFavoriteRefType, refId: number) =>
  http.delete(`/account/resource-reviews/${refType}/${refId}`),
```

- [ ] **Step 4: Run frontend typecheck/build**

Run:

```bash
cd frontend
npm run build
```

Expected: PASS.

---

### Task 5: Add Reusable Resource Review Panel

**Files:**

- Create: `frontend/src/components/ResourceReviewPanel.vue`
- Modify: `frontend/src/views/TutorialDetail.vue`
- Modify: `frontend/src/views/SkillDetail.vue`
- Modify: `frontend/src/views/McpDetail.vue`
- Modify: `frontend/src/views/ApiStationDetail.vue`

- [ ] **Step 1: Create the review component**

Create `frontend/src/components/ResourceReviewPanel.vue`:

```vue
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { accountApi, publicApi } from '../api'
import type { Page, ResourceFavoriteRefType, ResourceReview, ResourceReviewSummary } from '../api/types'
import { useAuthStore } from '../stores/auth'
import { useToast } from '../composables/useToast'

const props = defineProps<{
  refType: ResourceFavoriteRefType
  refId: number
}>()

const auth = useAuthStore()
const toast = useToast()
const summary = ref<ResourceReviewSummary | null>(null)
const page = ref<Page<ResourceReview> | null>(null)
const rating = ref(5)
const content = ref('')
const loading = ref(false)

const reviews = computed(() => page.value?.content ?? [])
const canSubmit = computed(() => auth.isLoggedIn() && rating.value >= 1 && rating.value <= 5)

async function load() {
  summary.value = await publicApi.resourceReviewSummary(props.refType, props.refId)
  page.value = await publicApi.resourceReviews(props.refType, props.refId, { page: 0, size: 10 })
  if (summary.value?.myReview) {
    rating.value = summary.value.myReview.rating
    content.value = summary.value.myReview.content ?? ''
  }
}

async function submit() {
  if (!canSubmit.value) return
  loading.value = true
  try {
    await accountApi.upsertResourceReview(props.refType, props.refId, {
      rating: rating.value,
      content: content.value
    })
    toast.success('评价已保存')
    await load()
  } catch {
    toast.error('评价保存失败')
  } finally {
    loading.value = false
  }
}

async function remove() {
  loading.value = true
  try {
    await accountApi.deleteResourceReview(props.refType, props.refId)
    rating.value = 5
    content.value = ''
    toast.success('评价已删除')
    await load()
  } catch {
    toast.error('评价删除失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="card review-panel">
    <div class="review-head">
      <div>
        <h2 class="section-title">资源评价</h2>
        <p class="muted">
          {{ summary?.reviewCount || 0 }} 条评价
          <span v-if="summary && summary.reviewCount"> · 平均 {{ summary.averageRating.toFixed(1) }}/5</span>
        </p>
      </div>
      <div class="score">{{ summary?.reviewCount ? summary.averageRating.toFixed(1) : '-' }}</div>
    </div>

    <div v-if="auth.isLoggedIn()" class="review-form">
      <label class="field">
        <span>评分</span>
        <select v-model.number="rating">
          <option v-for="n in [5, 4, 3, 2, 1]" :key="n" :value="n">{{ n }} 分</option>
        </select>
      </label>
      <label class="field">
        <span>评价</span>
        <textarea v-model="content" maxlength="1000" rows="4" placeholder="写下实际体验、适用场景或注意事项"></textarea>
      </label>
      <div class="actions">
        <button class="btn" :disabled="loading || !canSubmit" @click="submit">保存评价</button>
        <button v-if="summary?.myReview" class="btn btn-ghost" :disabled="loading" @click="remove">删除</button>
      </div>
    </div>

    <div v-else class="login-hint muted">登录后可以发表评价。</div>

    <div class="review-list">
      <article v-for="review in reviews" :key="review.id" class="review-item">
        <div class="review-meta">
          <strong>{{ review.rating }} 分</strong>
          <span class="muted">{{ new Date(review.createdAt).toLocaleString() }}</span>
        </div>
        <p v-if="review.content">{{ review.content }}</p>
      </article>
      <p v-if="!reviews.length" class="muted empty">暂无评价。</p>
    </div>
  </section>
</template>

<style scoped>
.review-panel { padding: 22px; margin-top: 24px; }
.review-head { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.review-head .section-title { margin: 0 0 6px; }
.score { font-size: 30px; font-weight: 800; color: var(--primary); font-family: var(--font-mono); }
.review-form { margin-top: 18px; display: grid; gap: 12px; }
.field { display: grid; gap: 6px; font-size: 14px; color: var(--text-soft); }
.field select,
.field textarea {
  width: 100%;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg);
  color: var(--text);
  padding: 10px 12px;
}
.field textarea { resize: vertical; min-height: 96px; line-height: 1.6; }
.actions { display: flex; gap: 10px; flex-wrap: wrap; }
.login-hint { margin-top: 16px; }
.review-list { margin-top: 20px; display: grid; gap: 12px; }
.review-item { border-top: 1px solid var(--border); padding-top: 12px; }
.review-meta { display: flex; justify-content: space-between; gap: 12px; font-size: 13px; }
.review-item p { margin: 8px 0 0; line-height: 1.7; }
.empty { margin: 0; }
</style>
```

- [ ] **Step 2: Add the panel to detail pages**

In each detail page, import the component:

```ts
import ResourceReviewPanel from '../components/ResourceReviewPanel.vue'
```

Then render it after the existing favorite/comment/discussion area, using the page's loaded resource id:

```vue
<ResourceReviewPanel v-if="post?.id" ref-type="POST" :ref-id="post.id" />
```

```vue
<ResourceReviewPanel v-if="skill?.id" ref-type="SKILL" :ref-id="skill.id" />
```

```vue
<ResourceReviewPanel v-if="mcp?.id" ref-type="MCP" :ref-id="mcp.id" />
```

```vue
<ResourceReviewPanel v-if="station?.id" ref-type="API" :ref-id="station.id" />
```

Use the existing resource variable names in:

- `frontend/src/views/TutorialDetail.vue`
- `frontend/src/views/SkillDetail.vue`
- `frontend/src/views/McpDetail.vue`
- `frontend/src/views/ApiStationDetail.vue`

- [ ] **Step 3: Verify frontend build**

Run:

```bash
cd frontend
npm run build
```

Expected: PASS.

---

### Task 6: Final Verification

**Files:**

- No new files.

- [ ] **Step 1: Run all backend tests**

Run:

```bash
cd backend
mvn -q test
```

Expected: PASS.

- [ ] **Step 2: Run frontend production build**

Run:

```bash
cd frontend
npm run build
```

Expected: PASS.

- [ ] **Step 3: Manual smoke test**

Start backend and frontend:

```bash
cd backend
mvn spring-boot:run
```

```bash
cd frontend
npm run dev
```

Verify:

- Anonymous `/search?q=mcp` works and shows results or an empty result state, not a login redirect or 401.
- A logged-in forum user can add a review on a Skill detail page.
- Refreshing the Skill detail page shows the review summary and the user's own review.
- Anonymous users can view review summary and review list.
- Deleting the user's review removes it from the visible list and summary count.

## Suggested Follow-Up Plans

Create separate plans after this batch for:

- Search suggestions and facet counts.
- Central tag/category taxonomy management.
- JWT refresh token rotation and logout/token revocation.
- API station uptime summary and incident notes.
- Playwright smoke tests for public browsing, login, forum posting, and admin moderation.
- Generalized admin audit logging coverage after the current operation-log work is complete.

## Self-Review

- Spec coverage: The user asked for project analysis, architecture design, missing feature identification, and a plan before execution. This document covers all four and limits execution scope to a coherent first batch.
- Placeholder scan: No implementation step depends on unspecified classes or methods. Future independent features are explicitly listed as separate follow-up plans.
- Type consistency: Backend review `RefType` values match frontend `ResourceFavoriteRefType` values: `POST`, `SKILL`, `MCP`, `API`.
