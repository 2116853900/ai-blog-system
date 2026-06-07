package com.aiblog.controller;

import com.aiblog.cache.CacheProperties;
import com.aiblog.cache.HybridCacheService;
import com.aiblog.cache.PublicContentCacheService;
import com.aiblog.dto.ResourceTagSummaryResponse;
import com.aiblog.repository.ApiStationRepository;
import com.aiblog.repository.McpRepository;
import com.aiblog.repository.SkillRepository;
import com.aiblog.service.ApiStationStatusHistoryService;
import com.aiblog.service.ResourceTagService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceTagControllerTest {

    @Test
    void skillPopularTagsForwardsLimit() {
        ResourceTagService tagService = mock(ResourceTagService.class);
        List<ResourceTagSummaryResponse> tags = List.of(new ResourceTagSummaryResponse("Prompt", 3));
        when(tagService.skillPopularTags(12)).thenReturn(tags);
        SkillController controller = new SkillController(mock(SkillRepository.class), cacheService(), tagService);

        List<ResourceTagSummaryResponse> response = controller.popularTags(12);

        assertThat(response).isSameAs(tags);
        verify(tagService).skillPopularTags(12);
    }

    @Test
    void mcpPopularTagsForwardsLimit() {
        ResourceTagService tagService = mock(ResourceTagService.class);
        List<ResourceTagSummaryResponse> tags = List.of(new ResourceTagSummaryResponse("server", 2));
        when(tagService.mcpPopularTags(8)).thenReturn(tags);
        McpController controller = new McpController(mock(McpRepository.class), cacheService(), tagService);

        List<ResourceTagSummaryResponse> response = controller.popularTags(8);

        assertThat(response).isSameAs(tags);
        verify(tagService).mcpPopularTags(8);
    }

    @Test
    void apiStationPopularTagsForwardsLimit() {
        ResourceTagService tagService = mock(ResourceTagService.class);
        List<ResourceTagSummaryResponse> tags = List.of(new ResourceTagSummaryResponse("api", 4));
        when(tagService.apiStationPopularTags(6)).thenReturn(tags);
        ApiStationController controller = new ApiStationController(
                mock(ApiStationRepository.class),
                mock(ApiStationStatusHistoryService.class),
                cacheService(),
                tagService
        );

        List<ResourceTagSummaryResponse> response = controller.popularTags(6);

        assertThat(response).isSameAs(tags);
        verify(tagService).apiStationPopularTags(6);
    }

    private PublicContentCacheService cacheService() {
        CacheProperties properties = new CacheProperties();
        properties.setKeyPrefix("resource-tag-controller-test");
        properties.setRedisEnabled(false);
        return new PublicContentCacheService(new HybridCacheService(properties, new ObjectMapper()), properties);
    }
}
