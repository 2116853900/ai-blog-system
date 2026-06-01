package com.aiblog.dto;

import java.util.List;

public class AdminForumBatchRequest {
    private List<Long> ids;
    private String reason;

    public List<Long> getIds() { return ids; }
    public void setIds(List<Long> ids) { this.ids = ids; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
