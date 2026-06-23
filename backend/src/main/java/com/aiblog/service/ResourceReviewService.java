package com.aiblog.service;

import com.aiblog.dto.AccountResourceReviewItemResponse;
import com.aiblog.dto.ResourceReviewRequest;
import com.aiblog.dto.ResourceReviewResponse;
import com.aiblog.dto.ResourceReviewSummaryResponse;
import com.aiblog.entity.ApiStation;
import com.aiblog.entity.Mcp;
import com.aiblog.entity.Post;
import com.aiblog.entity.ResourceReview;
import com.aiblog.entity.Skill;
import com.aiblog.repository.ApiStationRepository;
import com.aiblog.repository.McpRepository;
import com.aiblog.repository.PostRepository;
import com.aiblog.repository.ResourceReviewRepository;
import com.aiblog.repository.SkillRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.aiblog.entity.ResourceReview.ReviewStatus.NORMAL;

@Service
public class ResourceReviewService {

    private final ResourceReviewRepository reviewRepo;
    private final PostRepository postRepo;
    private final SkillRepository skillRepo;
    private final McpRepository mcpRepo;
    private final ApiStationRepository apiRepo;

    public ResourceReviewService(ResourceReviewRepository reviewRepo,
                                 PostRepository postRepo,
                                 SkillRepository skillRepo,
                                 McpRepository mcpRepo,
                                 ApiStationRepository apiRepo) {
        this.reviewRepo = reviewRepo;
        this.postRepo = postRepo;
        this.skillRepo = skillRepo;
        this.mcpRepo = mcpRepo;
        this.apiRepo = apiRepo;
    }

    @Transactional(readOnly = true)
    public ResourceReviewSummaryResponse summary(ResourceReview.RefType refType, Long refId, Long userId) {
        validateTarget(refType, refId);
        ResourceReviewResponse myReview = userId == null ? null : reviewRepo
                .findByUserIdAndRefTypeAndRefId(userId, refType, refId)
                .filter(review -> review.getStatus() == ResourceReview.ReviewStatus.NORMAL)
                .map(ResourceReviewResponse::from)
                .orElse(null);
        return new ResourceReviewSummaryResponse(
                reviewRepo.averageRating(refType, refId, ResourceReview.ReviewStatus.NORMAL),
                reviewRepo.countByRefTypeAndRefIdAndStatus(refType, refId, ResourceReview.ReviewStatus.NORMAL),
                myReview);
    }

    @Transactional(readOnly = true)
    public Page<AccountResourceReviewItemResponse> listMine(Long userId, Pageable pageable) {
        return reviewRepo.findByUserIdAndStatusOrderByUpdatedAtDesc(userId, NORMAL, pageable)
                .map(this::toAccountItem);
    }

    @Transactional(readOnly = true)
    public Page<ResourceReviewResponse> list(ResourceReview.RefType refType, Long refId, Pageable pageable) {
        validateTarget(refType, refId);
        return reviewRepo
                .findByRefTypeAndRefIdAndStatusOrderByCreatedAtDesc(
                        refType, refId, ResourceReview.ReviewStatus.NORMAL, pageable)
                .map(ResourceReviewResponse::from);
    }

    @Transactional
    public ResourceReviewResponse upsert(ResourceReview.RefType refType,
                                         Long refId,
                                         Long userId,
                                         ResourceReviewRequest request) {
        validateTarget(refType, refId);
        ResourceReview review = reviewRepo
                .findByUserIdAndRefTypeAndRefId(userId, refType, refId)
                .orElseGet(ResourceReview::new);
        review.setUserId(userId);
        review.setRefType(refType);
        review.setRefId(refId);
        review.setRating(request.getRating());
        review.setContent(clean(request.getContent()));
        review.setStatus(ResourceReview.ReviewStatus.NORMAL);
        return ResourceReviewResponse.from(reviewRepo.save(review));
    }

    @Transactional
    public boolean deleteOwn(ResourceReview.RefType refType, Long refId, Long userId) {
        validateTarget(refType, refId);
        return reviewRepo.findByUserIdAndRefTypeAndRefId(userId, refType, refId)
                .map(review -> {
                    review.setStatus(ResourceReview.ReviewStatus.DELETED);
                    reviewRepo.save(review);
                    return true;
                })
                .orElse(false);
    }

    private void validateTarget(ResourceReview.RefType refType, Long refId) {
        boolean exists = switch (refType) {
            case POST -> postRepo.findById(refId).filter(Post::isPublished).isPresent();
            case SKILL -> skillRepo.existsById(refId);
            case MCP -> mcpRepo.existsById(refId);
            case API -> apiRepo.existsById(refId);
        };
        if (!exists) {
            throw new IllegalArgumentException("资源不存在");
        }
    }

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private AccountResourceReviewItemResponse toAccountItem(ResourceReview review) {
        return new AccountResourceReviewItemResponse(
                review.getId(),
                review.getRefType(),
                review.getRefId(),
                resolveTitle(review.getRefType(), review.getRefId()),
                resolveUrl(review.getRefType(), review.getRefId()),
                review.getRating(),
                review.getContent(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }

    private String resolveTitle(ResourceReview.RefType refType, Long refId) {
        return switch (refType) {
            case POST -> postRepo.findById(refId).filter(Post::isPublished).map(Post::getTitle).orElse("教程已下架");
            case SKILL -> skillRepo.findById(refId).map(Skill::getName).orElse("Skill 已下架");
            case MCP -> mcpRepo.findById(refId).map(Mcp::getName).orElse("MCP 已下架");
            case API -> apiRepo.findById(refId).map(ApiStation::getName).orElse("API 站已下架");
        };
    }

    private String resolveUrl(ResourceReview.RefType refType, Long refId) {
        return switch (refType) {
            case POST -> postRepo.findById(refId).filter(Post::isPublished)
                    .map(p -> "/tutorials/" + p.getSlug()).orElse("/tutorials");
            case SKILL -> "/skills/" + refId;
            case MCP -> "/mcps/" + refId;
            case API -> "/api-stations/" + refId;
        };
    }
}
