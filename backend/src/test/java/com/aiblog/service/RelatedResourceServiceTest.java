package com.aiblog.service;

import com.aiblog.dto.RelatedResourceResponse;
import com.aiblog.entity.ApiStation;
import com.aiblog.entity.Mcp;
import com.aiblog.entity.Post;
import com.aiblog.entity.ResourceFavorite;
import com.aiblog.entity.Skill;
import com.aiblog.repository.ApiStationRepository;
import com.aiblog.repository.McpRepository;
import com.aiblog.repository.PostRepository;
import com.aiblog.repository.SkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatedResourceServiceTest {

    @Mock private PostRepository postRepo;
    @Mock private SkillRepository skillRepo;
    @Mock private McpRepository mcpRepo;
    @Mock private ApiStationRepository apiRepo;

    private RelatedResourceService service;

    @BeforeEach
    void setUp() {
        service = new RelatedResourceService(postRepo, skillRepo, mcpRepo, apiRepo);
    }

    @Test
    void missingSourceReturnsEmptyWithoutLoadingCandidates() {
        when(postRepo.findById(99L)).thenReturn(Optional.empty());

        Optional<List<RelatedResourceResponse>> result = service.related(ResourceFavorite.RefType.POST, 99L, 6);

        assertThat(result).isEmpty();
        verify(postRepo, never()).findByPublishedTrueOrderByCreatedAtDesc();
        verify(skillRepo, never()).findAll();
        verify(mcpRepo, never()).findAll();
        verify(apiRepo, never()).findAll();
    }

    @Test
    void ranksSharedTagsAndExcludesCurrentResource() {
        Post source = post(1L, "RAG MCP 入门", "rag-mcp", "构建检索增强 MCP 应用", "RAG,MCP", "进阶", true);
        Post sibling = post(2L, "RAG 实战教程", "rag-practice", "检索增强生成案例", "RAG,教程", "进阶", true);
        Post draft = post(3L, "未发布教程", "draft", "不可见", "RAG,MCP", "进阶", false);
        Skill skill = skill(10L, "RAG 工作流", "知识库检索增强生成", "RAG,知识库", "进阶", 5);
        Mcp mcp = mcp(11L, "filesystem MCP", "文件系统上下文工具", "MCP,工具", "开发", 4);
        ApiStation api = api(12L, "公益中转", "模型 API 中转", "API,代理", ApiStation.Status.UP);

        when(postRepo.findById(1L)).thenReturn(Optional.of(source));
        when(postRepo.findByPublishedTrueOrderByCreatedAtDesc()).thenReturn(List.of(source, sibling));
        when(skillRepo.findAll()).thenReturn(List.of(skill));
        when(mcpRepo.findAll()).thenReturn(List.of(mcp));
        when(apiRepo.findAll()).thenReturn(List.of(api));

        List<RelatedResourceResponse> result = service.related(ResourceFavorite.RefType.POST, 1L, 6).orElseThrow();

        assertThat(result).extracting(RelatedResourceResponse::id).doesNotContain(1L, 3L);
        assertThat(result).extracting(RelatedResourceResponse::type)
                .containsExactly("SKILL", "POST", "MCP", "API");
        assertThat(result.get(0).reason()).isEqualTo("共同标签：RAG");
        assertThat(result.get(0).score()).isGreaterThan(result.get(3).score());
    }

    @Test
    void clampsLimitBetweenOneAndTwelve() {
        Skill source = skill(1L, "核心 Skill", "Prompt 工程", "Prompt,AI", "技巧", 5);
        List<Skill> skills = new ArrayList<>();
        skills.add(source);
        for (long i = 2; i <= 20; i++) {
            skills.add(skill(i, "Prompt Skill " + i, "提示词模板", "Prompt", "技巧", 3));
        }

        when(skillRepo.findById(1L)).thenReturn(Optional.of(source));
        when(postRepo.findByPublishedTrueOrderByCreatedAtDesc()).thenReturn(List.of());
        when(skillRepo.findAll()).thenReturn(skills);
        when(mcpRepo.findAll()).thenReturn(List.of());
        when(apiRepo.findAll()).thenReturn(List.of());

        List<RelatedResourceResponse> highLimit = service.related(ResourceFavorite.RefType.SKILL, 1L, 99).orElseThrow();
        List<RelatedResourceResponse> lowLimit = service.related(ResourceFavorite.RefType.SKILL, 1L, 0).orElseThrow();

        assertThat(highLimit).hasSize(12);
        assertThat(lowLimit).hasSize(1);
    }

    private Post post(Long id, String title, String slug, String summary, String tags, String category, boolean published) {
        Post post = new Post();
        post.setId(id);
        post.setTitle(title);
        post.setSlug(slug);
        post.setSummary(summary);
        post.setTags(tags);
        post.setCategory(category);
        post.setPublished(published);
        return post;
    }

    private Skill skill(Long id, String name, String description, String tags, String category, int level) {
        Skill skill = new Skill();
        skill.setId(id);
        skill.setName(name);
        skill.setDescription(description);
        skill.setTags(tags);
        skill.setCategory(category);
        skill.setRecommendLevel(level);
        return skill;
    }

    private Mcp mcp(Long id, String name, String description, String tags, String category, int level) {
        Mcp mcp = new Mcp();
        mcp.setId(id);
        mcp.setName(name);
        mcp.setDescription(description);
        mcp.setTags(tags);
        mcp.setCategory(category);
        mcp.setRecommendLevel(level);
        return mcp;
    }

    private ApiStation api(Long id, String name, String description, String tags, ApiStation.Status status) {
        ApiStation api = new ApiStation();
        api.setId(id);
        api.setName(name);
        api.setDescription(description);
        api.setBaseUrl("https://example.com/" + id);
        api.setTags(tags);
        api.setStatus(status);
        return api;
    }
}
