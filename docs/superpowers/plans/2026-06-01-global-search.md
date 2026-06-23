# Global Search Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a public global search feature that searches tutorials, Skills, MCP servers, API stations, and visible forum threads from one page.

**Architecture:** Add a read-only Spring service and controller at `/api/search` that aggregate existing repositories into typed result groups. Add a Vue route `/search` that reads `q` from the URL, calls the endpoint, and renders grouped result cards with links into each existing section.

**Tech Stack:** Spring Boot 3, Spring Data JPA, Java 21, Vue 3, Vue Router, TypeScript, Vite.

---

## File Structure

- Create: `backend/src/main/java/com/aiblog/dto/GlobalSearchItemResponse.java`
  - Response DTO for one search result card.
- Create: `backend/src/main/java/com/aiblog/dto/GlobalSearchGroupResponse.java`
  - Response DTO for one result group such as `POST` or `SKILL`.
- Create: `backend/src/main/java/com/aiblog/dto/GlobalSearchResponse.java`
  - Top-level response containing normalized query, total count, and groups.
- Create: `backend/src/main/java/com/aiblog/service/GlobalSearchService.java`
  - Aggregates searches across posts, skills, MCPs, API stations, and forum threads.
- Create: `backend/src/main/java/com/aiblog/controller/SearchController.java`
  - Public controller exposing `GET /api/search?q=&limit=`.
- Modify: `backend/src/main/java/com/aiblog/repository/PostRepository.java`
  - Add `searchPublished(String q, Pageable pageable)` for published tutorial search.
- Create: `backend/src/test/java/com/aiblog/service/GlobalSearchServiceTest.java`
  - Unit tests for blank-query behavior, grouping, links, and per-group limit.
- Modify: `frontend/src/api/types.ts`
  - Add TypeScript types for global search response and result item.
- Modify: `frontend/src/api/index.ts`
  - Add `publicApi.search(params)`.
- Create: `frontend/src/views/Search.vue`
  - Public global search page with URL-synced query, state blocks, and grouped cards.
- Modify: `frontend/src/router/index.ts`
  - Add `/search` route.
- Modify: `frontend/src/components/NavBar.vue`
  - Add a search navigation item.
- Modify: `README.md`
  - Document `GET /api/search`.

---

### Task 1: Backend Global Search API

**Files:**
- Create: `backend/src/main/java/com/aiblog/dto/GlobalSearchItemResponse.java`
- Create: `backend/src/main/java/com/aiblog/dto/GlobalSearchGroupResponse.java`
- Create: `backend/src/main/java/com/aiblog/dto/GlobalSearchResponse.java`
- Create: `backend/src/main/java/com/aiblog/service/GlobalSearchService.java`
- Create: `backend/src/main/java/com/aiblog/controller/SearchController.java`
- Modify: `backend/src/main/java/com/aiblog/repository/PostRepository.java`
- Test: `backend/src/test/java/com/aiblog/service/GlobalSearchServiceTest.java`

- [ ] **Step 1: Write the failing service tests**

Create `backend/src/test/java/com/aiblog/service/GlobalSearchServiceTest.java`:

