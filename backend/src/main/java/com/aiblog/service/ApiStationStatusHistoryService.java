package com.aiblog.service;

import com.aiblog.dto.ApiStationStatusCheckResponse;
import com.aiblog.entity.ApiStation;
import com.aiblog.entity.ApiStationStatusCheck;
import com.aiblog.repository.ApiStationRepository;
import com.aiblog.repository.ApiStationStatusCheckRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class ApiStationStatusHistoryService {

    private static final int MAX_LIMIT = 50;
    private static final int ERROR_MAX_LENGTH = 500;

    private final ApiStationStatusCheckRepository checkRepo;
    private final ApiStationRepository stationRepo;

    public ApiStationStatusHistoryService(ApiStationStatusCheckRepository checkRepo,
                                          ApiStationRepository stationRepo) {
        this.checkRepo = checkRepo;
        this.stationRepo = stationRepo;
    }

    @Transactional
    public ApiStationStatusCheck record(ApiStation station, String errorMessage) {
        if (station.getId() == null) {
            throw new IllegalArgumentException("API 站点尚未持久化");
        }

        ApiStationStatusCheck check = new ApiStationStatusCheck();
        check.setStationId(station.getId());
        check.setStatus(station.getStatus());
        check.setLatencyMs(station.getLatencyMs());
        check.setCheckedAt(station.getLastCheckedAt() != null ? station.getLastCheckedAt() : Instant.now());
        check.setErrorMessage(truncate(errorMessage));
        return checkRepo.save(check);
    }

    @Transactional(readOnly = true)
    public Optional<List<ApiStationStatusCheckResponse>> recent(Long stationId, int limit) {
        if (!stationRepo.existsById(stationId)) {
            return Optional.empty();
        }
        return Optional.of(checkRepo.findByStationIdOrderByCheckedAtDesc(
                        stationId,
                        PageRequest.of(0, normalizeLimit(limit))
                ).stream()
                .map(ApiStationStatusCheckResponse::from)
                .toList());
    }

    private int normalizeLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private String truncate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= ERROR_MAX_LENGTH ? value : value.substring(0, ERROR_MAX_LENGTH);
    }
}
