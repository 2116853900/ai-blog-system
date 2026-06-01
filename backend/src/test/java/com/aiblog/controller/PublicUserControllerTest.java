package com.aiblog.controller;

import com.aiblog.entity.ForumReply;
import com.aiblog.entity.ForumThread;
import com.aiblog.entity.ForumUser;
import com.aiblog.service.ForumReplyService;
import com.aiblog.service.ForumThreadService;
import com.aiblog.service.ForumUserService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PublicUserControllerTest {

    @Test
    void profileReturnsPublicUserInfo() {
        ForumUserService userService = mock(ForumUserService.class);
        ForumThreadService threadService = mock(ForumThreadService.class);
        ForumReplyService replyService = mock(ForumReplyService.class);
        ForumUser user = user(7L);
        when(userService.findById(7L)).thenReturn(Optional.of(user));
        UserController controller = new UserController(userService, threadService, replyService);

        var response = controller.getProfile(7L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(7L);
        assertThat(response.getBody().getUsername()).isEqualTo("alice");
    }

    @Test
    void profileReturnsNotFoundWhenUserMissing() {
        ForumUserService userService = mock(ForumUserService.class);
        ForumThreadService threadService = mock(ForumThreadService.class);
        ForumReplyService replyService = mock(ForumReplyService.class);
        when(userService.findById(404L)).thenReturn(Optional.empty());
        UserController controller = new UserController(userService, threadService, replyService);

        var response = controller.getProfile(404L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void threadsReturnVisibleAuthorThreadsWithNormalizedPageRequest() {
        ForumUserService userService = mock(ForumUserService.class);
        ForumThreadService threadService = mock(ForumThreadService.class);
        ForumReplyService replyService = mock(ForumReplyService.class);
        ForumUser user = user(7L);
        ForumThread thread = thread(11L);
        PageRequest expectedPage = PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ForumThread> page = new PageImpl<>(List.of(thread), expectedPage, 1);
        when(userService.findById(7L)).thenReturn(Optional.of(user));
        when(threadService.listByAuthor(7L, expectedPage)).thenReturn(page);
        UserController controller = new UserController(userService, threadService, replyService);

        var response = controller.threads(7L, -3, 99);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(page);
        verify(threadService).listByAuthor(7L, expectedPage);
    }

    @Test
    void threadsReturnNotFoundWithoutQueryingActivityWhenUserMissing() {
        ForumUserService userService = mock(ForumUserService.class);
        ForumThreadService threadService = mock(ForumThreadService.class);
        ForumReplyService replyService = mock(ForumReplyService.class);
        when(userService.findById(404L)).thenReturn(Optional.empty());
        UserController controller = new UserController(userService, threadService, replyService);

        var response = controller.threads(404L, 0, 10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verifyNoInteractions(threadService);
    }

    @Test
    void repliesReturnPublicSafeAuthorReplies() {
        ForumUserService userService = mock(ForumUserService.class);
        ForumThreadService threadService = mock(ForumThreadService.class);
        ForumReplyService replyService = mock(ForumReplyService.class);
        ForumUser user = user(7L);
        ForumReply reply = reply(21L);
        PageRequest expectedPage = PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ForumReply> page = new PageImpl<>(List.of(reply), expectedPage, 1);
        when(userService.findById(7L)).thenReturn(Optional.of(user));
        when(replyService.listVisibleByAuthor(7L, expectedPage)).thenReturn(page);
        UserController controller = new UserController(userService, threadService, replyService);

        var response = controller.replies(7L, 1, 5);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(page);
        verify(replyService).listVisibleByAuthor(7L, expectedPage);
    }

    @Test
    void repliesReturnNotFoundWithoutQueryingActivityWhenUserMissing() {
        ForumUserService userService = mock(ForumUserService.class);
        ForumThreadService threadService = mock(ForumThreadService.class);
        ForumReplyService replyService = mock(ForumReplyService.class);
        when(userService.findById(404L)).thenReturn(Optional.empty());
        UserController controller = new UserController(userService, threadService, replyService);

        var response = controller.replies(404L, 0, 10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verifyNoInteractions(replyService);
    }

    private ForumUser user(Long id) {
        ForumUser user = new ForumUser();
        user.setId(id);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setNickname("Alice");
        user.setRole(ForumUser.Role.USER);
        return user;
    }

    private ForumThread thread(Long id) {
        ForumThread thread = new ForumThread();
        thread.setId(id);
        thread.setAuthorId(7L);
        thread.setCategoryId(3L);
        thread.setTitle("MCP 调试记录");
        thread.setContentMarkdown("content");
        thread.setStatus(ForumThread.ThreadStatus.NORMAL);
        return thread;
    }

    private ForumReply reply(Long id) {
        ForumReply reply = new ForumReply();
        reply.setId(id);
        reply.setAuthorId(7L);
        reply.setThreadId(11L);
        reply.setFloorNumber(2);
        reply.setContentMarkdown("reply");
        reply.setStatus(ForumReply.ReplyStatus.NORMAL);
        return reply;
    }
}