```java
package com.aiblog.service;

import com.aiblog.dto.GlobalSearchResponse;
import com.aiblog.entity.ApiStation;
import com.aiblog.entity.ForumThread;
import com.aiblog.entity.Mcp;
import com.aiblog.entity.Post;
import com.aiblog.entity.Skill;
import com.aiblog.repository.ApiStationRepository;
import com.aiblog.repository.ForumThreadRepository;
import com.aiblog.repository.McpRepository;
import com.aiblog.repository.PostRepository;
import com.aiblog.repository.SkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalSearchServiceTest {

    @Mock private PostRepository postRepo;
    @Mock private SkillRepository skillRepo;
    @Mock private McpRepository mcpRepo;
    @Mock private ApiStationRepository apiRepo;
    @Mock private ForumThreadRepository threadRepo;

    private GlobalSearchService service;

    @BeforeEach
    void setUp() {
        service = new GlobalSearchService(postRepo, skillRepo, mcpRepo, apiRepo, threadRepo);
    }

    @Test
    void blankQueryReturnsEmptyGroupsWithoutRepositoryCalls() {
        GlobalSearchResponse response = service.search("   ", 5);

        assertThat(response.getQuery()).isEmpty();
        assertThat(response.getTotalCount()).isZero();
        assertThat(response.getGroups()).isEmpty();
        verify(postRepo, never()).searchPublished(any(), any());
        verify(skillRepo, never()).findAll(any(Specification.class), any(Pageable.class));
        verify(threadRepo, never()).searchVisible(any(), any(), any(), any());
    }

    @Test
    void searchesAllPublicContentTypesAndBuildsStableLinks() {
        Post post = new Post();
        post.setId(1L);
        post.setTitle("MCP 入门");
        post.setSlug("mcp-guide");
        post.setSummary("模型上下文协议教程");
        post.setTags("MCP,教程");
        post.setCategory("进阶");
        post.setCreatedAt(Instant.parse("2026-06-01T00:00:00Z"));

        Skill skill = new Skill();
        skill.setId(2L);
        skill.setName("MCP 调试 Skill");
        skill.setDescription("调试 MCP 服务器");
        skill.setTags("MCP,调试");
        skill.setCategory("开发");
        skill.setRecommendLevel(5);

        Mcp mcp = new Mcp();
        mcp.setId(3L);
        mcp.setName("filesystem");
        mcp.setDescription("文件系统 MCP");
        mcp.setTags("官方,文件");
        mcp.setCategory("官方");
        mcp.setRecommendLevel(4);

        ApiStation api = new ApiStation();
        api.setId(4L);
        api.setName("OpenAI 官方");
        api.setBaseUrl("https://api.openai.com");
        api.setDescription("API 可用性基准");
        api.setSupportedModels("gpt-4o");
        api.setTags("官方");
        api.setStatus(ApiStation.Status.UP);

        ForumThread thread = new ForumThread();
        thread.setId(5L);
        thread.setCategoryId(10L);
        thread.setAuthorId(20L);
        thread.setTitle("MCP 开发问题");
        thread.setContentMarkdown("如何调试 MCP Server");
        thread.setTags("MCP,开发");
        thread.setStatus(ForumThread.ThreadStatus.NORMAL);
        thread.setReplyCount(3);
        thread.setViewCount(12);

        when(postRepo.searchPublished(eq("MCP"), any(Pageable.class))).thenReturn(List.of(post));
        when(skillRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(skill)));
        when(mcpRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(mcp)));
        when(apiRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(api)));
        when(threadRepo.searchVisible(eq(null), eq("MCP"), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(thread)));

        GlobalSearchResponse response = service.search(" MCP ", 8);

        assertThat(response.getQuery()).isEqualTo("MCP");
        assertThat(response.getTotalCount()).isEqualTo(5);
        assertThat(response.getGroups()).extracting("type")
                .containsExactly("POST", "SKILL", "MCP", "API", "FORUM_THREAD");
        assertThat(response.getGroups().get(0).getItems().get(0).getUrl()).isEqualTo("/tutorials/mcp-guide");
        assertThat(response.getGroups().get(1).getItems().get(0).getUrl()).isEqualTo("/skills");
        assertThat(response.getGroups().get(2).getItems().get(0).getUrl()).isEqualTo("/mcps");
        assertThat(response.getGroups().get(3).getItems().get(0).getUrl()).isEqualTo("/api-stations");
        assertThat(response.getGroups().get(4).getItems().get(0).getUrl()).isEqualTo("/forum/threads/5");
    }

    @Test
    void clampsLimitBetweenOneAndTwenty() {
        when(postRepo.searchPublished(eq("AI"), any(Pageable.class))).thenReturn(List.of());
        when(skillRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(mcpRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(apiRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(threadRepo.searchVisible(eq(null), eq("AI"), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        service.search("AI", 99);

        verify(postRepo).searchPublished(eq("AI"), eq(PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }
}
```

- [ ] **Step 2: Run the failing backend test**

Run:

```bash
cd backend
mvn -q -Dtest=GlobalSearchServiceTest test
```

