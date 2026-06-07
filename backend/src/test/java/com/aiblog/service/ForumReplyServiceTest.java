package com.aiblog.service;

import com.aiblog.dto.ReplyRequest;
import com.aiblog.entity.ForumReply;
import com.aiblog.entity.ForumThread;
import com.aiblog.repository.AdminOperationLogRepository;
import com.aiblog.repository.ForumReplyRepository;
import com.aiblog.repository.ForumThreadRepository;
import com.aiblog.repository.ForumUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForumReplyServiceTest {

    private static final long THREAD_ID = 10L;
    private static final long AUTHOR_ID = 20L;

    @Mock
    private ForumReplyRepository replyRepo;

    @Mock
    private ForumThreadRepository threadRepo;

    @Mock
    private ForumUserRepository userRepo;

    @Mock
    private AdminOperationLogRepository operationLogRepo;

    @Mock
    private NotificationService notificationService;

    private ForumReplyService service;
    private ForumThread thread;

    @BeforeEach
    void setUp() {
        service = new ForumReplyService(replyRepo, threadRepo, userRepo, operationLogRepo, notificationService);
        thread = new ForumThread();
        thread.setId(THREAD_ID);
        thread.setCategoryId(1L);
        thread.setAuthorId(7L);
        thread.setTitle("并发回复测试");
        thread.setContentMarkdown("content");
        thread.setCreatedAt(Instant.parse("2026-06-02T00:00:00Z"));
    }

    @Test
    void createLocksThreadAndUsesNextMaxFloorNumber() {
        ReplyRequest request = request("第一条回复");
        when(threadRepo.findByIdForUpdate(THREAD_ID)).thenReturn(Optional.of(thread));
        when(replyRepo.findMaxFloorNumberByThreadId(THREAD_ID)).thenReturn(8);
        when(replyRepo.save(any(ForumReply.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<ForumReply> replyCaptor = ArgumentCaptor.forClass(ForumReply.class);

        ForumReply saved = service.create(THREAD_ID, request, AUTHOR_ID);

        verify(replyRepo).save(replyCaptor.capture());
        assertThat(replyCaptor.getValue().getFloorNumber()).isEqualTo(9);
        assertThat(replyCaptor.getValue().getThreadId()).isEqualTo(THREAD_ID);
        assertThat(replyCaptor.getValue().getAuthorId()).isEqualTo(AUTHOR_ID);
        assertThat(saved.getFloorNumber()).isEqualTo(9);
        verify(threadRepo).incrementReplyCount(any(), any(), any());
        verify(notificationService).notifyReplyCreated(thread, saved);
    }

    @Test
    void createStoresReferencedReplyAuthor() {
        ReplyRequest request = request("引用回复");
        request.setReplyToId(99L);
        ForumReply referenced = new ForumReply();
        referenced.setId(99L);
        referenced.setThreadId(THREAD_ID);
        referenced.setAuthorId(30L);
        when(threadRepo.findByIdForUpdate(THREAD_ID)).thenReturn(Optional.of(thread));
        when(replyRepo.findMaxFloorNumberByThreadId(THREAD_ID)).thenReturn(1);
        when(replyRepo.findById(99L)).thenReturn(Optional.of(referenced));
        when(replyRepo.save(any(ForumReply.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ForumReply saved = service.create(THREAD_ID, request, AUTHOR_ID);

        assertThat(saved.getReplyToUserId()).isEqualTo(30L);
    }

    @Test
    void createFailsWithoutWritingWhenThreadDoesNotExist() {
        ReplyRequest request = request("不存在帖子");
        when(threadRepo.findByIdForUpdate(THREAD_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(THREAD_ID, request, AUTHOR_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("帖子不存在");

        verify(replyRepo, never()).findMaxFloorNumberByThreadId(THREAD_ID);
        verify(replyRepo, never()).save(any());
        verify(threadRepo, never()).incrementReplyCount(any(), any(), any());
    }

    @Test
    void deleteClearsAcceptedReplyWhenDeletingSolution() {
        ForumReply reply = new ForumReply();
        reply.setId(99L);
        reply.setThreadId(THREAD_ID);
        reply.setAuthorId(AUTHOR_ID);
        reply.setStatus(ForumReply.ReplyStatus.NORMAL);
        thread.setAcceptedReplyId(99L);
        thread.setAcceptedReplyUserId(AUTHOR_ID);
        thread.setAcceptedAt(Instant.parse("2026-06-07T00:00:00Z"));
        when(replyRepo.findById(99L)).thenReturn(Optional.of(reply));
        when(replyRepo.save(any(ForumReply.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(threadRepo.findById(THREAD_ID)).thenReturn(Optional.of(thread));
        when(threadRepo.save(any(ForumThread.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean deleted = service.delete(99L, AUTHOR_ID, false);

        assertThat(deleted).isTrue();
        assertThat(thread.getAcceptedReplyId()).isNull();
        assertThat(thread.getAcceptedReplyUserId()).isNull();
        assertThat(thread.getAcceptedAt()).isNull();
        verify(threadRepo).save(thread);
    }

    private ReplyRequest request(String content) {
        ReplyRequest request = new ReplyRequest();
        request.setContentMarkdown(content);
        return request;
    }
}
