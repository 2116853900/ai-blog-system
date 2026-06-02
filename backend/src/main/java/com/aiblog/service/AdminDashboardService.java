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
import org.springframework.stereotype.Service;

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

    public AdminDashboardService(CommentRepository commentRepo,
                                 SubmissionRepository submissionRepo,
                                 ContentReportRepository reportRepo,
                                 PostRepository postRepo,
                                 SkillRepository skillRepo,
                                 McpRepository mcpRepo,
                                 ApiStationRepository apiStationRepo,
                                 ForumUserRepository forumUserRepo,
                                 ForumThreadRepository forumThreadRepo,
                                 ForumReplyRepository forumReplyRepo) {
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
    }

    public AdminDashboardResponse overview() {
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
}