Expected: FAIL because `GlobalSearchService`, DTOs, and `PostRepository.searchPublished` do not exist yet.

- [ ] **Step 3: Create global search DTOs**

Create `backend/src/main/java/com/aiblog/dto/GlobalSearchItemResponse.java`:

```java
package com.aiblog.dto;

import java.time.Instant;

public class GlobalSearchItemResponse {
    private String type;
    private Long id;
    private String title;
    private String description;
    private String url;
    private String category;
    private String tags;
    private String meta;
    private Instant createdAt;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getMeta() { return meta; }
    public void setMeta(String meta) { this.meta = meta; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
```

Create `backend/src/main/java/com/aiblog/dto/GlobalSearchGroupResponse.java`:

```java
package com.aiblog.dto;

import java.util.List;

public class GlobalSearchGroupResponse {
    private String type;
    private String label;
    private List<GlobalSearchItemResponse> items;

    public GlobalSearchGroupResponse(String type, String label, List<GlobalSearchItemResponse> items) {
        this.type = type;
        this.label = label;
        this.items = items;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public List<GlobalSearchItemResponse> getItems() { return items; }
    public void setItems(List<GlobalSearchItemResponse> items) { this.items = items; }
}
```

Create `backend/src/main/java/com/aiblog/dto/GlobalSearchResponse.java`:

```java
package com.aiblog.dto;

import java.util.List;

public class GlobalSearchResponse {
    private String query;
    private int totalCount;
    private List<GlobalSearchGroupResponse> groups;

    public GlobalSearchResponse(String query, List<GlobalSearchGroupResponse> groups) {
        this.query = query;
        this.groups = groups;
        this.totalCount = groups.stream().mapToInt(group -> group.getItems().size()).sum();
    }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    public List<GlobalSearchGroupResponse> getGroups() { return groups; }
    public void setGroups(List<GlobalSearchGroupResponse> groups) { this.groups = groups; }
}
```

- [ ] **Step 4: Add published post search repository method**

Modify `backend/src/main/java/com/aiblog/repository/PostRepository.java`:

```java
package com.aiblog.repository;

import com.aiblog.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    Optional<Post> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<Post> findByPublishedTrueOrderByCreatedAtDesc();

    @Query("""
            select p from Post p
            where p.published = true
              and (
                lower(p.title) like lower(concat('%', :q, '%'))
                or lower(coalesce(p.summary, '')) like lower(concat('%', :q, '%'))
                or lower(coalesce(p.tags, '')) like lower(concat('%', :q, '%'))
                or lower(coalesce(p.category, '')) like lower(concat('%', :q, '%'))
              )
            """)
    List<Post> searchPublished(@Param("q") String q, Pageable pageable);
}
```

- [ ] **Step 5: Implement global search service**

Create `backend/src/main/java/com/aiblog/service/GlobalSearchService.java`:

