package com.aiblog.dto;

import com.aiblog.entity.ResourceFavorite;

import java.time.Instant;

public class ResourceFavoriteItemResponse {
    private Long id;
    private ResourceFavorite.RefType refType;
    private Long refId;
    private String title;
    private String description;
    private String url;
    private String category;
    private String tags;
    private boolean available;
    private Instant createdAt;

    public ResourceFavoriteItemResponse(Long id,
                                        ResourceFavorite.RefType refType,
                                        Long refId,
                                        String title,
                                        String description,
                                        String url,
                                        String category,
                                        String tags,
                                        boolean available,
                                        Instant createdAt) {
        this.id = id;
        this.refType = refType;
        this.refId = refId;
        this.title = title;
        this.description = description;
        this.url = url;
        this.category = category;
        this.tags = tags;
        this.available = available;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ResourceFavorite.RefType getRefType() { return refType; }
    public void setRefType(ResourceFavorite.RefType refType) { this.refType = refType; }

    public Long getRefId() { return refId; }
    public void setRefId(Long refId) { this.refId = refId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
