package com.aiblog.service;

import com.aiblog.dto.ApiStationHealthDashboardResponse;
import com.aiblog.dto.ApiStationHealthDashboardResponse.RecentFailure;
import com.aiblog.dto.ApiStationHealthDashboardResponse.StationHealth;
import com.aiblog.dto.ApiStationHealthTrendResponse;
import com.aiblog.dto.ApiStationHealthTrendResponse.Incident;
import com.aiblog.dto.ApiStationHealthTrendResponse.TrendBucket;
import com.aiblog.dto.ApiStationStatusCheckResponse;
import com.aiblog.dto.ApiStationStatusSummaryResponse;
import com.aiblog.entity.ApiStation;
import com.aiblog.entity.ApiStationStatusCheck;
import com.aiblog.repository.ApiStationRepository;
import com.aiblog.repository.ApiStationStatusCheckRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    @Transactional(readOnly = true)
    public Optional<ApiStationStatusSummaryResponse> summary(Long stationId, int limit) {
        if (!stationRepo.existsById(stationId)) {
            return Optional.empty();
        }
        List<ApiStationStatusCheck> checks = checkRepo.findByStationIdOrderByCheckedAtDesc(
                stationId,
                PageRequest.of(0, normalizeLimit(limit))
        );
        ApiStationHealthAnalyzer.HealthStats stats = ApiStationHealthAnalyzer.summarize(checks);

        return Optional.of(new ApiStationStatusSummaryResponse(
                stationId,
                stats.sampleSize(),
                stats.upCount(),
                stats.downCount(),
                stats.unknownCount(),
                stats.uptimeRate(),
                stats.averageLatencyMs(),
                stats.fastestLatencyMs(),
                stats.slowestLatencyMs(),
                stats.firstCheckedAt(),
                stats.lastCheckedAt(),
                stats.longestFailureStreak(),
                stats.currentStatus()
        ));
    }

    @Transactional(readOnly = true)
    public ApiStationHealthDashboardResponse healthDashboard(int sampleLimit, int failureLimit) {
        List<ApiStation> stations = stationRepo.findAll();
        int normalizedSampleLimit = normalizeLimit(sampleLimit);
        int normalizedFailureLimit = normalizeLimit(failureLimit);
        Map<Long, ApiStation> stationsById = stations.stream()
                .filter(station -> station.getId() != null)
                .collect(Collectors.toMap(ApiStation::getId, Function.identity()));

        List<StationHealth> stationHealth = stations.stream()
                .map(station -> toStationHealth(
                        station,
                        checkRepo.findByStationIdOrderByCheckedAtDesc(
                                station.getId(),
                                PageRequest.of(0, normalizedSampleLimit))))
                .sorted(Comparator.comparingInt((StationHealth item) -> ApiStationHealthAnalyzer.healthRank(item.healthLevel()))
                        .thenComparing(StationHealth::name, String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<RecentFailure> recentFailures = checkRepo.findByStatusNotOrderByCheckedAtDesc(
                        ApiStation.Status.UP,
                        PageRequest.of(0, normalizedFailureLimit))
                .stream()
                .filter(check -> stationsById.containsKey(check.getStationId()))
                .map(check -> toRecentFailure(check, stationsById.get(check.getStationId())))
                .toList();

        long upCount = stations.stream().filter(station -> station.getStatus() == ApiStation.Status.UP).count();
        long downCount = stations.stream().filter(station -> station.getStatus() == ApiStation.Status.DOWN).count();
        long unknownCount = stations.stream().filter(station -> station.getStatus() == ApiStation.Status.UNKNOWN).count();
        ApiStationHealthAnalyzer.HealthStats stationStats = ApiStationHealthAnalyzer.summarize(stations.stream()
                .map(this::stationSnapshotCheck)
                .toList());

        return new ApiStationHealthDashboardResponse(
                Instant.now(),
                stations.size(),
                Math.toIntExact(upCount),
                Math.toIntExact(downCount),
                Math.toIntExact(unknownCount),
                stations.isEmpty() ? 0 : (double) upCount / stations.size(),
                stationStats.averageLatencyMs(),
                stationHealth,
                recentFailures
        );
    }

    @Transactional(readOnly = true)
    public ApiStationHealthTrendResponse healthTrends(int days, int incidentLimit) {
        int normalizedDays = normalizeDays(days);
        int normalizedIncidentLimit = normalizeLimit(incidentLimit);
        Instant endAt = Instant.now();
        Instant startAt = endAt.minus(normalizedDays - 1L, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
        List<ApiStationStatusCheck> checks = checkRepo.findByCheckedAtGreaterThanEqualOrderByCheckedAtAsc(startAt);
        Map<Long, ApiStation> stationsById = stationRepo.findAll().stream()
                .filter(station -> station.getId() != null)
                .collect(Collectors.toMap(ApiStation::getId, Function.identity()));

        List<TrendBucket> buckets = buildTrendBuckets(startAt, normalizedDays, checks);
        List<Incident> incidents = buildIncidents(checks, stationsById, normalizedIncidentLimit, endAt);
        return new ApiStationHealthTrendResponse(endAt, normalizedDays, startAt, endAt, buckets, incidents);
    }

    private int normalizeLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private int normalizeDays(int days) {
        if (days < 1) {
            return 1;
        }
        return Math.min(days, 30);
    }

    private List<TrendBucket> buildTrendBuckets(Instant startAt, int days, List<ApiStationStatusCheck> checks) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate startDate = LocalDate.ofInstant(startAt, zone);
        Map<LocalDate, List<ApiStationStatusCheck>> checksByDate = checks.stream()
                .collect(Collectors.groupingBy(check -> LocalDate.ofInstant(check.getCheckedAt(), zone)));
        List<TrendBucket> buckets = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            ApiStationHealthAnalyzer.HealthStats stats = ApiStationHealthAnalyzer.summarize(
                    checksByDate.getOrDefault(date, List.of())
            );
            buckets.add(new TrendBucket(
                    date,
                    stats.sampleSize(),
                    stats.upCount(),
                    stats.downCount(),
                    stats.unknownCount(),
                    stats.uptimeRate(),
                    stats.averageLatencyMs()
            ));
        }
        return buckets;
    }

    private List<Incident> buildIncidents(List<ApiStationStatusCheck> checks,
                                          Map<Long, ApiStation> stationsById,
                                          int limit,
                                          Instant now) {
        Map<Long, IncidentBuilder> activeIncidents = new HashMap<>();
        List<Incident> incidents = new ArrayList<>();

        for (ApiStationStatusCheck check : checks) {
            ApiStation station = stationsById.get(check.getStationId());
            if (station == null) {
                continue;
            }

            if (check.getStatus() == ApiStation.Status.UP) {
                IncidentBuilder active = activeIncidents.remove(check.getStationId());
                if (active != null) {
                    incidents.add(active.resolve(check.getCheckedAt()));
                }
                continue;
            }

            if (check.getStatus() != ApiStation.Status.DOWN) {
                continue;
            }

            IncidentBuilder active = activeIncidents.get(check.getStationId());
            if (active == null) {
                activeIncidents.put(check.getStationId(), new IncidentBuilder(station, check));
            } else {
                active.add(check);
            }
        }

        for (IncidentBuilder active : activeIncidents.values()) {
            incidents.add(active.open(now));
        }

        return incidents.stream()
                .sorted(Comparator.comparing(Incident::startedAt).reversed())
                .limit(limit)
                .toList();
    }

    private StationHealth toStationHealth(ApiStation station, List<ApiStationStatusCheck> checks) {
        ApiStationHealthAnalyzer.HealthStats stats = ApiStationHealthAnalyzer.summarize(checks);
        String healthLevel = ApiStationHealthAnalyzer.classifyHealth(station.getStatus(), stats.uptimeRate(), stats.averageLatencyMs());
        return new StationHealth(
                station.getId(),
                station.getName(),
                station.getBaseUrl(),
                station.getStatus(),
                station.getLatencyMs(),
                station.getLastCheckedAt(),
                stats.sampleSize(),
                stats.upCount(),
                stats.downCount(),
                stats.unknownCount(),
                stats.uptimeRate(),
                stats.averageLatencyMs(),
                stats.longestFailureStreak(),
                healthLevel
        );
    }

    private ApiStationStatusCheck stationSnapshotCheck(ApiStation station) {
        ApiStationStatusCheck check = new ApiStationStatusCheck();
        check.setStationId(station.getId());
        check.setStatus(station.getStatus());
        check.setLatencyMs(station.getLatencyMs());
        check.setCheckedAt(station.getLastCheckedAt() != null ? station.getLastCheckedAt() : Instant.EPOCH);
        return check;
    }

    private RecentFailure toRecentFailure(ApiStationStatusCheck check, ApiStation station) {
        return new RecentFailure(
                station.getId(),
                station.getName(),
                check.getStatus(),
                check.getCheckedAt(),
                check.getErrorMessage()
        );
    }

    private String truncate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= ERROR_MAX_LENGTH ? value : value.substring(0, ERROR_MAX_LENGTH);
    }

    private static final class IncidentBuilder {
        private final ApiStation station;
        private final Instant startedAt;
        private int failureCount;
        private String latestErrorMessage;

        private IncidentBuilder(ApiStation station, ApiStationStatusCheck firstFailure) {
            this.station = station;
            this.startedAt = firstFailure.getCheckedAt();
            add(firstFailure);
        }

        private void add(ApiStationStatusCheck failure) {
            failureCount++;
            if (failure.getErrorMessage() != null && !failure.getErrorMessage().isBlank()) {
                latestErrorMessage = failure.getErrorMessage();
            }
        }

        private Incident resolve(Instant endedAt) {
            return toIncident(endedAt, true);
        }

        private Incident open(Instant now) {
            return toIncident(now, false);
        }

        private Incident toIncident(Instant endAt, boolean resolved) {
            Instant incidentEnd = resolved ? endAt : null;
            long durationMinutes = Math.max(0, ChronoUnit.MINUTES.between(startedAt, endAt));
            return new Incident(
                    station.getId(),
                    station.getName(),
                    startedAt,
                    incidentEnd,
                    durationMinutes,
                    failureCount,
                    latestErrorMessage,
                    resolved
            );
        }
    }
}
