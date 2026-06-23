package com.aiblog.service;

import com.aiblog.dto.ResourceTagSummaryResponse;
import com.aiblog.entity.ApiStation;
import com.aiblog.entity.Mcp;
import com.aiblog.entity.Post;
import com.aiblog.entity.Skill;
import com.aiblog.repository.ApiStationRepository;
import com.aiblog.repository.McpRepository;
import com.aiblog.repository.PostRepository;
import com.aiblog.repository.SkillRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ResourceTagService {

    private final PostRepository postRepo;
    private final SkillRepository skillRepo;
    private final McpRepository mcpRepo;
    private final ApiStationRepository apiStationRepo;

    public ResourceTagService(PostRepository postRepo,
                              SkillRepository skillRepo,
                              McpRepository mcpRepo,
                              ApiStationRepository apiStationRepo) {
        this.postRepo = postRepo;
        this.skillRepo = skillRepo;
        this.mcpRepo = mcpRepo;
        this.apiStationRepo = apiStationRepo;
    }

    public List<ResourceTagSummaryResponse> postPopularTags(int limit) {
        return popularTags(postRepo.findByPublishedTrueOrderByCreatedAtDesc().stream().map(Post::getTags).toList(), limit);
    }

    public List<ResourceTagSummaryResponse> skillPopularTags(int limit) {
        return popularTags(skillRepo.findAll().stream().map(Skill::getTags).toList(), limit);
    }

    public List<ResourceTagSummaryResponse> mcpPopularTags(int limit) {
        return popularTags(mcpRepo.findAll().stream().map(Mcp::getTags).toList(), limit);
    }

    public List<ResourceTagSummaryResponse> apiStationPopularTags(int limit) {
        return popularTags(apiStationRepo.findAll().stream().map(ApiStation::getTags).toList(), limit);
    }

    static List<ResourceTagSummaryResponse> popularTags(Collection<String> tagTexts, int limit) {
        int cappedLimit = Math.max(1, Math.min(limit, 50));
        Map<String, TagCounter> counters = new LinkedHashMap<>();

        for (String tagText : tagTexts) {
            if (tagText == null || tagText.isBlank()) {
                continue;
            }
            for (String rawTag : tagText.split("[,，]")) {
                String tag = rawTag.trim();
                if (tag.isBlank()) {
                    continue;
                }
                String key = tag.toLowerCase(Locale.ROOT);
                counters.computeIfAbsent(key, ignored -> new TagCounter(tag)).increment();
            }
        }

        return counters.values().stream()
                .sorted(Comparator.comparingLong(TagCounter::count).reversed()
                        .thenComparing(counter -> counter.tag().toLowerCase(Locale.ROOT)))
                .limit(cappedLimit)
                .map(counter -> new ResourceTagSummaryResponse(counter.tag(), counter.count()))
                .toList();
    }

    private static final class TagCounter {
        private final String tag;
        private long count;

        private TagCounter(String tag) {
            this.tag = tag;
        }

        private void increment() {
            count++;
        }

        private String tag() {
            return tag;
        }

        private long count() {
            return count;
        }
    }
}
