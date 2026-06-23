package com.aiblog.security;

import com.aiblog.entity.ForumUser;
import com.aiblog.service.ForumUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForumMutationGuardInterceptorTest {

    @Mock
    private ForumUserService userService;

    private ForumMutationGuardInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new ForumMutationGuardInterceptor(userService, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void blocksBannedAuthenticatedForumMutation() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/forum/threads");
        MockHttpServletResponse response = new MockHttpServletResponse();
        SecurityContextHolder.getContext().setAuthentication(auth("alice"));
        ForumUser user = user(7L, "alice");
        when(userService.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userService.isActiveForumUser(7L)).thenReturn(false);

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("账号已被封禁");
    }

    @Test
    void allowsActiveAuthenticatedForumMutation() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/forum/replies/9");
        MockHttpServletResponse response = new MockHttpServletResponse();
        SecurityContextHolder.getContext().setAuthentication(auth("alice"));
        ForumUser user = user(7L, "alice");
        when(userService.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userService.isActiveForumUser(7L)).thenReturn(true);

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void ignoresForumReads() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/forum/threads");
        MockHttpServletResponse response = new MockHttpServletResponse();
        SecurityContextHolder.getContext().setAuthentication(auth("alice"));

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isTrue();
        verify(userService, never()).findByUsername("alice");
    }

    @Test
    void ignoresAdminPaths() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/forum/posts/1/hide");
        MockHttpServletResponse response = new MockHttpServletResponse();
        SecurityContextHolder.getContext().setAuthentication(auth("admin"));

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isTrue();
        verify(userService, never()).findByUsername("admin");
    }

    @Test
    void allowsAnonymousCommentSubmissionToReachController() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/comments");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isTrue();
        verify(userService, never()).findByUsername("anonymousUser");
    }

    @Test
    void blocksBannedReportSubmission() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/reports");
        MockHttpServletResponse response = new MockHttpServletResponse();
        SecurityContextHolder.getContext().setAuthentication(auth("bob"));
        ForumUser user = user(8L, "bob");
        when(userService.findByUsername("bob")).thenReturn(Optional.of(user));
        when(userService.isActiveForumUser(8L)).thenReturn(false);

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void matchesPathsBehindContextPath() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/app/api/forum/threads");
        request.setContextPath("/app");
        MockHttpServletResponse response = new MockHttpServletResponse();
        SecurityContextHolder.getContext().setAuthentication(auth("alice"));
        ForumUser user = user(7L, "alice");
        when(userService.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userService.isActiveForumUser(7L)).thenReturn(false);

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
    }

    private UsernamePasswordAuthenticationToken auth(String username) {
        return new UsernamePasswordAuthenticationToken(
                username,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private ForumUser user(Long id, String username) {
        ForumUser user = new ForumUser();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPasswordHash("hash");
        user.setStatus(ForumUser.Status.ACTIVE);
        return user;
    }
}
