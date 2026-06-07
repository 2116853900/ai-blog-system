package com.aiblog.service;

import com.aiblog.dto.ResourceTagSummaryResponse;
import com.aiblog.entity.ApiStation;
import com.aiblog.entity.Mcp;
import com.aiblog.entity.Post;
import com.aiblog.entity.Skill;
import com.aiblog.repository.ApiStationRepository;
import com.aiblog.repository.McpRepository;
import com.aiblog.repository.PostRepository;
import com.aiblog.repository.SkillRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceTagServiceTest {

    @Test
    void popularTagsCountsCaseInsensitivelyAndPreservesFirstDisplayName() {
        List<ResourceTagSummaryResponse> result = ResourceTagService.popularTags(List.of(
                "MCP, Prompt, AI",
                "mcp, prompt",
                "API, ai"
        ), 10);

        assertThat(result).extracting(ResourceTagSummaryResponse::tag)
                .containsExactly("AI", "MCP", "Prompt", "API");
        assertThat(result).extracting(ResourceTagSummaryResponse::count)
                .containsExactly(2L, 2L, 2L, 1L);
    }

    @Test
    void popularTagsSplitsChineseCommaAndSkipsBlankTags() {
        List<ResourceTagSummaryResponse> result = ResourceTagService.popularTags(List.of(
                "工具，效率, , 编程",
                "效率，"
        ), 10);

        assertThat(result).extracting(ResourceTagSummaryResponse::tag)
                .containsExactly("效率", "工具", "编程");
        assertThat(result).extracting(ResourceTagSummaryResponse::count)
                .containsExactly(2L, 1L, 1L);
    }

    @Test
    void popularTagsCapsLimitToAtLeastOneAndAtMostFifty() {
        List<String> tags = List.of("a,b,c");

        assertThat(ResourceTagService.popularTags(tags, 0)).hasSize(1);
        assertThat(ResourceTagService.popularTags(tags, 100)).hasSize(3);
    }

    @Test
    void skillPopularTagsReadsSkillRepository() {
        SkillRepository skillRepo = mock(SkillRepository.class);
        ResourceTagService service = new ResourceTagService(mock(PostRepository.class), skillRepo, mock(McpRepository.class), mock(ApiStationRepository.class));
        Skill first = new Skill();
        first.setTags("Prompt,AI");
        Skill second = new Skill();
        second.setTags("prompt");
        when(skillRepo.findAll()).thenReturn(List.of(first, second));

        List<ResourceTagSummaryResponse> result = service.skillPopularTags(5);

        assertThat(result).extracting(ResourceTagSummaryResponse::tag).containsExactly("Prompt", "AI");
        assertThat(result).extracting(ResourceTagSummaryResponse::count).containsExactly(2L, 1L);
        verify(skillRepo).findAll();
    }

    @Test
    void mcpPopularTagsReadsMcpRepository() {
        McpRepository mcpRepo = mock(McpRepository.class);
        ResourceTagService service = new ResourceTagService(mock(PostRepository.class), mock(SkillRepository.class), mcpRepo, mock(ApiStationRepository.class));
        Mcp mcp = new Mcp();
        mcp.setTags("server,mcp");
        when(mcpRepo.findAll()).thenReturn(List.of(mcp));

        List<ResourceTagSummaryResponse> result = service.mcpPopularTags(5);

        assertThat(result).extracting(ResourceTagSummaryResponse::tag).containsExactly("mcp", "server");
        verify(mcpRepo).findAll();
    }

    @Test
    void apiStationPopularTagsReadsApiStationRepository() {
        ApiStationRepository apiStationRepo = mock(ApiStationRepository.class);
        ResourceTagService service = new ResourceTagService(mock(PostRepository.class), mock(SkillRepository.class), mock(McpRepository.class), apiStationRepo);
        ApiStation station = new ApiStation();
        station.setTags("api,proxy");
        when(apiStationRepo.findAll()).thenReturn(List.of(station));

        List<ResourceTagSummaryResponse> result = service.apiStationPopularTags(5);

        assertThat(result).extracting(ResourceTagSummaryResponse::tag).containsExactly("api", "proxy");
        verify(apiStationRepo).findAll();
    }

    @Test
    void postPopularTagsReadsPublishedPostsRepository() {
        PostRepository postRepo = mock(PostRepository.class);
        ResourceTagService service = new ResourceTagService(postRepo, mock(SkillRepository.class), mock(McpRepository.class), mock(ApiStationRepository.class));
        Post first = new Post();
        first.setTags("教程,Prompt");
        Post second = new Post();
        second.setTags("prompt");
        when(postRepo.findByPublishedTrueOrderByCreatedAtDesc()).thenReturn(List.of(first, second));

        List<ResourceTagSummaryResponse> result = service.postPopularTags(5);

        assertThat(result).extracting(ResourceTagSummaryResponse::tag).containsExactly("Prompt", "教程");
        assertThat(result).extracting(ResourceTagSummaryResponse::count).containsExactly(2L, 1L);
        verify(postRepo).findByPublishedTrueOrderByCreatedAtDesc();
    }
}