```java
package com.aiblog.service;

import com.aiblog.dto.GlobalSearchGroupResponse;
import com.aiblog.dto.GlobalSearchItemResponse;
import com.aiblog.dto.GlobalSearchResponse;
import com.aiblog.entity.ApiStation;
import com.aiblog.entity.ForumThread;
import com.aiblog.entity.Mcp;
import com.aiblog.entity.Post;
import com.aiblog.entity.Skill;
import com.aiblog.repository.ApiStationRepository;
import com.aiblog.repository.ForumThreadRepository;
import com.aiblog.repository.McpRepository;
import com.aiblog.repository.PostRepository;
import com.aiblog.repository.SkillRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GlobalSearchService {

    private static final int DEFAULT_LIMIT = 6;
    private static final List<ForumThread.ThreadStatus> VISIBLE_THREAD_STATUSES = List.of(
            ForumThread.ThreadStatus.NORMAL,
            ForumThread.ThreadStatus.PINNED,
            ForumThread.ThreadStatus.FEATURED,
            ForumThread.ThreadStatus.LOCKED
    );

    private final PostRepository postRepo;
    private final SkillRepository skillRepo;
    private final McpRepository mcpRepo;
    private final ApiStationRepository apiRepo;
    private final ForumThreadRepository threadRepo;

    public GlobalSearchService(PostRepository postRepo,
                               SkillRepository skillRepo,
                               McpRepository mcpRepo,
                               ApiStationRepository apiRepo,
                               ForumThreadRepository threadRepo) {
        this.postRepo = postRepo;
        this.skillRepo = skillRepo;
        this.mcpRepo = mcpRepo;
        this.apiRepo = apiRepo;
        this.threadRepo = threadRepo;
    }

    public GlobalSearchResponse search(String rawQuery, Integer rawLimit) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        if (query.isEmpty()) {
            return new GlobalSearchResponse("", List.of());
        }

        int limit = clampLimit(rawLimit);
        List<GlobalSearchGroupResponse> groups = new ArrayList<>();

        List<GlobalSearchItemResponse> posts = postRepo
                .searchPublished(query, PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .map(this::fromPost)
                .toList();
        addGroup(groups, "POST", "教程", posts);

        List<GlobalSearchItemResponse> skills = skillRepo
                .findAll(
                        SearchSpecs.build(query, null, null, List.of("name", "description", "tags", "category")),
                        PageRequest.of(0, limit,
                                Sort.by(Sort.Direction.DESC, "recommendLevel").and(Sort.by(Sort.Direction.DESC, "createdAt"))))
                .stream()
                .map(this::fromSkill)
                .toList();
        addGroup(groups, "SKILL", "AI Skill", skills);

        List<GlobalSearchItemResponse> mcps = mcpRepo
                .findAll(
                        SearchSpecs.build(query, null, null, List.of("name", "description", "tags", "category")),
                        PageRequest.of(0, limit,
                                Sort.by(Sort.Direction.DESC, "recommendLevel").and(Sort.by(Sort.Direction.DESC, "createdAt"))))
                .stream()
                .map(this::fromMcp)
                .toList();
        addGroup(groups, "MCP", "MCP", mcps);

        List<GlobalSearchItemResponse> apiStations = apiRepo
                .findAll(
                        SearchSpecs.build(query, null, null, List.of("name", "description", "supportedModels", "tags")),
                        PageRequest.of(0, limit, Sort.by(Sort.Direction.ASC, "name")))
                .stream()
                .map(this::fromApiStation)
                .toList();
        addGroup(groups, "API", "公益 API", apiStations);

        List<GlobalSearchItemResponse> threads = threadRepo
                .searchVisible(null, query, VISIBLE_THREAD_STATUSES,
                        PageRequest.of(0, limit,
                                Sort.by(Sort.Direction.DESC, "lastReplyAt").and(Sort.by(Sort.Direction.DESC, "createdAt"))))
                .stream()
                .map(this::fromForumThread)
                .toList();
        addGroup(groups, "FORUM_THREAD", "论坛讨论", threads);

        return new GlobalSearchResponse(query, groups);
    }

    private int clampLimit(Integer rawLimit) {
        int limit = rawLimit == null ? DEFAULT_LIMIT : rawLimit;
        return Math.max(1, Math.min(20, limit));
    }

    private void addGroup(List<GlobalSearchGroupResponse> groups,
                          String type,
                          String label,
                          List<GlobalSearchItemResponse> items) {
        if (!items.isEmpty()) {
            groups.add(new GlobalSearchGroupResponse(type, label, items));
        }
    }

    private GlobalSearchItemResponse fromPost(Post post) {
        GlobalSearchItemResponse item = base("POST", post.getId(), post.getTitle(), post.getSummary(),
                "/tutorials/" + post.getSlug(), post.getCategory(), post.getTags());
        item.setCreatedAt(post.getCreatedAt());
        return item;
    }

    private GlobalSearchItemResponse fromSkill(Skill skill) {
        GlobalSearchItemResponse item = base("SKILL", skill.getId(), skill.getName(), skill.getDescription(),
                "/skills", skill.getCategory(), skill.getTags());
        item.setCreatedAt(skill.getCreatedAt());
        item.setMeta("推荐 " + nullToDefault(skill.getRecommendLevel(), 0) + "/5");
        return item;
    }

    private GlobalSearchItemResponse fromMcp(Mcp mcp) {
        GlobalSearchItemResponse item = base("MCP", mcp.getId(), mcp.getName(), mcp.getDescription(),
                "/mcps", mcp.getCategory(), mcp.getTags());
        item.setCreatedAt(mcp.getCreatedAt());
        item.setMeta("推荐 " + nullToDefault(mcp.getRecommendLevel(), 0) + "/5");
        return item;
    }

    private GlobalSearchItemResponse fromApiStation(ApiStation apiStation) {
        GlobalSearchItemResponse item = base("API", apiStation.getId(), apiStation.getName(),
                apiStation.getDescription(), "/api-stations", null, apiStation.getTags());
        item.setCreatedAt(apiStation.getCreatedAt());
        item.setMeta(apiStation.getStatus().name() + formatLatency(apiStation.getLatencyMs()));
        return item;
    }

    private GlobalSearchItemResponse fromForumThread(ForumThread thread) {
        GlobalSearchItemResponse item = base("FORUM_THREAD", thread.getId(), thread.getTitle(),
                excerpt(thread.getContentMarkdown()), "/forum/threads/" + thread.getId(), null, thread.getTags());
        item.setCreatedAt(thread.getCreatedAt());
        item.setMeta(thread.getReplyCount() + " 回复 · " + thread.getViewCount() + " 浏览");
        return item;
    }

    private GlobalSearchItemResponse base(String type,
                                          Long id,
                                          String title,
                                          String description,
                                          String url,
                                          String category,
                                          String tags) {
        GlobalSearchItemResponse item = new GlobalSearchItemResponse();
        item.setType(type);
        item.setId(id);
        item.setTitle(title);
        item.setDescription(excerpt(description));
        item.setUrl(url);
        item.setCategory(category);
        item.setTags(tags);
        return item;
    }

    private String excerpt(String value) {
        if (value == null) return null;
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 180 ? normalized : normalized.substring(0, 177) + "...";
    }

    private Integer nullToDefault(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String formatLatency(Integer latencyMs) {
        return latencyMs == null ? "" : " · " + latencyMs + "ms";
    }
}
```

