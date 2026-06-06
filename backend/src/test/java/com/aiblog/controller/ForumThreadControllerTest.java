package com.aiblog.controller;

import com.aiblog.entity.ForumThread;
import com.aiblog.service.ForumThreadService;
import com.aiblog.service.ForumUserService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

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

        Page<ForumThread> response = controller.list(null, null, null, null, 0, 20);

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

        Page<ForumThread> response = controller.list(null, null, null, "newest", 1, 10);

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
        when(threadService.search(3L, "mcp", expectedPage)).thenReturn(page);

        Page<ForumThread> response = controller.list(3L, "mcp", null, "popular", 0, 20);

        assertThat(response).isSameAs(page);
        verify(threadService).search(3L, "mcp", expectedPage);
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

        Page<ForumThread> response = controller.list(null, null, null, "bad-input", 0, 20);

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
        when(threadService.search(3L, "", "mcp", expectedPage)).thenReturn(page);

        Page<ForumThread> response = controller.list(3L, "", "mcp", "newest", 0, 20);

        assertThat(response).isSameAs(page);
        verify(threadService).search(3L, "", "mcp", expectedPage);
    }
}
