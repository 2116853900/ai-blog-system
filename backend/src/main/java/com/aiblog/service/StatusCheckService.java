package com.aiblog.service;

import com.aiblog.cache.PublicContentCacheService;
import com.aiblog.entity.ApiStation;
import com.aiblog.repository.ApiStationRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class StatusCheckService {

    private static final Logger log = LoggerFactory.getLogger(StatusCheckService.class);

    private final ApiStationRepository repo;
    private final ApiStationStatusHistoryService historyService;
    private final boolean enabled;
    private final int timeoutMs;
    private final int maxConcurrency;
    private final HttpClient client;
    private final PublicContentCacheService cacheService;
    private final MeterRegistry meterRegistry;

    @Autowired
    public StatusCheckService(ApiStationRepository repo,
                              ApiStationStatusHistoryService historyService,
                              @Value("${app.status-check.enabled:true}") boolean enabled,
                              @Value("${app.status-check.timeout-ms:8000}") int timeoutMs,
                              @Value("${app.status-check.max-concurrency:8}") int maxConcurrency,
                              PublicContentCacheService cacheService,
                              MeterRegistry meterRegistry) {
        this(repo,
                historyService,
                enabled,
                timeoutMs,
                maxConcurrency,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofMillis(timeoutMs))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build(),
                cacheService,
                meterRegistry);
    }

    StatusCheckService(ApiStationRepository repo,
                       ApiStationStatusHistoryService historyService,
                       boolean enabled,
                       int timeoutMs,
                       int maxConcurrency,
                       HttpClient client) {
        this(repo, historyService, enabled, timeoutMs, maxConcurrency, client, null, new SimpleMeterRegistry());
    }

    StatusCheckService(ApiStationRepository repo,
                       ApiStationStatusHistoryService historyService,
                       boolean enabled,
                       int timeoutMs,
                       int maxConcurrency,
                       HttpClient client,
                       MeterRegistry meterRegistry) {
        this(repo, historyService, enabled, timeoutMs, maxConcurrency, client, null, meterRegistry);
    }

    StatusCheckService(ApiStationRepository repo,
                       ApiStationStatusHistoryService historyService,
                       boolean enabled,
                       int timeoutMs,
                       int maxConcurrency,
                       HttpClient client,
                       PublicContentCacheService cacheService,
                       MeterRegistry meterRegistry) {
        this.repo = repo;
        this.historyService = historyService;
        this.enabled = enabled;
        this.timeoutMs = timeoutMs;
        this.maxConcurrency = Math.max(1, maxConcurrency);
        this.client = client;
        this.cacheService = cacheService;
        this.meterRegistry = meterRegistry;
    }

    /** 定时探测所有 API 站点 */
    @Scheduled(cron = "${app.status-check.cron:0 */10 * * * *}")
    public void checkAll() {
        if (!enabled) return;
        List<ApiStation> stations = repo.findAll();
        if (stations.isEmpty()) {
            log.info("没有 API 站点需要检测");
            return;
        }
        int poolSize = Math.min(maxConcurrency, stations.size());
        log.info("开始检测 {} 个 API 站点状态，并发数 {}", stations.size(), poolSize);

        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (ApiStation station : stations) {
                futures.add(executor.submit(() -> {
                    try {
                        checkAndSave(station);
                    } catch (Exception e) {
                        log.warn("站点 {} 检测任务失败: {}", station.getName(), e.getMessage());
                    }
                }));
            }
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("API 站点状态检测被中断");
                    break;
                } catch (ExecutionException e) {
                    log.warn("API 站点状态检测任务异常: {}", e.getMessage());
                }
            }
        } finally {
            executor.shutdownNow();
        }
    }

    /** 检测单个站点并持久化结果 */
    public ApiStation checkAndSave(ApiStation s) {
        Timer.Sample sample = Timer.start(meterRegistry);
        long start = System.currentTimeMillis();
        ApiStation.Status status = ApiStation.Status.DOWN;
        Integer latency = null;
        String errorMessage = null;
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(s.getBaseUrl()))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<Void> resp = client.send(req, HttpResponse.BodyHandlers.discarding());
            latency = (int) (System.currentTimeMillis() - start);
            // 任何 HTTP 响应（含 4xx/401，常见于需鉴权的 API 根路径）都视为站点在线
            if (resp.statusCode() < 500) {
                status = ApiStation.Status.UP;
            } else {
                errorMessage = "HTTP " + resp.statusCode();
            }
        } catch (Exception e) {
            // 部分服务器不支持 HEAD，回退到 GET 再试一次
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(s.getBaseUrl()))
                        .timeout(Duration.ofMillis(timeoutMs))
                        .GET()
                        .build();
                HttpResponse<Void> resp = client.send(req, HttpResponse.BodyHandlers.discarding());
                latency = (int) (System.currentTimeMillis() - start);
                if (resp.statusCode() < 500) {
                    status = ApiStation.Status.UP;
                    errorMessage = null;
                } else {
                    errorMessage = "HTTP " + resp.statusCode();
                }
            } catch (Exception ex) {
                errorMessage = ex.getMessage();
                log.debug("站点 {} 检测失败: {}", s.getName(), ex.getMessage());
            }
        }
        s.setStatus(status);
        s.setLatencyMs(latency);
        s.setLastCheckedAt(Instant.now());
        ApiStation saved = repo.save(s);
        historyService.record(saved, errorMessage);
        if (cacheService != null) {
            cacheService.evictApiStations();
        }
        recordStatusCheck(status, sample);
        return saved;
    }

    private void recordStatusCheck(ApiStation.Status status, Timer.Sample sample) {
        meterRegistry.counter("aiblog.api_station.status_check.results", "status", status.name()).increment();
        sample.stop(meterRegistry.timer("aiblog.api_station.status_check.duration", "status", status.name()));
    }
}