- [ ] **Step 6: Expose the public search controller**

Create `backend/src/main/java/com/aiblog/controller/SearchController.java`:

```java
package com.aiblog.controller;

import com.aiblog.dto.GlobalSearchResponse;
import com.aiblog.service.GlobalSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final GlobalSearchService searchService;

    public SearchController(GlobalSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public GlobalSearchResponse search(@RequestParam(required = false) String q,
                                       @RequestParam(required = false) Integer limit) {
        return searchService.search(q, limit);
    }
}
```

- [ ] **Step 7: Run backend search tests**

Run:

```bash
cd backend
mvn -q -Dtest=GlobalSearchServiceTest test
```

Expected: PASS.

---

### Task 2: Frontend Global Search Page

**Files:**
- Modify: `frontend/src/api/types.ts`
- Modify: `frontend/src/api/index.ts`
- Create: `frontend/src/views/Search.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/components/NavBar.vue`

- [ ] **Step 1: Add TypeScript search types**

Append to `frontend/src/api/types.ts`:

```ts
export type GlobalSearchType = 'POST' | 'SKILL' | 'MCP' | 'API' | 'FORUM_THREAD'

export interface GlobalSearchItem {
  type: GlobalSearchType
  id: number
  title: string
  description?: string
  url: string
  category?: string
  tags?: string
  meta?: string
  createdAt?: string
}

export interface GlobalSearchGroup {
  type: GlobalSearchType
  label: string
  items: GlobalSearchItem[]
}

export interface GlobalSearchResponse {
  query: string
  totalCount: number
  groups: GlobalSearchGroup[]
}
```

- [ ] **Step 2: Add frontend API method**

Modify imports in `frontend/src/api/index.ts` to include `GlobalSearchResponse`, then add this method inside `publicApi`:

```ts
  search: (params?: { q?: string; limit?: number }) =>
    http.get<GlobalSearchResponse>('/search', { params }).then(r => r.data),
```

