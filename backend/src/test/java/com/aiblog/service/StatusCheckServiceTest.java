package com.aiblog.service;

import com.aiblog.entity.ApiStation;
import com.aiblog.repository.ApiStationRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class StatusCheckServiceTest {

    @Mock
    private ApiStationRepository repo;

    @Mock
    private ApiStationStatusHistoryService historyService;

    @Mock
    private HttpClient client;

    @Mock
    private HttpResponse<Void> response;

    private SimpleMeterRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
    }

    @Test
    void checkAllDoesNotQueryWhenDisabled() {
        StatusCheckService service = new StatusCheckService(repo, historyService, false, 100, 2, client, registry);

        service.checkAll();

        verifyNoInteractions(repo, historyService, client);
    }

    @Test
    void checkAllSkipsExecutorWhenNoStationsExist() {
        StatusCheckService service = new StatusCheckService(repo, historyService, true, 100, 2, client, registry);
        when(repo.findAll()).thenReturn(List.of());

        service.checkAll();

        verify(repo).findAll();
        verifyNoInteractions(historyService, client);
    }

    @Test
    void checkAllChecksAndRecordsEveryStation() throws Exception {
        StatusCheckService service = new StatusCheckService(repo, historyService, true, 100, 2, client, registry);
        ApiStation first = station(1L, "A");
        ApiStation second = station(2L, "B");
        when(repo.findAll()).thenReturn(List.of(first, second));
        when(response.statusCode()).thenReturn(200);
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        when(repo.save(any(ApiStation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.checkAll();

        verify(repo, times(2)).save(any(ApiStation.class));
        verify(historyService, times(2)).record(any(ApiStation.class), isNull());
        verify(client, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        assertThat(registry.counter("aiblog.api_station.status_check.results", "status", "UP").count()).isEqualTo(2);
        assertThat(registry.timer("aiblog.api_station.status_check.duration", "status", "UP").count()).isEqualTo(2);
    }

    @Test
    void failedCheckStillRecordsStationSnapshot() throws Exception {
        StatusCheckService service = new StatusCheckService(repo, historyService, true, 100, 1, client, registry);
        ApiStation station = station(1L, "A");
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new RuntimeException("head failed"))
                .thenThrow(new RuntimeException("get failed"));
        when(repo.save(any(ApiStation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.checkAndSave(station);

        verify(repo).save(station);
        verify(historyService).record(any(ApiStation.class), any(String.class));
        verify(client, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        verify(repo, never()).findAll();
        assertThat(registry.counter("aiblog.api_station.status_check.results", "status", "DOWN").count()).isEqualTo(1);
        assertThat(registry.timer("aiblog.api_station.status_check.duration", "status", "DOWN").count()).isEqualTo(1);
    }

    private ApiStation station(Long id, String name) {
        ApiStation station = new ApiStation();
        station.setId(id);
        station.setName(name);
        station.setBaseUrl("https://example.com/" + name.toLowerCase());
        return station;
    }
}
