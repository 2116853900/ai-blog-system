package com.aiblog.service;

import com.aiblog.dto.GlobalSearchGroupResponse;
import com.aiblog.dto.GlobalSearchItemResponse;
import com.aiblog.dto.GlobalSearchResponse;
import com.aiblog.entity.ApiStation;
import com.aiblog.entity.ForumThread;
import com.aiblog.entity.Mcp;
import com.aiblog.entity.Post;
import com.aiblog.entity.Skill;
import com.aiblog.repository.ApiStationRepository;
import com.aiblog.repository.ForumThreadRepository;
import com.aiblog.repository.McpRepository;
import com.aiblog.repository.PostRepository;
import com.aiblog.repository.SkillRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GlobalSearchService {

    private static final int DEFAULT_LIMIT = 6;
    private static final List<ForumThread.ThreadStatus> VISIBLE_THREAD_STATUSES = List.of(
            ForumThread.ThreadStatus.NORMAL,
            ForumThread.ThreadStatus.PINNED,
            ForumThread.ThreadStatus.FEATURED,
            ForumThread.ThreadStatus.LOCKED
    );

    private final PostRepository postRepo;
    private final SkillRepository skillRepo;
    private final McpRepository mcpRepo;
    private final ApiStationRepository apiRepo;
    private final ForumThreadRepository threadRepo;

    public GlobalSearchService(PostRepository postRepo,
                               SkillRepository skillRepo,
                               McpRepository mcpRepo,
                               ApiStationRepository apiRepo,
                               ForumThreadRepository threadRepo) {
        this.postRepo = postRepo;
        this.skillRepo = skillRepo;
        this.mcpRepo = mcpRepo;
        this.apiRepo = apiRepo;
        this.threadRepo = threadRepo;
    }

    public GlobalSearchResponse search(String rawQuery, Integer rawLimit) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        if (query.isEmpty()) {
            return new GlobalSearchResponse("", List.of());
        }

        int limit = clampLimit(rawLimit);
        List<GlobalSearchGroupResponse> groups = new ArrayList<>();

        List<GlobalSearchItemResponse> posts = postRepo
                .searchPublished(query, PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .map(this::fromPost)
                .toList();
        addGroup(groups, "POST", "教程", posts);

        List<GlobalSearchItemResponse> skills = skillRepo
                .findAll(
                        SearchSpecs.build(query, null, null, List.of("name", "description", "tags", "category")),
                        PageRequest.of(0, limit,
                                Sort.by(Sort.Direction.DESC, "recommendLevel").and(Sort.by(Sort.Direction.DESC, "createdAt"))))
                .stream()
                .map(this::fromSkill)
                .toList();
        addGroup(groups, "SKILL", "AI Skill", skills);

        List<GlobalSearchItemResponse> mcps = mcpRepo
                .findAll(
                        SearchSpecs.build(query, null, null, List.of("name", "description", "tags", "category")),
                        PageRequest.of(0, limit,
                                Sort.by(Sort.Direction.DESC, "recommendLevel").and(Sort.by(Sort.Direction.DESC, "createdAt"))))
                .stream()
                .map(this::fromMcp)
                .toList();
        addGroup(groups, "MCP", "MCP", mcps);

        List<GlobalSearchItemResponse> apiStations = apiRepo
                .findAll(
                        SearchSpecs.build(query, null, null, List.of("name", "description", "supportedModels", "tags")),
                        PageRequest.of(0, limit, Sort.by(Sort.Direction.ASC, "name")))
                .stream()
                .map(this::fromApiStation)
                .toList();
        addGroup(groups, "API", "公益 API", apiStations);

        List<GlobalSearchItemResponse> threads = threadRepo
                .searchVisible(null, query, VISIBLE_THREAD_STATUSES,
                        PageRequest.of(0, limit,
                                Sort.by(Sort.Direction.DESC, "lastReplyAt").and(Sort.by(Sort.Direction.DESC, "createdAt"))))
                .stream()
                .map(this::fromForumThread)
                .toList();
        addGroup(groups, "FORUM_THREAD", "论坛讨论", threads);

        return new GlobalSearchResponse(query, groups);
    }

    private int clampLimit(Integer rawLimit) {
        int limit = rawLimit == null ? DEFAULT_LIMIT : rawLimit;
        return Math.max(1, Math.min(20, limit));
    }

    private void addGroup(List<GlobalSearchGroupResponse> groups,
                          String type,
                          String label,
                          List<GlobalSearchItemResponse> items) {
        if (!items.isEmpty()) {
            groups.add(new GlobalSearchGroupResponse(type, label, items));
        }
    }

    private GlobalSearchItemResponse fromPost(Post post) {
        GlobalSearchItemResponse item = base("POST", post.getId(), post.getTitle(), post.getSummary(),
                "/tutorials/" + post.getSlug(), post.getCategory(), post.getTags());
        item.setCreatedAt(post.getCreatedAt());
        return item;
    }

    private GlobalSearchItemResponse fromSkill(Skill skill) {
        GlobalSearchItemResponse item = base("SKILL", skill.getId(), skill.getName(), skill.getDescription(),
                "/skills/" + skill.getId(), skill.getCategory(), skill.getTags());
        item.setCreatedAt(skill.getCreatedAt());
        item.setMeta("推荐 " + nullToDefault(skill.getRecommendLevel(), 0) + "/5");
        return item;
    }

    private GlobalSearchItemResponse fromMcp(Mcp mcp) {
        GlobalSearchItemResponse item = base("MCP", mcp.getId(), mcp.getName(), mcp.getDescription(),
                "/mcps/" + mcp.getId(), mcp.getCategory(), mcp.getTags());
        item.setCreatedAt(mcp.getCreatedAt());
        item.setMeta("推荐 " + nullToDefault(mcp.getRecommendLevel(), 0) + "/5");
        return item;
    }

    private GlobalSearchItemResponse fromApiStation(ApiStation apiStation) {
        GlobalSearchItemResponse item = base("API", apiStation.getId(), apiStation.getName(),
                apiStation.getDescription(), "/api-stations/" + apiStation.getId(), null, apiStation.getTags());
        item.setCreatedAt(apiStation.getCreatedAt());
        item.setMeta(apiStation.getStatus().name() + formatLatency(apiStation.getLatencyMs()));
        return item;
    }

    private GlobalSearchItemResponse fromForumThread(ForumThread thread) {
        GlobalSearchItemResponse item = base("FORUM_THREAD", thread.getId(), thread.getTitle(),
                excerpt(thread.getContentMarkdown()), "/forum/threads/" + thread.getId(), null, thread.getTags());
        item.setCreatedAt(thread.getCreatedAt());
        item.setMeta(thread.getReplyCount() + " 回复 · " + thread.getViewCount() + " 浏览");
        return item;
    }

    private GlobalSearchItemResponse base(String type,
                                          Long id,
                                          String title,
                                          String description,
                                          String url,
                                          String category,
                                          String tags) {
        GlobalSearchItemResponse item = new GlobalSearchItemResponse();
        item.setType(type);
        item.setId(id);
        item.setTitle(title);
        item.setDescription(excerpt(description));
        item.setUrl(url);
        item.setCategory(category);
        item.setTags(tags);
        return item;
    }

    private String excerpt(String value) {
        if (value == null) return null;
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 180 ? normalized : normalized.substring(0, 177) + "...";
    }

    private Integer nullToDefault(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String formatLatency(Integer latencyMs) {
        return latencyMs == null ? "" : " · " + latencyMs + "ms";
    }
}
