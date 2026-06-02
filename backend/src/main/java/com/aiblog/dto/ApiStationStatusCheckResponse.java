package com.aiblog.dto;

import com.aiblog.entity.ApiStation;
import com.aiblog.entity.ApiStationStatusCheck;

import java.time.Instant;

public record ApiStationStatusCheckResponse(
        Long id,
        Long stationId,
        ApiStation.Status status,
        Integer latencyMs,
        Instant checkedAt,
        String errorMessage
) {
    public static ApiStationStatusCheckResponse from(ApiStationStatusCheck check) {
        return new ApiStationStatusCheckResponse(
                check.getId(),
                check.getStationId(),
                check.getStatus(),
                check.getLatencyMs(),
                check.getCheckedAt(),
                check.getErrorMessage()
        );
    }
}
