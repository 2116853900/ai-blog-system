package com.aiblog.service;

import com.aiblog.entity.ApiStation;
import com.aiblog.repository.ApiStationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class StatusCheckService {

    private static final Logger log = LoggerFactory.getLogger(StatusCheckService.class);

    private final ApiStationRepository repo;
    private final ApiStationStatusHistoryService historyService;
    private final boolean enabled;
    private final int timeoutMs;
    private final HttpClient client;

    public StatusCheckService(ApiStationRepository repo,
                              ApiStationStatusHistoryService historyService,
                              @Value("${app.status-check.enabled:true}") boolean enabled,
                              @Value("${app.status-check.timeout-ms:8000}") int timeoutMs) {
        this.repo = repo;
        this.historyService = historyService;
        this.enabled = enabled;
        this.timeoutMs = timeoutMs;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /** 定时探测所有 API 站点 */
    @Scheduled(cron = "${app.status-check.cron:0 */10 * * * *}")
    public void checkAll() {
        if (!enabled) return;
        List<ApiStation> stations = repo.findAll();
        log.info("开始检测 {} 个 API 站点状态", stations.size());
        for (ApiStation s : stations) {
            checkAndSave(s);
        }
    }

    /** 检测单个站点并持久化结果 */
    public ApiStation checkAndSave(ApiStation s) {
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
        return saved;
    }
}
