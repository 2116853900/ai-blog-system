package com.aiblog.config;

import com.aiblog.controller.SearchController;
import com.aiblog.dto.GlobalSearchResponse;
import com.aiblog.security.ForumMutationGuardInterceptor;
import com.aiblog.security.JwtAuthFilter;
import com.aiblog.security.JwtUtil;
import com.aiblog.security.RateLimitInterceptor;
import com.aiblog.service.GlobalSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class SearchSecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GlobalSearchService searchService;

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
    void searchEndpointIsPublicForAnonymousUsers() throws Exception {
        when(searchService.search("mcp", 5)).thenReturn(new GlobalSearchResponse("mcp", List.of()));

        mockMvc.perform(get("/api/search").param("q", "mcp").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("mcp"))
                .andExpect(jsonPath("$.totalCount").value(0));
    }
}
