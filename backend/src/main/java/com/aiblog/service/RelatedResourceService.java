package com.aiblog.service;

import com.aiblog.dto.RelatedResourceResponse;
import com.aiblog.entity.ApiStation;
import com.aiblog.entity.Mcp;
import com.aiblog.entity.Post;
import com.aiblog.entity.ResourceFavorite;
import com.aiblog.entity.Skill;
import com.aiblog.repository.ApiStationRepository;
import com.aiblog.repository.McpRepository;
import com.aiblog.repository.PostRepository;
import com.aiblog.repository.SkillRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class RelatedResourceService {

    private static final Pattern TAG_SPLITTER = Pattern.compile("[,，]");
    private static final Pattern TOKEN_SPLITTER = Pattern.compile("[\\s,，。；;：:、/\\\\|()（）\\[\\]{}<>《》\"'`!?！？.-]+");

    private final PostRepository postRepo;
    private final SkillRepository skillRepo;
    private final McpRepository mcpRepo;
    private final ApiStationRepository apiRepo;

    public RelatedResourceService(PostRepository postRepo,
                                  SkillRepository skillRepo,
                                  McpRepository mcpRepo,
                                  ApiStationRepository apiRepo) {
        this.postRepo = postRepo;
        this.skillRepo = skillRepo;
        this.mcpRepo = mcpRepo;
        this.apiRepo = apiRepo;
    }

    public Optional<List<RelatedResourceResponse>> related(ResourceFavorite.RefType refType, Long refId, int limit) {
        if (refType == null || refId == null) {
            return Optional.empty();
        }
        Optional<ResourceCandidate> source = source(refType, refId);
        if (source.isEmpty()) {
            return Optional.empty();
        }

        ResourceCandidate src = source.get();
        int cappedLimit = Math.max(1, Math.min(limit, 12));
        List<RelatedResourceResponse> result = candidates().stream()
                .filter(candidate -> !candidate.sameIdentity(src))
                .map(candidate -> score(src, candidate))
                .filter(scored -> scored.score() > 0)
                .sorted(Comparator.comparingInt(ScoredCandidate::score).reversed()
                        .thenComparing(scored -> scored.candidate().type())
                        .thenComparing(scored -> scored.candidate().title(), String.CASE_INSENSITIVE_ORDER))
                .limit(cappedLimit)
                .map(ScoredCandidate::toResponse)
                .toList();

        return Optional.of(result);
    }

    private Optional<ResourceCandidate> source(ResourceFavorite.RefType refType, Long refId) {
        return switch (refType) {
            case POST -> postRepo.findById(refId).filter(Post::isPublished).map(this::postCandidate);
            case SKILL -> skillRepo.findById(refId).map(this::skillCandidate);
            case MCP -> mcpRepo.findById(refId).map(this::mcpCandidate);
            case API -> apiRepo.findById(refId).map(this::apiCandidate);
        };
    }

    private List<ResourceCandidate> candidates() {
        List<ResourceCandidate> candidates = new ArrayList<>();
        postRepo.findByPublishedTrueOrderByCreatedAtDesc().forEach(post -> candidates.add(postCandidate(post)));
        skillRepo.findAll().forEach(skill -> candidates.add(skillCandidate(skill)));
        mcpRepo.findAll().forEach(mcp -> candidates.add(mcpCandidate(mcp)));
        apiRepo.findAll().forEach(api -> candidates.add(apiCandidate(api)));
        return candidates;
    }

    private ScoredCandidate score(ResourceCandidate source, ResourceCandidate candidate) {
        List<String> sharedTags = sharedTags(source, candidate);
        int categoryScore = sameText(source.category(), candidate.category()) ? 6 : 0;
        int tokenScore = Math.min(8, sharedTokens(source, candidate) * 2);
        int score = sharedTags.size() * 12 + categoryScore + tokenScore + candidate.qualityScore();
        String reason = reason(sharedTags, source.category(), candidate.category(), tokenScore, candidate.qualityScore());
        return new ScoredCandidate(candidate, score, reason);
    }

    private String reason(List<String> sharedTags, String sourceCategory, String candidateCategory, int tokenScore, int qualityScore) {
        if (!sharedTags.isEmpty()) {
            return "共同标签：" + String.join("、", sharedTags.stream().limit(3).toList());
        }
        if (sameText(sourceCategory, candidateCategory)) {
            return "同属分类：" + candidateCategory;
        }
        if (tokenScore > 0) {
            return "关键词相近";
        }
        if (qualityScore >= 4) {
            return "推荐级别较高";
        }
        return "可作为延伸阅读";
    }

    private ResourceCandidate postCandidate(Post post) {
        return new ResourceCandidate(
                ResourceFavorite.RefType.POST,
                post.getId(),
                value(post.getTitle()),
                value(post.getSummary()),
                "/tutorials/" + post.getSlug(),
                post.getCategory(),
                post.getTags(),
                tags(post.getTags()),
                1
        );
    }

    private ResourceCandidate skillCandidate(Skill skill) {
        return new ResourceCandidate(
                ResourceFavorite.RefType.SKILL,
                skill.getId(),
                value(skill.getName()),
                value(skill.getDescription()),
                "/skills/" + skill.getId(),
                skill.getCategory(),
                skill.getTags(),
                tags(skill.getTags()),
                safeLevel(skill.getRecommendLevel())
        );
    }

    private ResourceCandidate mcpCandidate(Mcp mcp) {
        return new ResourceCandidate(
                ResourceFavorite.RefType.MCP,
                mcp.getId(),
                value(mcp.getName()),
                value(mcp.getDescription()),
                "/mcps/" + mcp.getId(),
                mcp.getCategory(),
                mcp.getTags(),
                tags(mcp.getTags()),
                safeLevel(mcp.getRecommendLevel())
        );
    }

    private ResourceCandidate apiCandidate(ApiStation api) {
        return new ResourceCandidate(
                ResourceFavorite.RefType.API,
                api.getId(),
                value(api.getName()),
                value(api.getDescription()),
                "/api-stations/" + api.getId(),
                null,
                api.getTags(),
                tags(api.getTags()),
                switch (api.getStatus()) {
                    case UP -> 3;
                    case UNKNOWN -> 1;
                    case DOWN -> 0;
                }
        );
    }

    private List<String> sharedTags(ResourceCandidate source, ResourceCandidate candidate) {
        Set<String> candidateTagKeys = candidate.tagMap().keySet();
        return source.tagMap().entrySet().stream()
                .filter(entry -> candidateTagKeys.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();
    }

    private int sharedTokens(ResourceCandidate source, ResourceCandidate candidate) {
        Set<String> sourceTokens = tokens(source.title() + " " + source.description());
        Set<String> candidateTokens = tokens(candidate.title() + " " + candidate.description());
        sourceTokens.retainAll(candidateTokens);
        return sourceTokens.size();
    }

    private Set<String> tokens(String text) {
        Set<String> tokens = new LinkedHashSet<>();
        for (String part : TOKEN_SPLITTER.split(value(text).toLowerCase(Locale.ROOT))) {
            String token = part.trim();
            if (token.length() >= 2) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private Map<String, String> tags(String tags) {
        Map<String, String> parsed = new LinkedHashMap<>();
        for (String part : TAG_SPLITTER.split(value(tags))) {
            String display = part.trim();
            if (!display.isBlank()) {
                parsed.putIfAbsent(display.toLowerCase(Locale.ROOT), display);
            }
        }
        return parsed;
    }

    private boolean sameText(String left, String right) {
        return left != null && right != null && !left.isBlank() && left.trim().equalsIgnoreCase(right.trim());
    }

    private int safeLevel(Integer level) {
        return level == null ? 0 : Math.max(0, Math.min(level, 5));
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private record ResourceCandidate(
            ResourceFavorite.RefType type,
            Long id,
            String title,
            String description,
            String url,
            String category,
            String tags,
            Map<String, String> tagMap,
            int qualityScore
    ) {
        boolean sameIdentity(ResourceCandidate other) {
            return type == other.type && id != null && id.equals(other.id);
        }
    }

    private record ScoredCandidate(ResourceCandidate candidate, int score, String reason) {
        RelatedResourceResponse toResponse() {
            return new RelatedResourceResponse(
                    candidate.type().name(),
                    candidate.id(),
                    candidate.title(),
                    candidate.description(),
                    candidate.url(),
                    candidate.category(),
                    candidate.tags(),
                    score,
                    reason
            );
        }
    }
}
