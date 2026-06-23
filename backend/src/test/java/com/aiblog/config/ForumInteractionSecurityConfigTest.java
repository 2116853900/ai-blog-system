package com.aiblog.config;

import com.aiblog.controller.ForumInteractionController;
import com.aiblog.dto.ForumInteractionResponse;
import com.aiblog.entity.ForumUser;
import com.aiblog.security.ForumMutationGuardInterceptor;
import com.aiblog.security.JwtAuthFilter;
import com.aiblog.security.JwtUtil;
import com.aiblog.security.RateLimitInterceptor;
import com.aiblog.service.ForumInteractionService;
import com.aiblog.service.ForumUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ForumInteractionController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class ForumInteractionSecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ForumInteractionService interactionService;

    @MockBean
    private ForumUserService userService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private RateLimitInterceptor rateLimitInterceptor;

    @MockBean
    private ForumMutationGuardInterceptor forumMutationGuardInterceptor;

    @BeforeEach
    void setUp() throws Exception {
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);
        when(forumMutationGuardInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    void forumLikeAcceptsJwtRoleWithRolePrefix() throws Exception {
        String token = "token-with-prefixed-role";
        ForumUser user = new ForumUser();
        user.setId(7L);
        user.setUsername("alice");
        when(jwtUtil.isValid(token)).thenReturn(true);
        when(jwtUtil.extractUsername(token)).thenReturn("alice");
        when(jwtUtil.extractRole(token)).thenReturn("ROLE_USER");
        when(userService.findByUsername("alice")).thenReturn(Optional.of(user));
        when(interactionService.like(1L, 7L)).thenReturn(new ForumInteractionResponse(true, false, false, 3, 0, 0));

        mockMvc.perform(post("/api/forum/threads/1/like")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true))
                .andExpect(jsonPath("$.likeCount").value(3));
    }
}