- [ ] **Step 3: Create search page**

Create `frontend/src/views/Search.vue`:

```vue
<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { publicApi } from '../api'
import type { GlobalSearchResponse } from '../api/types'
import SearchBar from '../components/SearchBar.vue'
import Skeleton from '../components/Skeleton.vue'
import StateBlock from '../components/StateBlock.vue'

const route = useRoute()
const router = useRouter()
const q = ref(typeof route.query.q === 'string' ? route.query.q : '')
const result = ref<GlobalSearchResponse | null>(null)
const loading = ref(false)
const searched = ref(false)

const hasQuery = computed(() => q.value.trim().length > 0)
const empty = computed(() => searched.value && !loading.value && (result.value?.totalCount || 0) === 0)

function tagsOf(tags?: string) {
  return (tags || '').split(',').map(tag => tag.trim()).filter(Boolean)
}

function typeClass(type: string) {
  return `type-${type.toLowerCase().replace('_', '-')}`
}

async function syncQuery() {
  const query = q.value.trim() ? { q: q.value.trim() } : {}
  await router.replace({ query })
}

async function search() {
  await syncQuery()
  const keyword = q.value.trim()
  searched.value = true
  if (!keyword) {
    result.value = { query: '', totalCount: 0, groups: [] }
    return
  }

  loading.value = true
  try {
    result.value = await publicApi.search({ q: keyword, limit: 6 })
  } finally {
    loading.value = false
  }
}

watch(
  () => route.query.q,
  value => {
    if (typeof value === 'string' && value !== q.value) {
      q.value = value
      search()
    }
  }
)

onMounted(() => {
  if (hasQuery.value) search()
})
</script>

<template>
  <div class="container page">
    <header class="page-head">
      <p class="mono dim">// global grep</p>
      <h1 class="section-title prompt">全站搜索</h1>
      <p class="muted">一次搜索教程、Skill、MCP、公益 API 与论坛讨论。</p>
    </header>

    <div class="search-panel card">
      <SearchBar
        v-model="q"
        placeholder="输入关键词，例如 MCP、RAG、API、提示词..."
        debounce="350"
        @search="search"
      />
      <button class="btn btn-primary" :disabled="loading || !hasQuery" @click="search">搜索</button>
    </div>

    <StateBlock :loading="loading" :empty="empty" empty-text="没有匹配结果，换个关键词试试。" class="result-state">
      <template #skeleton>
        <div class="result-groups">
          <section v-for="group in 3" :key="group" class="group">
            <Skeleton block height="20px" width="140px" />
            <div class="result-grid">
              <div v-for="item in 3" :key="item" class="card result-card">
                <Skeleton block height="18px" width="70%" />
                <Skeleton block height="14px" />
                <Skeleton block height="14px" width="55%" />
              </div>
            </div>
          </section>
        </div>
      </template>

      <div v-if="!searched && !loading" class="empty-start card">
        <span class="mono">grep -R "keyword" ./ai-info-station</span>
      </div>

      <div v-else-if="result?.groups.length" class="result-groups">
        <div class="summary mono">{{ result.totalCount }} results for "{{ result.query }}"</div>
        <section v-for="group in result.groups" :key="group.type" class="group">
          <div class="group-head">
            <h2>{{ group.label }}</h2>
            <span class="badge badge-unknown">{{ group.items.length }}</span>
          </div>

          <div class="result-grid">
            <RouterLink
              v-for="(item, index) in group.items"
              :key="`${item.type}-${item.id}`"
              :to="item.url"
              class="card result-card rise"
              :style="{ animationDelay: `${Math.min(index * 0.035, 0.25)}s` }"
            >
              <div class="result-top">
                <span class="type-pill mono" :class="typeClass(item.type)">{{ item.type }}</span>
                <span v-if="item.meta" class="muted mono meta">{{ item.meta }}</span>
              </div>
              <h3>{{ item.title }}</h3>
              <p class="muted desc">{{ item.description || '暂无摘要。' }}</p>
              <div class="result-foot">
                <span v-if="item.category" class="chip">{{ item.category }}</span>
                <span v-for="tag in tagsOf(item.tags).slice(0, 4)" :key="tag" class="tag">{{ tag }}</span>
              </div>
            </RouterLink>
          </div>
        </section>
      </div>
    </StateBlock>
  </div>
</template>

<style scoped>
.page { padding: 30px 0 70px; }
.page-head { margin-bottom: 16px; }
.search-panel {
  padding: 14px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
}
.result-state { margin-top: 24px; }
.empty-start {
  padding: 28px;
  color: var(--text-dim);
  text-align: center;
  overflow: hidden;
}
.result-groups { display: flex; flex-direction: column; gap: 28px; }
.summary { color: var(--text-dim); font-size: 13px; }
.group-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.group-head h2 { margin: 0; font-size: 19px; }
.result-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
}
.result-card {
  padding: 18px;
  color: var(--text);
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 210px;
}
.result-card:hover { text-decoration: none; }
.result-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}
.type-pill {
  padding: 3px 8px;
  border-radius: var(--radius-sm);
  background: var(--primary-soft);
  color: var(--primary);
  border: 1px solid var(--primary-dim);
  font-size: 11px;
}
.type-api { color: var(--info); border-color: color-mix(in srgb, var(--info) 45%, transparent); background: color-mix(in srgb, var(--info) 12%, transparent); }
.type-forum-thread { color: var(--warning); border-color: color-mix(in srgb, var(--warning) 45%, transparent); background: color-mix(in srgb, var(--warning) 12%, transparent); }
.meta {
  font-size: 11.5px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.result-card h3 { margin: 0; font-size: 16px; line-height: 1.45; }
.desc {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.65;
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.result-foot { display: flex; flex-wrap: wrap; gap: 4px; align-items: center; }
.result-foot .chip, .result-foot .tag { cursor: default; margin: 0; }
@media (max-width: 640px) {
  .search-panel { grid-template-columns: 1fr; }
  .search-panel .btn { justify-content: center; }
}
</style>
```

