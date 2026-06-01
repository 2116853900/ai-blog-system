package com.aiblog.service;

import com.aiblog.dto.ContentReportRequest;
import com.aiblog.dto.ContentReportTargetResponse;
import com.aiblog.dto.ReportReviewRequest;
import com.aiblog.entity.AdminOperationLog;
import com.aiblog.entity.Comment;
import com.aiblog.entity.ContentReport;
import com.aiblog.entity.ForumThread;
import com.aiblog.repository.AdminOperationLogRepository;
import com.aiblog.repository.CommentRepository;
import com.aiblog.repository.ContentReportRepository;
import com.aiblog.repository.ForumReplyRepository;
import com.aiblog.repository.ForumThreadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentReportServiceTest {

    @Mock
    private ContentReportRepository reportRepo;

    @Mock
    private ForumThreadRepository threadRepo;

    @Mock
    private ForumReplyRepository replyRepo;

    @Mock
    private CommentRepository commentRepo;

    @Mock
    private ForumThreadService threadService;

    @Mock
    private ForumReplyService replyService;

    @Mock
    private ForumUserService userService;

    @Mock
    private AdminOperationLogRepository operationLogRepo;

    private ContentReportService service;

    @BeforeEach
    void setUp() {
        service = new ContentReportService(
                reportRepo,
                threadRepo,
                replyRepo,
                commentRepo,
                threadService,
                replyService,
                userService,
                operationLogRepo);
        lenient().when(reportRepo.save(any(ContentReport.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void submitPostReportStoresSnapshotAndIncrementsReportCount() {
        ForumThread thread = new ForumThread();
        thread.setId(11L);
        thread.setCategoryId(1L);
        thread.setAuthorId(22L);
        thread.setTitle("违规标题");
        thread.setContentMarkdown("违规内容");
        thread.setReportCount(2);
        when(threadRepo.findById(11L)).thenReturn(Optional.of(thread));

        ContentReportRequest request = new ContentReportRequest();
        request.setTargetType(ContentReport.TargetType.POST);
        request.setTargetId(11L);
        request.setReasonType(ContentReport.ReasonType.SPAM);
        request.setReasonText("  广告刷屏  ");

        ContentReport report = service.submit(request, 33L);

        assertThat(report.getTargetType()).isEqualTo(ContentReport.TargetType.POST);
        assertThat(report.getTargetId()).isEqualTo(11L);
        assertThat(report.getTargetAuthorId()).isEqualTo(22L);
        assertThat(report.getReporterId()).isEqualTo(33L);
        assertThat(report.getReasonType()).isEqualTo(ContentReport.ReasonType.SPAM);
        assertThat(report.getReasonText()).isEqualTo("广告刷屏");
        assertThat(report.getContentSnapshot()).isEqualTo("# 违规标题\n\n违规内容");
        assertThat(report.getStatus()).isEqualTo(ContentReport.ReportStatus.PENDING);
        assertThat(thread.getReportCount()).isEqualTo(3);
        verify(threadRepo).save(thread);
    }

    @Test
    void approveCanHideContentAndBanTargetAuthor() {
        ContentReport report = new ContentReport();
        report.setId(100L);
        report.setTargetType(ContentReport.TargetType.POST);
        report.setTargetId(11L);
        report.setTargetAuthorId(22L);
        when(reportRepo.findById(100L)).thenReturn(Optional.of(report));
        Instant banEndTime = Instant.parse("2026-06-08T00:00:00Z");

        ReportReviewRequest request = new ReportReviewRequest();
        request.setReviewNote("  审核成立  ");
        request.setHideContent(true);
        request.setBanTargetAuthor(true);
        request.setBanReason("  多次违规  ");
        request.setBanEndTime(banEndTime);

        Optional<ContentReport> result = service.approve(100L, request, "admin");

        assertThat(result).isPresent();
        ContentReport reviewed = result.orElseThrow();
        assertThat(reviewed.getStatus()).isEqualTo(ContentReport.ReportStatus.APPROVED);
        assertThat(reviewed.getReviewResult()).isEqualTo("APPROVED");
        assertThat(reviewed.getReviewNote()).isEqualTo("审核成立");
        assertThat(reviewed.getReviewerUsername()).isEqualTo("admin");
        assertThat(reviewed.getReviewedAt()).isNotNull();
        verify(threadService).hide(11L, "admin", "审核成立");
        verify(userService).ban(22L, "多次违规", banEndTime, "admin");
        ArgumentCaptor<AdminOperationLog> logCaptor = ArgumentCaptor.forClass(AdminOperationLog.class);
        verify(operationLogRepo).save(logCaptor.capture());
        AdminOperationLog log = logCaptor.getValue();
        assertThat(log.getOperatorUsername()).isEqualTo("admin");
        assertThat(log.getAction()).isEqualTo("APPROVE_CONTENT_REPORT");
        assertThat(log.getTargetType()).isEqualTo("CONTENT_REPORT");
        assertThat(log.getTargetId()).isEqualTo(100L);
        assertThat(log.getDetail()).isEqualTo("审核成立");
    }

    @Test
    void approveDoesNotReprocessReviewedReport() {
        Instant reviewedAt = Instant.parse("2026-06-01T08:00:00Z");
        ContentReport report = new ContentReport();
        report.setId(100L);
        report.setTargetType(ContentReport.TargetType.POST);
        report.setTargetId(11L);
        report.setTargetAuthorId(22L);
        report.setStatus(ContentReport.ReportStatus.APPROVED);
        report.setReviewResult("APPROVED");
        report.setReviewNote("首次审核");
        report.setReviewerUsername("first-admin");
        report.setReviewedAt(reviewedAt);
        when(reportRepo.findById(100L)).thenReturn(Optional.of(report));

        ReportReviewRequest request = new ReportReviewRequest();
        request.setReviewNote("重复审核");
        request.setHideContent(true);
        request.setBanTargetAuthor(true);
        request.setBanReason("重复封禁");

        Optional<ContentReport> result = service.approve(100L, request, "second-admin");

        assertThat(result).isPresent();
        ContentReport reviewed = result.orElseThrow();
        assertThat(reviewed.getStatus()).isEqualTo(ContentReport.ReportStatus.APPROVED);
        assertThat(reviewed.getReviewNote()).isEqualTo("首次审核");
        assertThat(reviewed.getReviewerUsername()).isEqualTo("first-admin");
        assertThat(reviewed.getReviewedAt()).isEqualTo(reviewedAt);
        verify(reportRepo, never()).save(any(ContentReport.class));
        verify(threadService, never()).hide(any(), any(), any());
        verify(userService, never()).ban(any(), any(), any(), any());
        verify(operationLogRepo, never()).save(any(AdminOperationLog.class));
    }

    @Test
    void approveCanHideReportedCommentAndRecordOperation() {
        ContentReport report = new ContentReport();
        report.setId(101L);
        report.setTargetType(ContentReport.TargetType.COMMENT);
        report.setTargetId(55L);
        when(reportRepo.findById(101L)).thenReturn(Optional.of(report));
        Comment comment = new Comment();
        comment.setId(55L);
        comment.setRefId(1L);
        comment.setAuthor("visitor");
        comment.setContent("bad comment");
        comment.setStatus(Comment.CommentStatus.NORMAL);
        when(commentRepo.findById(55L)).thenReturn(Optional.of(comment));

        ReportReviewRequest request = new ReportReviewRequest();
        request.setHideContent(true);
        request.setReviewNote("  评论违规  ");

        service.approve(101L, request, "moderator");

        assertThat(comment.getStatus()).isEqualTo(Comment.CommentStatus.HIDDEN);
        verify(commentRepo).save(comment);
        ArgumentCaptor<AdminOperationLog> logCaptor = ArgumentCaptor.forClass(AdminOperationLog.class);
        verify(operationLogRepo, times(2)).save(logCaptor.capture());
        List<AdminOperationLog> logs = logCaptor.getAllValues();
        assertThat(logs)
                .extracting(AdminOperationLog::getAction)
                .containsExactly("APPROVE_CONTENT_REPORT", "HIDE_COMMENT");
        AdminOperationLog log = logs.get(1);
        assertThat(log.getOperatorUsername()).isEqualTo("moderator");
        assertThat(log.getAction()).isEqualTo("HIDE_COMMENT");
        assertThat(log.getTargetType()).isEqualTo("COMMENT");
        assertThat(log.getTargetId()).isEqualTo(55L);
        assertThat(log.getDetail()).isEqualTo("评论违规");
    }

    @Test
    void rejectRecordsReportReviewOperation() {
        ContentReport report = new ContentReport();
        report.setId(102L);
        report.setTargetType(ContentReport.TargetType.POST);
        report.setTargetId(11L);
        when(reportRepo.findById(102L)).thenReturn(Optional.of(report));
        ReportReviewRequest request = new ReportReviewRequest();
        request.setReviewNote("证据不足");

        service.reject(102L, request, "reviewer");

        ArgumentCaptor<AdminOperationLog> logCaptor = ArgumentCaptor.forClass(AdminOperationLog.class);
        verify(operationLogRepo).save(logCaptor.capture());
        AdminOperationLog log = logCaptor.getValue();
        assertThat(log.getOperatorUsername()).isEqualTo("reviewer");
        assertThat(log.getAction()).isEqualTo("REJECT_CONTENT_REPORT");
        assertThat(log.getTargetType()).isEqualTo("CONTENT_REPORT");
        assertThat(log.getTargetId()).isEqualTo(102L);
        assertThat(log.getDetail()).isEqualTo("证据不足");
    }

    @Test
    void currentTargetReturnsCurrentPostContent() {
        ContentReport report = new ContentReport();
        report.setId(100L);
        report.setTargetType(ContentReport.TargetType.POST);
        report.setTargetId(11L);
        when(reportRepo.findById(100L)).thenReturn(Optional.of(report));
        ForumThread thread = new ForumThread();
        thread.setId(11L);
        thread.setCategoryId(1L);
        thread.setAuthorId(22L);
        thread.setTitle("当前标题");
        thread.setContentMarkdown("当前正文");
        thread.setStatus(ForumThread.ThreadStatus.HIDDEN);
        when(threadRepo.findById(11L)).thenReturn(Optional.of(thread));

        Optional<ContentReportTargetResponse> target = service.currentTarget(100L);

        assertThat(target).isPresent();
        ContentReportTargetResponse response = target.orElseThrow();
        assertThat(response.isExists()).isTrue();
        assertThat(response.getTitle()).isEqualTo("当前标题");
        assertThat(response.getContent()).isEqualTo("当前正文");
        assertThat(response.getStatus()).isEqualTo("HIDDEN");
        assertThat(response.getAuthorId()).isEqualTo(22L);
    }

    @Test
    void currentTargetReturnsMissingStateWhenTargetRowDoesNotExist() {
        ContentReport report = new ContentReport();
        report.setId(103L);
        report.setTargetType(ContentReport.TargetType.REPLY);
        report.setTargetId(77L);
        when(reportRepo.findById(103L)).thenReturn(Optional.of(report));
        when(replyRepo.findById(77L)).thenReturn(Optional.empty());

        Optional<ContentReportTargetResponse> target = service.currentTarget(103L);

        assertThat(target).isPresent();
        ContentReportTargetResponse response = target.orElseThrow();
        assertThat(response.isExists()).isFalse();
        assertThat(response.getTargetType()).isEqualTo(ContentReport.TargetType.REPLY);
        assertThat(response.getTargetId()).isEqualTo(77L);
    }
}
