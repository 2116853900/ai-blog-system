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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.function.LongSupplier;

@Service
public class AdminDashboardService {

    private final CommentRepository commentRepo;
    private final SubmissionRepository submissionRepo;
    private final ContentReportRepository reportRepo;
    private final PostRepository postRepo;
    private final SkillRepository skillRepo;
    private final McpRepository mcpRepo;
    private final ApiStationRepository apiStationRepo;
    private final ForumUserRepository forumUserRepo;
    private final ForumThreadRepository forumThreadRepo;
    private final ForumReplyRepository forumReplyRepo;
    private final long cacheTtlMs;
    private final LongSupplier currentTimeMillis;

    private volatile CachedOverview cachedOverview;

    @Autowired
    public AdminDashboardService(CommentRepository commentRepo,
                                 SubmissionRepository submissionRepo,
                                 ContentReportRepository reportRepo,
                                 PostRepository postRepo,
                                 SkillRepository skillRepo,
                                 McpRepository mcpRepo,
                                 ApiStationRepository apiStationRepo,
                                 ForumUserRepository forumUserRepo,
                                 ForumThreadRepository forumThreadRepo,
                                 ForumReplyRepository forumReplyRepo,
                                 @Value("${app.admin-dashboard.cache-ttl-ms:5000}") long cacheTtlMs) {
        this(commentRepo,
                submissionRepo,
                reportRepo,
                postRepo,
                skillRepo,
                mcpRepo,
                apiStationRepo,
                forumUserRepo,
                forumThreadRepo,
                forumReplyRepo,
                cacheTtlMs,
                System::currentTimeMillis);
    }

    AdminDashboardService(CommentRepository commentRepo,
                          SubmissionRepository submissionRepo,
                          ContentReportRepository reportRepo,
                          PostRepository postRepo,
                          SkillRepository skillRepo,
                          McpRepository mcpRepo,
                          ApiStationRepository apiStationRepo,
                          ForumUserRepository forumUserRepo,
                          ForumThreadRepository forumThreadRepo,
                          ForumReplyRepository forumReplyRepo,
                          long cacheTtlMs,
                          LongSupplier currentTimeMillis) {
        this.commentRepo = commentRepo;
        this.submissionRepo = submissionRepo;
        this.reportRepo = reportRepo;
        this.postRepo = postRepo;
        this.skillRepo = skillRepo;
        this.mcpRepo = mcpRepo;
        this.apiStationRepo = apiStationRepo;
        this.forumUserRepo = forumUserRepo;
        this.forumThreadRepo = forumThreadRepo;
        this.forumReplyRepo = forumReplyRepo;
        this.cacheTtlMs = Math.max(0, cacheTtlMs);
        this.currentTimeMillis = currentTimeMillis;
    }

    public AdminDashboardResponse overview() {
        if (cacheTtlMs <= 0) {
            return loadOverview();
        }

        long now = currentTimeMillis.getAsLong();
        CachedOverview cached = cachedOverview;
        if (cached != null && cached.expiresAtMs() > now) {
            return cached.response();
        }

        synchronized (this) {
            cached = cachedOverview;
            if (cached != null && cached.expiresAtMs() > now) {
                return cached.response();
            }
            AdminDashboardResponse response = loadOverview();
            cachedOverview = new CachedOverview(response, now + cacheTtlMs);
            return response;
        }
    }

    public void evictCache() {
        cachedOverview = null;
    }

    private AdminDashboardResponse loadOverview() {
        return new AdminDashboardResponse(
                new AdminDashboardResponse.Moderation(
                        commentRepo.countByApprovedFalseAndStatus(Comment.CommentStatus.NORMAL),
                        submissionRepo.countByStatus(Submission.Status.PENDING),
                        reportRepo.countByStatus(ContentReport.ReportStatus.PENDING)
                ),
                new AdminDashboardResponse.Content(
                        postRepo.count(),
                        skillRepo.count(),
                        mcpRepo.count(),
                        apiStationRepo.count()
                ),
                new AdminDashboardResponse.Community(
                        forumUserRepo.count(),
                        forumUserRepo.countByStatus(ForumUser.Status.ACTIVE),
                        forumUserRepo.countByStatus(ForumUser.Status.BANNED),
                        forumThreadRepo.count(),
                        forumReplyRepo.count()
                ),
                new AdminDashboardResponse.ApiStations(
                        apiStationRepo.countByStatus(ApiStation.Status.UP),
                        apiStationRepo.countByStatus(ApiStation.Status.DOWN),
                        apiStationRepo.countByStatus(ApiStation.Status.UNKNOWN)
                )
        );
    }

    private record CachedOverview(AdminDashboardResponse response, long expiresAtMs) {
    }
}
