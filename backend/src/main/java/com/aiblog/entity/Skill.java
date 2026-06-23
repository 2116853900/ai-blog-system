package com.aiblog.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "skill", indexes = {
        @Index(name = "idx_skill_created", columnList = "createdAt")
})
public class Skill implements com.aiblog.service.ResourceReviewBatchAggregator.ReviewRatingTarget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    /** 官网/文档链接 */
    private String link;

    /** 逗号分隔标签 */
    private String tags;

    private String category;

    /** 推荐星级 1-5 */
    private Integer recommendLevel = 3;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Transient
    private double averageRating;

    @Transient
    private long reviewCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Integer getRecommendLevel() { return recommendLevel; }
    public void setRecommendLevel(Integer recommendLevel) { this.recommendLevel = recommendLevel; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public long getReviewCount() { return reviewCount; }
    public void setReviewCount(long reviewCount) { this.reviewCount = reviewCount; }

    @Override
    public Long getReviewRefId() { return id; }
}
