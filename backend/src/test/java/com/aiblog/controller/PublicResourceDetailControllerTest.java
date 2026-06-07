package com.aiblog.controller;

import com.aiblog.cache.CacheProperties;
import com.aiblog.cache.HybridCacheService;
import com.aiblog.cache.PublicContentCacheService;
import com.aiblog.dto.ApiStationStatusCheckResponse;
import com.aiblog.entity.ApiStation;
import com.aiblog.entity.Mcp;
import com.aiblog.entity.Skill;
import com.aiblog.repository.ApiStationRepository;
import com.aiblog.repository.McpRepository;
import com.aiblog.repository.SkillRepository;
import com.aiblog.service.ApiStationStatusHistoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicResourceDetailControllerTest {

    @Test
    void skillDetailReturnsExistingSkill() {
        SkillRepository repo = mock(SkillRepository.class);
        Skill skill = new Skill();
        skill.setId(1L);
        skill.setName("Prompt Engineering");
        when(repo.findById(1L)).thenReturn(Optional.of(skill));
        SkillController controller = new SkillController(repo, cacheService());

        var response = controller.detail(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(skill);
    }

    @Test
    void skillDetailReturnsNotFoundWhenMissing() {
        SkillRepository repo = mock(SkillRepository.class);
        when(repo.findById(404L)).thenReturn(Optional.empty());
        SkillController controller = new SkillController(repo, cacheService());

        var response = controller.detail(404L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void mcpDetailReturnsExistingMcp() {
        McpRepository repo = mock(McpRepository.class);
        Mcp mcp = new Mcp();
        mcp.setId(2L);
        mcp.setName("filesystem");
        when(repo.findById(2L)).thenReturn(Optional.of(mcp));
        McpController controller = new McpController(repo, cacheService());

        var response = controller.detail(2L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(mcp);
    }

    @Test
    void mcpDetailReturnsNotFoundWhenMissing() {
        McpRepository repo = mock(McpRepository.class);
        when(repo.findById(404L)).thenReturn(Optional.empty());
        McpController controller = new McpController(repo, cacheService());

        var response = controller.detail(404L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void apiStationDetailReturnsExistingStation() {
        ApiStationRepository repo = mock(ApiStationRepository.class);
        ApiStation station = new ApiStation();
        station.setId(3L);
        station.setName("OpenAI 官方");
        station.setBaseUrl("https://api.openai.com");
        when(repo.findById(3L)).thenReturn(Optional.of(station));
        ApiStationController controller = new ApiStationController(repo, mock(ApiStationStatusHistoryService.class), cacheService());

        var response = controller.detail(3L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(station);
    }

    @Test
    void apiStationDetailReturnsNotFoundWhenMissing() {
        ApiStationRepository repo = mock(ApiStationRepository.class);
        when(repo.findById(404L)).thenReturn(Optional.empty());
        ApiStationController controller = new ApiStationController(repo, mock(ApiStationStatusHistoryService.class), cacheService());

        var response = controller.detail(404L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void apiStationListForwardsStatusFilter() {
        ApiStationRepository repo = mock(ApiStationRepository.class);
        ApiStation station = new ApiStation();
        station.setId(3L);
        station.setName("Fast Relay");
        station.setBaseUrl("https://relay.example.com");
        station.setStatus(ApiStation.Status.UP);
        Sort expectedSort = Sort.by(Sort.Direction.ASC, "name");
        when(repo.findAll(any(Specification.class), eq(expectedSort))).thenReturn(List.of(station));
        ApiStationController controller = new ApiStationController(repo, mock(ApiStationStatusHistoryService.class), cacheService());

        List<ApiStation> response = controller.list(null, null, ApiStation.Status.UP);

        assertThat(response).containsExactly(station);
        verify(repo).findAll(any(Specification.class), eq(expectedSort));
    }

    @Test
    void apiStationChecksReturnsRecentHistory() {
        ApiStationRepository repo = mock(ApiStationRepository.class);
        ApiStationStatusHistoryService historyService = mock(ApiStationStatusHistoryService.class);
        ApiStationStatusCheckResponse check = new ApiStationStatusCheckResponse(
                9L,
                3L,
                ApiStation.Status.UP,
                120,
                Instant.parse("2026-06-02T10:15:30Z"),
                null
        );
        when(historyService.recent(3L, 10)).thenReturn(Optional.of(List.of(check)));
        ApiStationController controller = new ApiStationController(repo, historyService, cacheService());

        var response = controller.checks(3L, 10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(check);
    }

    @Test
    void apiStationChecksReturnsNotFoundWhenStationMissing() {
        ApiStationRepository repo = mock(ApiStationRepository.class);
        ApiStationStatusHistoryService historyService = mock(ApiStationStatusHistoryService.class);
        when(historyService.recent(404L, 20)).thenReturn(Optional.empty());
        ApiStationController controller = new ApiStationController(repo, historyService, cacheService());

        var response = controller.checks(404L, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private PublicContentCacheService cacheService() {
        CacheProperties properties = new CacheProperties();
        properties.setKeyPrefix("public-resource-test");
        properties.setRedisEnabled(false);
        return new PublicContentCacheService(new HybridCacheService(properties, new ObjectMapper()), properties);
    }
}
