package com.aiblog.service;

import com.aiblog.dto.AdminDashboardResponse;
import com.aiblog.entity.ApiStation;
import com.aiblog.entity.Comment;
import com.aiblog.entity.ContentReport;
import com.aiblog.entity.ForumUser;
import com.aiblog.entity.Submission;
import com.aiblog.repository.ApiStationRepository;
import com.aiblog.repository.CommentRepository;
import com.aiblog.repository.ContentReportRepository;
import com.aiblog.repository.ForumReplyRepository;
import com.aiblog.repository.ForumThreadRepository;
import com.aiblog.repository.ForumUserRepository;
import com.aiblog.repository.McpRepository;
import com.aiblog.repository.PostRepository;
import com.aiblog.repository.SkillRepository;
import com.aiblog.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock private CommentRepository commentRepo;
    @Mock private SubmissionRepository submissionRepo;
    @Mock private ContentReportRepository reportRepo;
    @Mock private PostRepository postRepo;
    @Mock private SkillRepository skillRepo;
    @Mock private McpRepository mcpRepo;
    @Mock private ApiStationRepository apiStationRepo;
    @Mock private ForumUserRepository forumUserRepo;
    @Mock private ForumThreadRepository forumThreadRepo;
    @Mock private ForumReplyRepository forumReplyRepo;

    private AdminDashboardService service;
    private AtomicLong now;

    @BeforeEach
    void setUp() {
        now = new AtomicLong(1_000);
        service = new AdminDashboardService(
                commentRepo,
                submissionRepo,
                reportRepo,
                postRepo,
                skillRepo,
                mcpRepo,
                apiStationRepo,
                forumUserRepo,
                forumThreadRepo,
                forumReplyRepo,
                5_000,
                now::get
        );
    }

    @Test
    void overviewAggregatesModerationContentCommunityAndApiStatusCounts() {
        when(commentRepo.countByApprovedFalseAndStatus(Comment.CommentStatus.NORMAL)).thenReturn(2L);
        when(submissionRepo.countByStatus(Submission.Status.PENDING)).thenReturn(3L);
        when(reportRepo.countByStatus(ContentReport.ReportStatus.PENDING)).thenReturn(4L);
        when(postRepo.count()).thenReturn(5L);
        when(skillRepo.count()).thenReturn(6L);
        when(mcpRepo.count()).thenReturn(7L);
        when(apiStationRepo.count()).thenReturn(8L);
        when(forumUserRepo.count()).thenReturn(9L);
        when(forumUserRepo.countByStatus(ForumUser.Status.ACTIVE)).thenReturn(8L);
        when(forumUserRepo.countByStatus(ForumUser.Status.BANNED)).thenReturn(1L);
        when(forumThreadRepo.count()).thenReturn(10L);
        when(forumReplyRepo.count()).thenReturn(11L);
        when(apiStationRepo.countByStatus(ApiStation.Status.UP)).thenReturn(4L);
        when(apiStationRepo.countByStatus(ApiStation.Status.DOWN)).thenReturn(2L);
        when(apiStationRepo.countByStatus(ApiStation.Status.UNKNOWN)).thenReturn(2L);

        AdminDashboardResponse response = service.overview();

        assertThat(response.moderation().pendingComments()).isEqualTo(2L);
        assertThat(response.moderation().pendingSubmissions()).isEqualTo(3L);
        assertThat(response.moderation().pendingReports()).isEqualTo(4L);
        assertThat(response.content().posts()).isEqualTo(5L);
        assertThat(response.content().skills()).isEqualTo(6L);
        assertThat(response.content().mcps()).isEqualTo(7L);
        assertThat(response.content().apiStations()).isEqualTo(8L);
        assertThat(response.community().users()).isEqualTo(9L);
        assertThat(response.community().activeUsers()).isEqualTo(8L);
        assertThat(response.community().bannedUsers()).isEqualTo(1L);
        assertThat(response.community().threads()).isEqualTo(10L);
        assertThat(response.community().replies()).isEqualTo(11L);
        assertThat(response.apiStations().up()).isEqualTo(4L);
        assertThat(response.apiStations().down()).isEqualTo(2L);
        assertThat(response.apiStations().unknown()).isEqualTo(2L);
        verify(commentRepo).countByApprovedFalseAndStatus(Comment.CommentStatus.NORMAL);
        verify(submissionRepo).countByStatus(Submission.Status.PENDING);
        verify(reportRepo).countByStatus(ContentReport.ReportStatus.PENDING);
    }

    @Test
    void overviewReusesCachedResponseWithinTtl() {
        stubOverviewCounts(2L);

        AdminDashboardResponse first = service.overview();
        AdminDashboardResponse second = service.overview();

        assertThat(second).isSameAs(first);
        verify(commentRepo, times(1)).countByApprovedFalseAndStatus(Comment.CommentStatus.NORMAL);
        verify(postRepo, times(1)).count();
    }

    @Test
    void overviewReloadsAfterCacheExpires() {
        stubOverviewCounts(2L);
        AdminDashboardResponse first = service.overview();
        now.addAndGet(5_001);
        when(commentRepo.countByApprovedFalseAndStatus(Comment.CommentStatus.NORMAL)).thenReturn(9L);

        AdminDashboardResponse second = service.overview();

        assertThat(first.moderation().pendingComments()).isEqualTo(2L);
        assertThat(second.moderation().pendingComments()).isEqualTo(9L);
        verify(commentRepo, times(2)).countByApprovedFalseAndStatus(Comment.CommentStatus.NORMAL);
    }

    @Test
    void evictCacheForcesReloadBeforeTtlExpires() {
        stubOverviewCounts(2L);
        service.overview();
        service.evictCache();
        when(commentRepo.countByApprovedFalseAndStatus(Comment.CommentStatus.NORMAL)).thenReturn(6L);

        AdminDashboardResponse response = service.overview();

        assertThat(response.moderation().pendingComments()).isEqualTo(6L);
        verify(commentRepo, times(2)).countByApprovedFalseAndStatus(Comment.CommentStatus.NORMAL);
    }

    private void stubOverviewCounts(long pendingComments) {
        when(commentRepo.countByApprovedFalseAndStatus(Comment.CommentStatus.NORMAL)).thenReturn(pendingComments);
        when(submissionRepo.countByStatus(Submission.Status.PENDING)).thenReturn(3L);
        when(reportRepo.countByStatus(ContentReport.ReportStatus.PENDING)).thenReturn(4L);
        when(postRepo.count()).thenReturn(5L);
        when(skillRepo.count()).thenReturn(6L);
        when(mcpRepo.count()).thenReturn(7L);
        when(apiStationRepo.count()).thenReturn(8L);
        when(forumUserRepo.count()).thenReturn(9L);
        when(forumUserRepo.countByStatus(ForumUser.Status.ACTIVE)).thenReturn(8L);
        when(forumUserRepo.countByStatus(ForumUser.Status.BANNED)).thenReturn(1L);
        when(forumThreadRepo.count()).thenReturn(10L);
        when(forumReplyRepo.count()).thenReturn(11L);
        when(apiStationRepo.countByStatus(ApiStation.Status.UP)).thenReturn(4L);
        when(apiStationRepo.countByStatus(ApiStation.Status.DOWN)).thenReturn(2L);
        when(apiStationRepo.countByStatus(ApiStation.Status.UNKNOWN)).thenReturn(2L);
    }
}
