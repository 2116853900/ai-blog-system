package com.aiblog.service;

import com.aiblog.dto.ResourceFavoriteInteractionResponse;
import com.aiblog.dto.ResourceFavoriteItemResponse;
import com.aiblog.entity.ApiStation;
import com.aiblog.entity.Mcp;
import com.aiblog.entity.Post;
import com.aiblog.entity.ResourceFavorite;
import com.aiblog.entity.Skill;
import com.aiblog.repository.ApiStationRepository;
import com.aiblog.repository.McpRepository;
import com.aiblog.repository.PostRepository;
import com.aiblog.repository.ResourceFavoriteRepository;
import com.aiblog.repository.SkillRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResourceFavoriteService {

    private final ResourceFavoriteRepository favoriteRepo;
    private final PostRepository postRepo;
    private final SkillRepository skillRepo;
    private final McpRepository mcpRepo;
    private final ApiStationRepository apiRepo;

    public ResourceFavoriteService(ResourceFavoriteRepository favoriteRepo,
                                   PostRepository postRepo,
                                   SkillRepository skillRepo,
                                   McpRepository mcpRepo,
                                   ApiStationRepository apiRepo) {
        this.favoriteRepo = favoriteRepo;
        this.postRepo = postRepo;
        this.skillRepo = skillRepo;
        this.mcpRepo = mcpRepo;
        this.apiRepo = apiRepo;
    }

    @Transactional(readOnly = true)
    public ResourceFavoriteInteractionResponse getInteraction(ResourceFavorite.RefType refType, Long refId, Long userId) {
        validateTarget(refType, refId);
        boolean favorited = userId != null && favoriteRepo.existsByUserIdAndRefTypeAndRefId(userId, refType, refId);
        return new ResourceFavoriteInteractionResponse(favorited, toIntCount(favoriteRepo.countByRefTypeAndRefId(refType, refId)));
    }

    @Transactional
    public ResourceFavoriteInteractionResponse favorite(ResourceFavorite.RefType refType, Long refId, Long userId) {
        validateTarget(refType, refId);
        favoriteRepo.insertIgnore(userId, refType.name(), refId);
        return getInteraction(refType, refId, userId);
    }

    @Transactional
    public ResourceFavoriteInteractionResponse unfavorite(ResourceFavorite.RefType refType, Long refId, Long userId) {
        validateTarget(refType, refId);
        favoriteRepo.deleteByUserIdAndRefTypeAndRefId(userId, refType, refId);
        return getInteraction(refType, refId, userId);
    }

    @Transactional(readOnly = true)
    public Page<ResourceFavoriteItemResponse> listFavorites(Long userId, Pageable pageable) {
        return favoriteRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(this::toItem);
    }

    private void validateTarget(ResourceFavorite.RefType refType, Long refId) {
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

    private ResourceFavoriteItemResponse toItem(ResourceFavorite favorite) {
        return switch (favorite.getRefType()) {
            case POST -> postRepo.findById(favorite.getRefId())
                    .filter(Post::isPublished)
                    .map(post -> fromPost(favorite, post))
                    .orElseGet(() -> missing(favorite));
            case SKILL -> skillRepo.findById(favorite.getRefId())
                    .map(skill -> fromSkill(favorite, skill))
                    .orElseGet(() -> missing(favorite));
            case MCP -> mcpRepo.findById(favorite.getRefId())
                    .map(mcp -> fromMcp(favorite, mcp))
                    .orElseGet(() -> missing(favorite));
            case API -> apiRepo.findById(favorite.getRefId())
                    .map(station -> fromApiStation(favorite, station))
                    .orElseGet(() -> missing(favorite));
        };
    }

    private ResourceFavoriteItemResponse fromPost(ResourceFavorite favorite, Post post) {
        return new ResourceFavoriteItemResponse(
                favorite.getId(),
                favorite.getRefType(),
                favorite.getRefId(),
                post.getTitle(),
                post.getSummary(),
                "/tutorials/" + post.getSlug(),
                post.getCategory(),
                post.getTags(),
                true,
                favorite.getCreatedAt()
        );
    }

    private ResourceFavoriteItemResponse fromSkill(ResourceFavorite favorite, Skill skill) {
        return new ResourceFavoriteItemResponse(
                favorite.getId(),
                favorite.getRefType(),
                favorite.getRefId(),
                skill.getName(),
                skill.getDescription(),
                urlFor(favorite.getRefType(), favorite.getRefId()),
                skill.getCategory(),
                skill.getTags(),
                true,
                favorite.getCreatedAt()
        );
    }

    private ResourceFavoriteItemResponse fromMcp(ResourceFavorite favorite, Mcp mcp) {
        return new ResourceFavoriteItemResponse(
                favorite.getId(),
                favorite.getRefType(),
                favorite.getRefId(),
                mcp.getName(),
                mcp.getDescription(),
                urlFor(favorite.getRefType(), favorite.getRefId()),
                mcp.getCategory(),
                mcp.getTags(),
                true,
                favorite.getCreatedAt()
        );
    }

    private ResourceFavoriteItemResponse fromApiStation(ResourceFavorite favorite, ApiStation station) {
        return new ResourceFavoriteItemResponse(
                favorite.getId(),
                favorite.getRefType(),
                favorite.getRefId(),
                station.getName(),
                station.getDescription(),
                urlFor(favorite.getRefType(), favorite.getRefId()),
                null,
                station.getTags(),
                true,
                favorite.getCreatedAt()
        );
    }

    private ResourceFavoriteItemResponse missing(ResourceFavorite favorite) {
        return new ResourceFavoriteItemResponse(
                favorite.getId(),
                favorite.getRefType(),
                favorite.getRefId(),
                "资源已下架",
                "该资源已不存在或已被删除。",
                urlFor(favorite.getRefType(), favorite.getRefId()),
                null,
                null,
                false,
                favorite.getCreatedAt()
        );
    }

    private String urlFor(ResourceFavorite.RefType refType, Long refId) {
        return switch (refType) {
            case POST -> "/tutorials";
            case SKILL -> "/skills/" + refId;
            case MCP -> "/mcps/" + refId;
            case API -> "/api-stations/" + refId;
        };
    }

    private int toIntCount(long count) {
        return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
    }
}
