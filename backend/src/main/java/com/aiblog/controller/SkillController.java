package com.aiblog.controller;

import com.aiblog.cache.PublicContentCacheService;
import com.aiblog.dto.ResourceTagSummaryResponse;
import com.aiblog.entity.ResourceReview;
import com.aiblog.entity.Skill;
import com.aiblog.service.ResourceReviewBatchAggregator;
import com.aiblog.repository.SkillRepository;
import com.aiblog.service.ResourceTagService;
import com.aiblog.service.SearchSpecs;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
public class SkillController {

    private final SkillRepository repo;
    private final PublicContentCacheService cacheService;
    private final ResourceTagService tagService;
    private final ResourceReviewBatchAggregator reviewBatch;

    public SkillController(SkillRepository repo,
                           PublicContentCacheService cacheService,
                           ResourceTagService tagService,
                           ResourceReviewBatchAggregator reviewBatch) {
        this.repo = repo;
        this.cacheService = cacheService;
        this.tagService = tagService;
        this.reviewBatch = reviewBatch;
    }

    @GetMapping
    public List<Skill> list(@RequestParam(required = false) String q,
                            @RequestParam(required = false) String tag,
                            @RequestParam(required = false) String category) {
        List<Skill> skills = cacheService.publicContent(
                cacheService.skillsListKey(q, tag, category),
                new TypeReference<List<Skill>>() {},
                () -> repo.findAll(
                        SearchSpecs.build(q, tag, category, List.of("name", "description", "tags")),
                        Sort.by(Sort.Direction.DESC, "recommendLevel").and(Sort.by(Sort.Direction.DESC, "createdAt"))));
        reviewBatch.apply(ResourceReview.RefType.SKILL, skills);
        return skills;
    }

    @GetMapping("/tags/popular")
    public List<ResourceTagSummaryResponse> popularTags(@RequestParam(defaultValue = "20") int limit) {
        return cacheService.publicContent(
                cacheService.skillsPopularTagsKey(limit),
                new TypeReference<List<ResourceTagSummaryResponse>>() {},
                () -> tagService.skillPopularTags(limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Skill> detail(@PathVariable Long id) {
        Skill skill = cacheService.publicContent(cacheService.skillDetailKey(id), Skill.class, () -> repo.findById(id).orElse(null));
        return skill == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(skill);
    }
}
