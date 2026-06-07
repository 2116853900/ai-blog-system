package com.aiblog.service;

import com.aiblog.dto.PublicStatsResponse;
import com.aiblog.dto.ResourceTagSummaryResponse;
import com.aiblog.entity.ApiStation;
import com.aiblog.entity.ForumReply;
import com.aiblog.entity.ForumThread;
import com.aiblog.entity.Mcp;
import com.aiblog.entity.Post;
import com.aiblog.entity.Skill;
import com.aiblog.repository.ApiStationRepository;
import com.aiblog.repository.ForumReplyRepository;
import com.aiblog.repository.ForumThreadRepository;
import com.aiblog.repository.McpRepository;
import com.aiblog.repository.PostRepository;
import com.aiblog.repository.SkillRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class PublicStatsService {

    private static final int RECENT_LIMIT_PER_TYPE = 6;
    private static final int RECENT_LIMIT = 10;
    private static final int TAG_LIMIT = 16;
    private static final List<ForumThread.ThreadStatus> VISIBLE_THREAD_STATUSES = List.of(
            ForumThread.ThreadStatus.NORMAL,
            ForumThread.ThreadStatus.PINNED,
            ForumThread.ThreadStatus.FEATURED,
            ForumThread.ThreadStatus.LOCKED
    );

    private final PostRepository postRepo;
    private final SkillRepository skillRepo;
    private final McpRepository mcpRepo;
    private final ApiStationRepository apiStationRepo;
    private final ForumThreadRepository threadRepo;
    private final ForumReplyRepository replyRepo;

    public PublicStatsService(PostRepository postRepo,
                              SkillRepository skillRepo,
                              McpRepository mcpRepo,
                              ApiStationRepository apiStationRepo,
                              ForumThreadRepository threadRepo,
                              ForumReplyRepository replyRepo) {
        this.postRepo = postRepo;
        this.skillRepo = skillRepo;
        this.mcpRepo = mcpRepo;
        this.apiStationRepo = apiStationRepo;
        this.threadRepo = threadRepo;
        this.replyRepo = replyRepo;
    }

    public PublicStatsResponse getStats() {
        long posts = postRepo.count(publishedPostSpec());
        long skills = skillRepo.count();
        long mcps = mcpRepo.count();
        long apiStations = apiStationRepo.count();

        var content = new PublicStatsResponse.ContentMetrics(
                posts,
                skills,
                mcps,
                apiStations,
                posts + skills + mcps + apiStations
        );

        long up = apiStationRepo.countByStatus(ApiStation.Status.UP);
        long down = apiStationRepo.countByStatus(ApiStation.Status.DOWN);
        long unknown = apiStationRepo.countByStatus(ApiStation.Status.UNKNOWN);
        var apiHealth = new PublicStatsResponse.ApiHealthMetrics(
                apiStations,
                up,
                down,
                unknown,
                PublicStatsResponse.uptimeRate(apiStations, up),
                averageLatency(apiStationRepo.findAll())
        );

        var community = new PublicStatsResponse.CommunityMetrics(
                threadRepo.count(visibleThreadSpec()),
                replyRepo.count(normalReplySpec()),
                threadRepo.count(solvedThreadSpec()),
                threadRepo.sumViewCountByStatusIn(VISIBLE_THREAD_STATUSES),
                threadRepo.sumLikeCountByStatusIn(VISIBLE_THREAD_STATUSES),
                threadRepo.sumFavoriteCountByStatusIn(VISIBLE_THREAD_STATUSES)
        );

        return new PublicStatsResponse(
                Instant.now(),
                content,
                community,
                apiHealth,
                popularTags(),
                recentItems(),
                hotThreads()
        );
    }

    private List<PublicStatsResponse.TagMetric> popularTags() {
        List<String> tagTexts = new ArrayList<>();
        tagTexts.addAll(postRepo.findByPublishedTrueOrderByCreatedAtDesc().stream().map(Post::getTags).toList());
        tagTexts.addAll(skillRepo.findAll().stream().map(Skill::getTags).toList());
        tagTexts.addAll(mcpRepo.findAll().stream().map(Mcp::getTags).toList());
        tagTexts.addAll(apiStationRepo.findAll().stream().map(ApiStation::getTags).toList());
        tagTexts.addAll(threadRepo.findTagTextsByStatusIn(VISIBLE_THREAD_STATUSES));

        return ResourceTagService.popularTags(tagTexts, TAG_LIMIT).stream()
                .map(this::toTagMetric)
                .toList();
    }

    private PublicStatsResponse.TagMetric toTagMetric(ResourceTagSummaryResponse tag) {
        return new PublicStatsResponse.TagMetric(
                tag.tag(),
                tag.count(),
                "/search?q=" + URLEncoder.encode(tag.tag(), StandardCharsets.UTF_8)
        );
    }

    private List<PublicStatsResponse.RecentItem> recentItems() {
        PageRequest latest = PageRequest.of(0, RECENT_LIMIT_PER_TYPE, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<PublicStatsResponse.RecentItem> items = new ArrayList<>();

        postRepo.findAll(publishedPostSpec(), latest).forEach(post -> items.add(new PublicStatsResponse.RecentItem(
                "POST",
                post.getTitle(),
                post.getSummary(),
                "/tutorials/" + post.getSlug(),
                post.getCategory(),
                post.getTags(),
                post.getCreatedAt(),
                "教程"
        )));

        skillRepo.findAll(latest).forEach(skill -> items.add(new PublicStatsResponse.RecentItem(
                "SKILL",
                skill.getName(),
                skill.getDescription(),
                "/skills/" + skill.getId(),
                skill.getCategory(),
                skill.getTags(),
                skill.getCreatedAt(),
                recommendMetric(skill.getRecommendLevel())
        )));

        mcpRepo.findAll(latest).forEach(mcp -> items.add(new PublicStatsResponse.RecentItem(
                "MCP",
                mcp.getName(),
                mcp.getDescription(),
                "/mcps/" + mcp.getId(),
                mcp.getCategory(),
                mcp.getTags(),
                mcp.getCreatedAt(),
                recommendMetric(mcp.getRecommendLevel())
        )));

        apiStationRepo.findAll(latest).forEach(api -> items.add(new PublicStatsResponse.RecentItem(
                "API",
                api.getName(),
                api.getDescription(),
                "/api-stations/" + api.getId(),
                null,
                api.getTags(),
                api.getCreatedAt(),
                PublicStatsResponse.apiStatusMetric(api.getStatus(), api.getLatencyMs())
        )));

        threadRepo.findByStatusIn(VISIBLE_THREAD_STATUSES, latest).forEach(thread -> items.add(new PublicStatsResponse.RecentItem(
                "FORUM_THREAD",
                thread.getTitle(),
                snippet(thread.getContentMarkdown()),
                "/forum/threads/" + thread.getId(),
                null,
                thread.getTags(),
                thread.getCreatedAt(),
                thread.getReplyCount() + " replies"
        )));

        return items.stream()
                .sorted(Comparator.comparing(PublicStatsResponse.RecentItem::createdAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(RECENT_LIMIT)
                .toList();
    }

    private List<PublicStatsResponse.HotThread> hotThreads() {
        PageRequest hot = PageRequest.of(0, 8, Sort.by(
                Sort.Order.desc("viewCount"),
                Sort.Order.desc("replyCount"),
                Sort.Order.desc("likeCount"),
                Sort.Order.desc("createdAt")
        ));

        return threadRepo.findByStatusIn(VISIBLE_THREAD_STATUSES, hot).stream()
                .map(thread -> new PublicStatsResponse.HotThread(
                        thread.getId(),
                        thread.getTitle(),
                        "/forum/threads/" + thread.getId(),
                        thread.getTags(),
                        thread.getViewCount(),
                        thread.getReplyCount(),
                        thread.getLikeCount(),
                        thread.getLastReplyAt() != null ? thread.getLastReplyAt() : thread.getCreatedAt(),
                        thread.getAcceptedReplyId() != null
                ))
                .toList();
    }

    private Integer averageLatency(List<ApiStation> stations) {
        var values = stations.stream()
                .map(ApiStation::getLatencyMs)
                .filter(latency -> latency != null && latency >= 0)
                .toList();
        if (values.isEmpty()) {
            return null;
        }
        return (int) Math.round(values.stream().mapToInt(Integer::intValue).average().orElse(0));
    }

    private String recommendMetric(Integer recommendLevel) {
        if (recommendLevel == null) {
            return "推荐";
        }
        return recommendLevel + "/5";
    }

    private String snippet(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 120) {
            return normalized;
        }
        return normalized.substring(0, 120) + "...";
    }

    private Specification<Post> publishedPostSpec() {
        return (root, query, cb) -> cb.isTrue(root.get("published"));
    }

    private Specification<ForumThread> visibleThreadSpec() {
        return (root, query, cb) -> root.get("status").in(VISIBLE_THREAD_STATUSES);
    }

    private Specification<ForumThread> solvedThreadSpec() {
        return (root, query, cb) -> cb.and(
                root.get("status").in(VISIBLE_THREAD_STATUSES),
                cb.isNotNull(root.get("acceptedReplyId"))
        );
    }

    private Specification<ForumReply> normalReplySpec() {
        return (root, query, cb) -> cb.equal(root.get("status"), ForumReply.ReplyStatus.NORMAL);
    }
}
