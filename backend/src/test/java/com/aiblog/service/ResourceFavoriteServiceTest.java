package com.aiblog.service;

import com.aiblog.dto.ResourceFavoriteInteractionResponse;
import com.aiblog.dto.ResourceFavoriteItemResponse;
import com.aiblog.entity.ApiStation;
import com.aiblog.entity.Mcp;
import com.aiblog.entity.Post;
import com.aiblog.entity.ResourceFavorite;
import com.aiblog.entity.Skill;
import com.aiblog.repository.ApiStationRepository;
import com.aiblog.repository.McpRepository;
import com.aiblog.repository.PostRepository;
import com.aiblog.repository.ResourceFavoriteRepository;
import com.aiblog.repository.SkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.aiblog.entity.ResourceFavorite.RefType.API;
import static com.aiblog.entity.ResourceFavorite.RefType.MCP;
import static com.aiblog.entity.ResourceFavorite.RefType.POST;
import static com.aiblog.entity.ResourceFavorite.RefType.SKILL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceFavoriteServiceTest {

    private static final long USER_ID = 7L;
    private static final long POST_ID = 10L;
    private static final long SKILL_ID = 11L;
    private static final long MCP_ID = 12L;
    private static final long API_ID = 13L;

    @Mock
    private ResourceFavoriteRepository favoriteRepo;

    @Mock
    private PostRepository postRepo;

    @Mock
    private SkillRepository skillRepo;

    @Mock
    private McpRepository mcpRepo;

    @Mock
    private ApiStationRepository apiRepo;

    private ResourceFavoriteService service;

    @BeforeEach
    void setUp() {
        service = new ResourceFavoriteService(favoriteRepo, postRepo, skillRepo, mcpRepo, apiRepo);
    }

    @Test
    void postFavoriteCreatesOneRecordForPublishedTutorial() {
        when(postRepo.findById(POST_ID)).thenReturn(Optional.of(post(true)));
        when(favoriteRepo.existsByUserIdAndRefTypeAndRefId(USER_ID, POST, POST_ID)).thenReturn(true);
        when(favoriteRepo.countByRefTypeAndRefId(POST, POST_ID)).thenReturn(1L);

        ResourceFavoriteInteractionResponse response = service.favorite(POST, POST_ID, USER_ID);

        assertThat(response.isFavorited()).isTrue();
        assertThat(response.getFavoriteCount()).isEqualTo(1);
        verify(favoriteRepo).insertIgnore(USER_ID, POST.name(), POST_ID);
        verify(favoriteRepo, never()).save(any(ResourceFavorite.class));
    }

    @Test
    void postFavoriteRejectsUnpublishedTutorial() {
        when(postRepo.findById(POST_ID)).thenReturn(Optional.of(post(false)));

        assertThatThrownBy(() -> service.favorite(POST, POST_ID, USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("资源不存在");

        verifyNoInteractions(favoriteRepo);
    }

    @Test
    void favoriteCreatesOneRecordAndReturnsUpdatedCount() {
        when(skillRepo.existsById(SKILL_ID)).thenReturn(true);
        when(favoriteRepo.existsByUserIdAndRefTypeAndRefId(USER_ID, SKILL, SKILL_ID)).thenReturn(true);
        when(favoriteRepo.countByRefTypeAndRefId(SKILL, SKILL_ID)).thenReturn(1L);

        ResourceFavoriteInteractionResponse response = service.favorite(SKILL, SKILL_ID, USER_ID);

        assertThat(response.isFavorited()).isTrue();
        assertThat(response.getFavoriteCount()).isEqualTo(1);
        verify(favoriteRepo).insertIgnore(USER_ID, SKILL.name(), SKILL_ID);
        verify(favoriteRepo, never()).save(any(ResourceFavorite.class));
    }

    @Test
    void favoriteIsIdempotent() {
        when(mcpRepo.existsById(MCP_ID)).thenReturn(true);
        when(favoriteRepo.existsByUserIdAndRefTypeAndRefId(USER_ID, MCP, MCP_ID)).thenReturn(true);
        when(favoriteRepo.countByRefTypeAndRefId(MCP, MCP_ID)).thenReturn(1L);

        ResourceFavoriteInteractionResponse response = service.favorite(MCP, MCP_ID, USER_ID);

        assertThat(response.isFavorited()).isTrue();
        assertThat(response.getFavoriteCount()).isEqualTo(1);
        verify(favoriteRepo).insertIgnore(USER_ID, MCP.name(), MCP_ID);
        verify(favoriteRepo, never()).save(any(ResourceFavorite.class));
    }

    @Test
    void unfavoriteDeletesExistingRecord() {
        when(apiRepo.existsById(API_ID)).thenReturn(true);
        when(favoriteRepo.existsByUserIdAndRefTypeAndRefId(USER_ID, API, API_ID)).thenReturn(false);
        when(favoriteRepo.countByRefTypeAndRefId(API, API_ID)).thenReturn(0L);

        ResourceFavoriteInteractionResponse response = service.unfavorite(API, API_ID, USER_ID);

        assertThat(response.isFavorited()).isFalse();
        assertThat(response.getFavoriteCount()).isZero();
        verify(favoriteRepo).deleteByUserIdAndRefTypeAndRefId(USER_ID, API, API_ID);
        verify(favoriteRepo, never()).delete(any(ResourceFavorite.class));
    }

    @Test
    void getInteractionRejectsMissingResource() {
        when(skillRepo.existsById(SKILL_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.getInteraction(SKILL, SKILL_ID, USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("资源不存在");

        verifyNoInteractions(favoriteRepo);
    }

    @Test
    void listFavoritesMapsPostSkillMcpAndApiItems() {
        PageRequest pageable = PageRequest.of(0, 10);
        ResourceFavorite postFavorite = favorite(POST, POST_ID);
        ResourceFavorite skillFavorite = favorite(SKILL, SKILL_ID);
        ResourceFavorite mcpFavorite = favorite(MCP, MCP_ID);
        ResourceFavorite apiFavorite = favorite(API, API_ID);
        when(favoriteRepo.findByUserIdOrderByCreatedAtDesc(USER_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(postFavorite, skillFavorite, mcpFavorite, apiFavorite), pageable, 4));
        when(postRepo.findById(POST_ID)).thenReturn(Optional.of(post(true)));
        when(skillRepo.findById(SKILL_ID)).thenReturn(Optional.of(skill()));
        when(mcpRepo.findById(MCP_ID)).thenReturn(Optional.of(mcp()));
        when(apiRepo.findById(API_ID)).thenReturn(Optional.of(apiStation()));

        Page<ResourceFavoriteItemResponse> result = service.listFavorites(USER_ID, pageable);

        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getContent())
                .extracting(ResourceFavoriteItemResponse::getTitle)
                .containsExactly("新手入门", "Prompt Engineering", "filesystem", "OpenAI 官方");
        assertThat(result.getContent())
                .extracting(ResourceFavoriteItemResponse::getUrl)
                .containsExactly("/tutorials/getting-started", "/skills/11", "/mcps/12", "/api-stations/13");
        assertThat(result.getContent())
                .extracting(ResourceFavoriteItemResponse::isAvailable)
                .containsExactly(true, true, true, true);
    }

    private ResourceFavorite favorite(ResourceFavorite.RefType refType, Long refId) {
        ResourceFavorite favorite = new ResourceFavorite();
        favorite.setUserId(USER_ID);
        favorite.setRefType(refType);
        favorite.setRefId(refId);
        favorite.setCreatedAt(Instant.parse("2026-06-02T00:00:00Z"));
        return favorite;
    }

    private Post post(boolean published) {
        Post post = new Post();
        post.setId(POST_ID);
        post.setTitle("新手入门");
        post.setSlug("getting-started");
        post.setSummary("快速上手 AI 助手");
        post.setCategory("入门");
        post.setTags("教程,入门");
        post.setPublished(published);
        return post;
    }

    private Skill skill() {
        Skill skill = new Skill();
        skill.setId(SKILL_ID);
        skill.setName("Prompt Engineering");
        skill.setDescription("提示词技巧");
        skill.setCategory("技巧");
        skill.setTags("Prompt,入门");
        return skill;
    }

    private Mcp mcp() {
        Mcp mcp = new Mcp();
        mcp.setId(MCP_ID);
        mcp.setName("filesystem");
        mcp.setDescription("文件系统 MCP");
        mcp.setCategory("官方");
        mcp.setTags("文件,基础");
        return mcp;
    }

    private ApiStation apiStation() {
        ApiStation station = new ApiStation();
        station.setId(API_ID);
        station.setName("OpenAI 官方");
        station.setBaseUrl("https://api.openai.com");
        station.setDescription("官方 API");
        station.setTags("官方,对照");
        return station;
    }
}
