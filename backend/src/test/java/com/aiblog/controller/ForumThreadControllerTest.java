package com.aiblog.controller;

import com.aiblog.dto.ForumTagSummaryResponse;
import com.aiblog.entity.ForumThread;
import com.aiblog.entity.ForumUser;
import com.aiblog.service.ForumThreadService;
import com.aiblog.service.ForumUserService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ForumThreadControllerTest {

    @Test
    void listDefaultsToLatestActivitySort() {
        ForumThreadService threadService = mock(ForumThreadService.class);
        ForumUserService userService = mock(ForumUserService.class);
        ForumThreadController controller = new ForumThreadController(threadService, userService);
        PageRequest expectedPage = PageRequest.of(0, 20,
                Sort.by(Sort.Direction.DESC, "lastReplyAt").and(Sort.by(Sort.Direction.DESC, "createdAt")));
        Page<ForumThread> page = new PageImpl<>(List.of(), expectedPage, 0);
        when(threadService.listAll(expectedPage)).thenReturn(page);

        Page<ForumThread> response = controller.list(null, null, null, null, null, null, 0, 20);

        assertThat(response).isSameAs(page);
        verify(threadService).listAll(expectedPage);
    }

    @Test
    void listSupportsNewestSort() {
        ForumThreadService threadService = mock(ForumThreadService.class);
        ForumUserService userService = mock(ForumUserService.class);
        ForumThreadController controller = new ForumThreadController(threadService, userService);
        PageRequest expectedPage = PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ForumThread> page = new PageImpl<>(List.of(), expectedPage, 0);
        when(threadService.listAll(expectedPage)).thenReturn(page);

        Page<ForumThread> response = controller.list(null, null, null, null, null, "newest", 1, 10);

        assertThat(response).isSameAs(page);
        verify(threadService).listAll(expectedPage);
    }

    @Test
    void listSupportsPopularSortWithStableTieBreakers() {
        ForumThreadService threadService = mock(ForumThreadService.class);
        ForumUserService userService = mock(ForumUserService.class);
        ForumThreadController controller = new ForumThreadController(threadService, userService);
        Sort expectedSort = Sort.by(Sort.Direction.DESC, "replyCount")
                .and(Sort.by(Sort.Direction.DESC, "viewCount"))
                .and(Sort.by(Sort.Direction.DESC, "likeCount"))
                .and(Sort.by(Sort.Direction.DESC, "createdAt"));
        PageRequest expectedPage = PageRequest.of(0, 20, expectedSort);
        Page<ForumThread> page = new PageImpl<>(List.of(), expectedPage, 0);
        when(threadService.search(3L, "mcp", null, null, null, expectedPage)).thenReturn(page);

        Page<ForumThread> response = controller.list(3L, "mcp", null, null, null, "popular", 0, 20);

        assertThat(response).isSameAs(page);
        verify(threadService).search(3L, "mcp", null, null, null, expectedPage);
    }

    @Test
    void listFallsBackToLatestSortForUnknownSortKey() {
        ForumThreadService threadService = mock(ForumThreadService.class);
        ForumUserService userService = mock(ForumUserService.class);
        ForumThreadController controller = new ForumThreadController(threadService, userService);
        PageRequest expectedPage = PageRequest.of(0, 20,
                Sort.by(Sort.Direction.DESC, "lastReplyAt").and(Sort.by(Sort.Direction.DESC, "createdAt")));
        Page<ForumThread> page = new PageImpl<>(List.of(), expectedPage, 0);
        when(threadService.listAll(expectedPage)).thenReturn(page);

        Page<ForumThread> response = controller.list(null, null, null, null, null, "bad-input", 0, 20);

        assertThat(response).isSameAs(page);
        verify(threadService).listAll(expectedPage);
    }

    @Test
    void listForwardsTagFilterWithSortAndCategory() {
        ForumThreadService threadService = mock(ForumThreadService.class);
        ForumUserService userService = mock(ForumUserService.class);
        ForumThreadController controller = new ForumThreadController(threadService, userService);
        PageRequest expectedPage = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ForumThread> page = new PageImpl<>(List.of(), expectedPage, 0);
        when(threadService.search(3L, "", "mcp", null, null, expectedPage)).thenReturn(page);

        Page<ForumThread> response = controller.list(3L, "", "mcp", null, null, "newest", 0, 20);

        assertThat(response).isSameAs(page);
        verify(threadService).search(3L, "", "mcp", null, null, expectedPage);
    }

    @Test
    void listForwardsUnansweredFilterWithSort() {
        ForumThreadService threadService = mock(ForumThreadService.class);
        ForumUserService userService = mock(ForumUserService.class);
        ForumThreadController controller = new ForumThreadController(threadService, userService);
        PageRequest expectedPage = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ForumThread> page = new PageImpl<>(List.of(), expectedPage, 0);
        when(threadService.search(null, null, null, true, null, expectedPage)).thenReturn(page);

        Page<ForumThread> response = controller.list(null, null, null, true, null, "newest", 0, 20);

        assertThat(response).isSameAs(page);
        verify(threadService).search(null, null, null, true, null, expectedPage);
    }

    @Test
    void listForwardsSolvedFilterWithSort() {
        ForumThreadService threadService = mock(ForumThreadService.class);
        ForumUserService userService = mock(ForumUserService.class);
        ForumThreadController controller = new ForumThreadController(threadService, userService);
        PageRequest expectedPage = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ForumThread> page = new PageImpl<>(List.of(), expectedPage, 0);
        when(threadService.search(null, null, null, null, true, expectedPage)).thenReturn(page);

        Page<ForumThread> response = controller.list(null, null, null, null, true, "newest", 0, 20);

        assertThat(response).isSameAs(page);
        verify(threadService).search(null, null, null, null, true, expectedPage);
    }

    @Test
    void listForwardsUnsolvedFilterWithSort() {
        ForumThreadService threadService = mock(ForumThreadService.class);
        ForumUserService userService = mock(ForumUserService.class);
        ForumThreadController controller = new ForumThreadController(threadService, userService);
        PageRequest expectedPage = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ForumThread> page = new PageImpl<>(List.of(), expectedPage, 0);
        when(threadService.search(null, null, null, null, false, expectedPage)).thenReturn(page);

        Page<ForumThread> response = controller.list(null, null, null, null, false, "newest", 0, 20);

        assertThat(response).isSameAs(page);
        verify(threadService).search(null, null, null, null, false, expectedPage);
    }

    @Test
    void popularTagsForwardsLimit() {
        ForumThreadService threadService = mock(ForumThreadService.class);
        ForumUserService userService = mock(ForumUserService.class);
        ForumThreadController controller = new ForumThreadController(threadService, userService);
        List<ForumTagSummaryResponse> tags = List.of(new ForumTagSummaryResponse("MCP", 4));
        when(threadService.popularTags(12)).thenReturn(tags);

        List<ForumTagSummaryResponse> response = controller.popularTags(12);

        assertThat(response).isSameAs(tags);
        verify(threadService).popularTags(12);
    }

    @Test
    void acceptReplyRequiresLogin() {
        ForumThreadService threadService = mock(ForumThreadService.class);
        ForumUserService userService = mock(ForumUserService.class);
        ForumThreadController controller = new ForumThreadController(threadService, userService);

        var response = controller.acceptReply(10L, Map.of("replyId", 99L), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void acceptReplyRequiresReplyId() {
        ForumThreadService threadService = mock(ForumThreadService.class);
        ForumUserService userService = mock(ForumUserService.class);
        ForumThreadController controller = new ForumThreadController(threadService, userService);
        when(userService.findByUsername("alice")).thenReturn(Optional.of(user(20L, "alice")));

        var response = controller.acceptReply(10L, Map.<String, Long>of(), auth("alice"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void acceptReplyForwardsAuthenticatedUser() {
        ForumThreadService threadService = mock(ForumThreadService.class);
        ForumUserService userService = mock(ForumUserService.class);
        ForumThreadController controller = new ForumThreadController(threadService, userService);
        ForumThread thread = new ForumThread();
        when(userService.findByUsername("alice")).thenReturn(Optional.of(user(20L, "alice")));
        when(threadService.acceptReply(10L, 99L, 20L, false)).thenReturn(Optional.of(thread));

        var response = controller.acceptReply(10L, Map.of("replyId", 99L), auth("alice"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(thread);
        verify(threadService).acceptReply(10L, 99L, 20L, false);
    }

    @Test
    void clearAcceptedReplyForwardsModerator() {
        ForumThreadService threadService = mock(ForumThreadService.class);
        ForumUserService userService = mock(ForumUserService.class);
        ForumThreadController controller = new ForumThreadController(threadService, userService);
        ForumThread thread = new ForumThread();
        when(userService.findByUsername("mod")).thenReturn(Optional.of(user(30L, "mod")));
        when(threadService.clearAcceptedReply(10L, 30L, true)).thenReturn(Optional.of(thread));

        var response = controller.clearAcceptedReply(10L, auth("mod", "ROLE_MODERATOR"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(thread);
        verify(threadService).clearAcceptedReply(10L, 30L, true);
    }

    private TestingAuthenticationToken auth(String username, String... authorities) {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(username, "password", authorities);
        auth.setAuthenticated(true);
        return auth;
    }

    private ForumUser user(Long id, String username) {
        ForumUser user = new ForumUser();
        user.setId(id);
        user.setUsername(username);
        return user;
    }
}
