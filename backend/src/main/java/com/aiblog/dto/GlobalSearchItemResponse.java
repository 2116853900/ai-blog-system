package com.aiblog.dto;

import java.time.Instant;

public class GlobalSearchItemResponse {
    private String type;
    private Long id;
    private String title;
    private String description;
    private String url;
    private String category;
    private String tags;
    private String meta;
    private Instant createdAt;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public String getMeta() { return meta; }
    public void setMeta(String meta) { this.meta = meta; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
