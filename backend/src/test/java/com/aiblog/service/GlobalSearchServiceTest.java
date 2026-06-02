package com.aiblog.service;

import com.aiblog.cache.CacheProperties;
import com.aiblog.cache.HybridCacheService;
import com.aiblog.cache.PublicContentCacheService;
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
import com.fasterxml.jackson.databind.ObjectMapper;
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
        CacheProperties cacheProperties = new CacheProperties();
        cacheProperties.setKeyPrefix("test-search");
        cacheProperties.setRedisEnabled(false);
        PublicContentCacheService cacheService = new PublicContentCacheService(
                new HybridCacheService(cacheProperties, new ObjectMapper()),
                cacheProperties);
        service = new GlobalSearchService(postRepo, skillRepo, mcpRepo, apiRepo, threadRepo, cacheService);
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
        assertThat(response.getGroups().get(1).getItems().get(0).getUrl()).isEqualTo("/skills/2");
        assertThat(response.getGroups().get(2).getItems().get(0).getUrl()).isEqualTo("/mcps/3");
        assertThat(response.getGroups().get(3).getItems().get(0).getUrl()).isEqualTo("/api-stations/4");
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
