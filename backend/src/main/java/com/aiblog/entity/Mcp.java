package com.aiblog.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "mcp", indexes = {
        @Index(name = "idx_mcp_created", columnList = "createdAt")
})
public class Mcp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    /** 仓库地址 */
    private String repoUrl;

    /** 安装命令 */
    @Column(length = 1000)
    private String installCmd;

    /** 逗号分隔标签 */
    private String tags;

    private String category;

    private Integer recommendLevel = 3;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRepoUrl() { return repoUrl; }
    public void setRepoUrl(String repoUrl) { this.repoUrl = repoUrl; }
    public String getInstallCmd() { return installCmd; }
    public void setInstallCmd(String installCmd) { this.installCmd = installCmd; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Integer getRecommendLevel() { return recommendLevel; }
    public void setRecommendLevel(Integer recommendLevel) { this.recommendLevel = recommendLevel; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
