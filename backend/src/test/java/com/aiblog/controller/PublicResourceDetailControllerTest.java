package com.aiblog.controller;

import com.aiblog.entity.ApiStation;
import com.aiblog.entity.Mcp;
import com.aiblog.entity.Skill;
import com.aiblog.repository.ApiStationRepository;
import com.aiblog.repository.McpRepository;
import com.aiblog.repository.SkillRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PublicResourceDetailControllerTest {

    @Test
    void skillDetailReturnsExistingSkill() {
        SkillRepository repo = mock(SkillRepository.class);
        Skill skill = new Skill();
        skill.setId(1L);
        skill.setName("Prompt Engineering");
        when(repo.findById(1L)).thenReturn(Optional.of(skill));
        SkillController controller = new SkillController(repo);

        var response = controller.detail(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(skill);
    }

    @Test
    void skillDetailReturnsNotFoundWhenMissing() {
        SkillRepository repo = mock(SkillRepository.class);
        when(repo.findById(404L)).thenReturn(Optional.empty());
        SkillController controller = new SkillController(repo);

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
        McpController controller = new McpController(repo);

        var response = controller.detail(2L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(mcp);
    }

    @Test
    void mcpDetailReturnsNotFoundWhenMissing() {
        McpRepository repo = mock(McpRepository.class);
        when(repo.findById(404L)).thenReturn(Optional.empty());
        McpController controller = new McpController(repo);

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
        ApiStationController controller = new ApiStationController(repo);

        var response = controller.detail(3L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(station);
    }

    @Test
    void apiStationDetailReturnsNotFoundWhenMissing() {
        ApiStationRepository repo = mock(ApiStationRepository.class);
        when(repo.findById(404L)).thenReturn(Optional.empty());
        ApiStationController controller = new ApiStationController(repo);

        var response = controller.detail(404L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
