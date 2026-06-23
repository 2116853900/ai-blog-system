package com.aiblog.config;

import com.aiblog.controller.PublicStatsController;
import com.aiblog.dto.PublicStatsResponse;
import com.aiblog.security.ForumMutationGuardInterceptor;
import com.aiblog.security.JwtAuthFilter;
import com.aiblog.security.JwtUtil;
import com.aiblog.security.RateLimitInterceptor;
import com.aiblog.service.PublicStatsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicStatsController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class PublicStatsSecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PublicStatsService statsService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private RateLimitInterceptor rateLimitInterceptor;

    @MockBean
    private ForumMutationGuardInterceptor forumMutationGuardInterceptor;

    @BeforeEach
    void allowMvcInterceptors() throws Exception {
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);
        when(forumMutationGuardInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    void statsEndpointIsPublicForAnonymousUsers() throws Exception {
        when(statsService.getStats()).thenReturn(new PublicStatsResponse(
                Instant.parse("2026-06-07T00:00:00Z"),
                new PublicStatsResponse.ContentMetrics(1, 2, 3, 4, 10),
                new PublicStatsResponse.CommunityMetrics(5, 6, 1, 100, 8, 9),
                new PublicStatsResponse.ApiHealthMetrics(4, 3, 1, 0, 75.0, 128),
                List.of(),
                List.of(),
                List.of()
        ));

        mockMvc.perform(get("/api/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.totalResources").value(10))
                .andExpect(jsonPath("$.apiHealth.uptimeRate").value(75.0));
    }
}
