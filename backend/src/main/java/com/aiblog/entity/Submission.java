package com.aiblog.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "submission", indexes = {
        @Index(name = "idx_submission_status_created", columnList = "status,createdAt"),
        @Index(name = "idx_submission_created", columnList = "createdAt")
})
public class Submission {
    public enum Type { SKILL, MCP, API }
    public enum Status { PENDING, APPROVED, REJECTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    /** 投稿内容的 JSON 负载 */
    @Column(columnDefinition = "TEXT")
    private String payloadJson;

    private String contactInfo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
