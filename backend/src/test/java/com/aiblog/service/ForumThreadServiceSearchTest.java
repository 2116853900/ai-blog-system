package com.aiblog.service;

import com.aiblog.dto.ForumTagSummaryResponse;
import com.aiblog.entity.ForumReply;
import com.aiblog.entity.ForumThread;
import com.aiblog.repository.AdminOperationLogRepository;
import com.aiblog.repository.ForumCategoryRepository;
import com.aiblog.repository.ForumReplyRepository;
import com.aiblog.repository.ForumThreadRepository;
import com.aiblog.repository.ForumUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ForumThreadServiceSearchTest {

    @Test
    void searchWithTagUsesSpecificationSearch() {
        ForumThreadRepository threadRepo = mock(ForumThreadRepository.class);
        ForumThreadService service = newService(threadRepo);
        PageRequest pageable = PageRequest.of(0, 20);
        Page<ForumThread> page = new PageImpl<>(List.of(), pageable, 0);
        when(threadRepo.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<ForumThread> result = service.search(null, "", "mcp", pageable);

        assertThat(result).isSameAs(page);
        verify(threadRepo).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void legacySearchDelegatesToSpecificationSearchWithoutTag() {
        ForumThreadRepository threadRepo = mock(ForumThreadRepository.class);
        ForumThreadService service = newService(threadRepo);
        PageRequest pageable = PageRequest.of(0, 20);
        Page<ForumThread> page = new PageImpl<>(List.of(), pageable, 0);
        when(threadRepo.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<ForumThread> result = service.search(2L, "prompt", pageable);

        assertThat(result).isSameAs(page);
        verify(threadRepo).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchWithUnansweredUsesSpecificationSearch() {
        ForumThreadRepository threadRepo = mock(ForumThreadRepository.class);
        ForumThreadService service = newService(threadRepo);
        PageRequest pageable = PageRequest.of(0, 20);
        Page<ForumThread> page = new PageImpl<>(List.of(), pageable, 0);
        when(threadRepo.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<ForumThread> result = service.search(null, null, null, true, pageable);

        assertThat(result).isSameAs(page);
        verify(threadRepo).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void popularTagsCountsVisibleThreadTagsCaseInsensitively() {
        ForumThreadRepository threadRepo = mock(ForumThreadRepository.class);
        ForumThreadService service = newService(threadRepo);
        when(threadRepo.findTagTextsByStatusIn(anyCollection())).thenReturn(List.of(
                "MCP, Prompt, AI",
                "mcp,Prompt",
                "API, , prompt"
        ));

        List<ForumTagSummaryResponse> result = service.popularTags(3);

        assertThat(result).extracting(ForumTagSummaryResponse::tag).containsExactly("Prompt", "MCP", "AI");
        assertThat(result).extracting(ForumTagSummaryResponse::count).containsExactly(3L, 2L, 1L);
        verify(threadRepo).findTagTextsByStatusIn(anyCollection());
    }

    @Test
    void popularTagsCapsRequestedLimit() {
        ForumThreadRepository threadRepo = mock(ForumThreadRepository.class);
        ForumThreadService service = newService(threadRepo);
        when(threadRepo.findTagTextsByStatusIn(anyCollection())).thenReturn(List.of("a,b,c,d,e,f"));

        List<ForumTagSummaryResponse> result = service.popularTags(2);

        assertThat(result).hasSize(2);
    }

    @Test
    void likeContainsPatternEscapesSqlWildcards() {
        String result = ForumThreadService.likeContainsPattern("50%_\\mcp");

        assertThat(result).isEqualTo("%50\\%\\_\\\\mcp%");
    }

    @Test
    void normalizeTagKeepsLiteralWildcardCharacters() {
        String result = ForumThreadService.normalizeTag(" AI_% Tag ");

        assertThat(result).isEqualTo("ai_%tag");
    }

    @Test
    void acceptReplyStoresSolutionWhenOwnerSelectsVisibleReplyInSameThread() {
        ForumThreadRepository threadRepo = mock(ForumThreadRepository.class);
        ForumReplyRepository replyRepo = mock(ForumReplyRepository.class);
        ForumThreadService service = newService(threadRepo, replyRepo);
        ForumThread thread = thread(10L, 20L);
        ForumReply reply = reply(99L, 10L, 30L, ForumReply.ReplyStatus.NORMAL);
        when(threadRepo.findById(10L)).thenReturn(Optional.of(thread));
        when(replyRepo.findById(99L)).thenReturn(Optional.of(reply));
        when(threadRepo.save(any(ForumThread.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<ForumThread> result = service.acceptReply(10L, 99L, 20L, false);

        assertThat(result).isPresent();
        assertThat(result.get().getAcceptedReplyId()).isEqualTo(99L);
        assertThat(result.get().getAcceptedReplyUserId()).isEqualTo(30L);
        assertThat(result.get().getAcceptedAt()).isNotNull();
        verify(threadRepo).save(thread);
    }

    @Test
    void acceptReplyRejectsReplyFromAnotherThread() {
        ForumThreadRepository threadRepo = mock(ForumThreadRepository.class);
        ForumReplyRepository replyRepo = mock(ForumReplyRepository.class);
        ForumThreadService service = newService(threadRepo, replyRepo);
        when(threadRepo.findById(10L)).thenReturn(Optional.of(thread(10L, 20L)));
        when(replyRepo.findById(99L)).thenReturn(Optional.of(reply(99L, 11L, 30L, ForumReply.ReplyStatus.NORMAL)));

        Optional<ForumThread> result = service.acceptReply(10L, 99L, 20L, false);

        assertThat(result).isEmpty();
        verify(threadRepo, never()).save(any());
    }

    @Test
    void acceptReplyRejectsHiddenReply() {
        ForumThreadRepository threadRepo = mock(ForumThreadRepository.class);
        ForumReplyRepository replyRepo = mock(ForumReplyRepository.class);
        ForumThreadService service = newService(threadRepo, replyRepo);
        when(threadRepo.findById(10L)).thenReturn(Optional.of(thread(10L, 20L)));
        when(replyRepo.findById(99L)).thenReturn(Optional.of(reply(99L, 10L, 30L, ForumReply.ReplyStatus.HIDDEN)));

        Optional<ForumThread> result = service.acceptReply(10L, 99L, 20L, false);

        assertThat(result).isEmpty();
        verify(threadRepo, never()).save(any());
    }

    @Test
    void clearAcceptedReplyRemovesSolutionWhenModeratorActs() {
        ForumThreadRepository threadRepo = mock(ForumThreadRepository.class);
        ForumReplyRepository replyRepo = mock(ForumReplyRepository.class);
        ForumThreadService service = newService(threadRepo, replyRepo);
        ForumThread thread = thread(10L, 20L);
        thread.setAcceptedReplyId(99L);
        thread.setAcceptedReplyUserId(30L);
        thread.setAcceptedAt(java.time.Instant.parse("2026-06-07T00:00:00Z"));
        when(threadRepo.findById(10L)).thenReturn(Optional.of(thread));
        when(threadRepo.save(any(ForumThread.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<ForumThread> result = service.clearAcceptedReply(10L, 999L, true);

        assertThat(result).isPresent();
        assertThat(result.get().getAcceptedReplyId()).isNull();
        assertThat(result.get().getAcceptedReplyUserId()).isNull();
        assertThat(result.get().getAcceptedAt()).isNull();
        verify(threadRepo).save(thread);
    }

    private ForumThreadService newService(ForumThreadRepository threadRepo) {
        return newService(threadRepo, mock(ForumReplyRepository.class));
    }

    private ForumThreadService newService(ForumThreadRepository threadRepo, ForumReplyRepository replyRepo) {
        return new ForumThreadService(
                threadRepo,
                replyRepo,
                mock(ForumCategoryRepository.class),
                mock(ForumUserRepository.class),
                mock(AdminOperationLogRepository.class),
                mock(ForumViewCountBuffer.class)
        );
    }

    private ForumThread thread(Long id, Long authorId) {
        ForumThread thread = new ForumThread();
        thread.setId(id);
        thread.setCategoryId(1L);
        thread.setAuthorId(authorId);
        thread.setTitle("Thread");
        thread.setContentMarkdown("Content");
        return thread;
    }

    private ForumReply reply(Long id, Long threadId, Long authorId, ForumReply.ReplyStatus status) {
        ForumReply reply = new ForumReply();
        reply.setId(id);
        reply.setThreadId(threadId);
        reply.setAuthorId(authorId);
        reply.setStatus(status);
        reply.setContentMarkdown("Reply");
        return reply;
    }
}