- [ ] **Step 4: Register route and nav item**

Modify `frontend/src/router/index.ts` by adding the route after the home route:

```ts
{ path: '/search', name: 'search', component: () => import('../views/Search.vue') },
```

Modify `frontend/src/components/NavBar.vue` by adding the link after `home`:

```ts
{ to: '/search', label: 'search' },
```

- [ ] **Step 5: Run frontend build**

Run:

```bash
cd frontend
npm run build
```

Expected: PASS.

---

### Task 3: Documentation and Full Verification

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Document the endpoint**

Modify the API table in `README.md` by adding:

```markdown
| GET  | `/api/search?q=&limit=` | 全站搜索：教程、Skill、MCP、API 站点、论坛帖子 |
```

- [ ] **Step 2: Run focused backend tests**

Run:

```bash
cd backend
mvn -q -Dtest=GlobalSearchServiceTest test
```

Expected: PASS.

- [ ] **Step 3: Compile backend**

Run:

```bash
cd backend
mvn -q -DskipTests compile
```

Expected: PASS.

- [ ] **Step 4: Build frontend**

Run:

```bash
cd frontend
npm run build
```

Expected: PASS.

- [ ] **Step 5: Final review**

Run:

```bash
git diff -- backend/src/main/java/com/aiblog frontend/src README.md docs/superpowers/plans/2026-06-01-global-search.md
```

Expected: Diff only contains the global search feature and the plan file. Existing unrelated dirty files must not be reverted.

---

## Self-Review

Spec coverage:
- Project gap analysis is captured by selecting a global search feature that connects existing content silos.
- Backend search API is covered in Task 1.
- Frontend searchable route and navigation are covered in Task 2.
- Documentation and verification are covered in Task 3.

Placeholder scan:
- No `TBD`, `TODO`, `implement later`, or unspecified test steps remain.

Type consistency:
- Backend DTO names are `GlobalSearchItemResponse`, `GlobalSearchGroupResponse`, and `GlobalSearchResponse`.
- Frontend types are `GlobalSearchItem`, `GlobalSearchGroup`, and `GlobalSearchResponse`.
- API method is `publicApi.search`.
