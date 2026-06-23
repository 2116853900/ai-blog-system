package com.aiblog.service;

import com.aiblog.dto.PublicStatsResponse;
import com.aiblog.entity.ApiStation;
import com.aiblog.entity.ForumThread;
import com.aiblog.entity.Mcp;
import com.aiblog.entity.Post;
import com.aiblog.entity.Skill;
import com.aiblog.repository.ApiStationRepository;
import com.aiblog.repository.ForumReplyRepository;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicStatsServiceTest {

    @Mock private PostRepository postRepo;
    @Mock private SkillRepository skillRepo;
    @Mock private McpRepository mcpRepo;
    @Mock private ApiStationRepository apiStationRepo;
    @Mock private ForumThreadRepository threadRepo;
    @Mock private ForumReplyRepository replyRepo;

    private PublicStatsService service;

    @BeforeEach
    void setUp() {
        service = new PublicStatsService(postRepo, skillRepo, mcpRepo, apiStationRepo, threadRepo, replyRepo);
    }

    @Test
    void buildsPublicStatsFromPublishedContentAndVisibleForumThreads() {
        Post post = post(1L, "Prompt 工程", "prompt-guide", "AI,Prompt", Instant.parse("2026-06-05T00:00:00Z"));
        Skill skill = skill(2L, "调试 Skill", "prompt,Tool", Instant.parse("2026-06-06T00:00:00Z"));
        Mcp mcp = mcp(3L, "filesystem", "MCP,Tool", Instant.parse("2026-06-04T00:00:00Z"));
        ApiStation upApi = apiStation(4L, "OpenAI Proxy", "API,AI", ApiStation.Status.UP, 120, Instant.parse("2026-06-03T00:00:00Z"));
        ApiStation downApi = apiStation(5L, "Backup Proxy", "API", ApiStation.Status.DOWN, null, Instant.parse("2026-06-02T00:00:00Z"));
        ForumThread thread = thread(6L, "如何部署 MCP", "AI，Forum", Instant.parse("2026-06-07T00:00:00Z"));

        when(postRepo.count(any(Specification.class))).thenReturn(1L);
        when(skillRepo.count()).thenReturn(1L);
        when(mcpRepo.count()).thenReturn(1L);
        when(apiStationRepo.count()).thenReturn(2L);
        when(apiStationRepo.countByStatus(ApiStation.Status.UP)).thenReturn(1L);
        when(apiStationRepo.countByStatus(ApiStation.Status.DOWN)).thenReturn(1L);
        when(apiStationRepo.countByStatus(ApiStation.Status.UNKNOWN)).thenReturn(0L);

        when(threadRepo.count(any(Specification.class))).thenReturn(1L, 1L);
        when(replyRepo.count(any(Specification.class))).thenReturn(3L);
        when(threadRepo.sumViewCountByStatusIn(any())).thenReturn(42L);
        when(threadRepo.sumLikeCountByStatusIn(any())).thenReturn(7L);
        when(threadRepo.sumFavoriteCountByStatusIn(any())).thenReturn(4L);

        when(postRepo.findByPublishedTrueOrderByCreatedAtDesc()).thenReturn(List.of(post));
        when(skillRepo.findAll()).thenReturn(List.of(skill));
        when(mcpRepo.findAll()).thenReturn(List.of(mcp));
        when(apiStationRepo.findAll()).thenReturn(List.of(upApi, downApi));
        when(threadRepo.findTagTextsByStatusIn(any())).thenReturn(List.of(thread.getTags()));

        when(postRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(post)));
        when(skillRepo.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(skill)));
        when(mcpRepo.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(mcp)));
        when(apiStationRepo.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(upApi, downApi)));
        when(threadRepo.findByStatusIn(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(thread)), new PageImpl<>(List.of(thread)));

        PublicStatsResponse stats = service.getStats();

        assertThat(stats.content().totalResources()).isEqualTo(5);
        assertThat(stats.apiHealth().uptimeRate()).isEqualTo(50.0);
        assertThat(stats.apiHealth().averageLatencyMs()).isEqualTo(120);
        assertThat(stats.community().threads()).isEqualTo(1);
        assertThat(stats.community().replies()).isEqualTo(3);
        assertThat(stats.community().solvedThreads()).isEqualTo(1);
        assertThat(stats.community().totalViews()).isEqualTo(42);
        assertThat(stats.popularTags()).extracting(PublicStatsResponse.TagMetric::tag)
                .contains("AI", "Prompt", "Tool", "API", "Forum");
        assertThat(stats.popularTags())
                .filteredOn(tag -> tag.tag().equals("AI"))
                .singleElement()
                .extracting(PublicStatsResponse.TagMetric::count)
                .isEqualTo(3L);
        assertThat(stats.recentItems()).first()
                .extracting(PublicStatsResponse.RecentItem::type, PublicStatsResponse.RecentItem::url)
                .containsExactly("FORUM_THREAD", "/forum/threads/6");
        assertThat(stats.hotThreads()).singleElement()
                .extracting(PublicStatsResponse.HotThread::solved)
                .isEqualTo(true);
    }

    private Post post(Long id, String title, String slug, String tags, Instant createdAt) {
        Post post = new Post();
        post.setId(id);
        post.setTitle(title);
        post.setSlug(slug);
        post.setSummary("summary");
        post.setTags(tags);
        post.setPublished(true);
        post.setCreatedAt(createdAt);
        return post;
    }

    private Skill skill(Long id, String name, String tags, Instant createdAt) {
        Skill skill = new Skill();
        skill.setId(id);
        skill.setName(name);
        skill.setDescription("skill description");
        skill.setTags(tags);
        skill.setRecommendLevel(5);
        skill.setCreatedAt(createdAt);
        return skill;
    }

    private Mcp mcp(Long id, String name, String tags, Instant createdAt) {
        Mcp mcp = new Mcp();
        mcp.setId(id);
        mcp.setName(name);
        mcp.setDescription("mcp description");
        mcp.setTags(tags);
        mcp.setRecommendLevel(4);
        mcp.setCreatedAt(createdAt);
        return mcp;
    }

    private ApiStation apiStation(Long id, String name, String tags, ApiStation.Status status, Integer latencyMs, Instant createdAt) {
        ApiStation apiStation = new ApiStation();
        apiStation.setId(id);
        apiStation.setName(name);
        apiStation.setBaseUrl("https://example.com/" + id);
        apiStation.setDescription("api description");
        apiStation.setTags(tags);
        apiStation.setStatus(status);
        apiStation.setLatencyMs(latencyMs);
        apiStation.setCreatedAt(createdAt);
        return apiStation;
    }

    private ForumThread thread(Long id, String title, String tags, Instant createdAt) {
        ForumThread thread = new ForumThread();
        thread.setId(id);
        thread.setCategoryId(1L);
        thread.setAuthorId(2L);
        thread.setTitle(title);
        thread.setContentMarkdown("thread body");
        thread.setTags(tags);
        thread.setStatus(ForumThread.ThreadStatus.NORMAL);
        thread.setViewCount(42);
        thread.setReplyCount(3);
        thread.setLikeCount(7);
        thread.setAcceptedReplyId(9L);
        thread.setCreatedAt(createdAt);
        return thread;
    }
}
