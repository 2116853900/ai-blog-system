package com.aiblog.dto;

import java.time.Instant;

public class AdminUserBanRequest {
    private String reason;
    private Instant banEndTime;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Instant getBanEndTime() { return banEndTime; }
    public void setBanEndTime(Instant banEndTime) { this.banEndTime = banEndTime; }
}
