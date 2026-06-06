package com.aiblog.service;

import com.aiblog.entity.AdminOperationLog;
import com.aiblog.repository.AdminOperationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOperationLogServiceTest {

    @Mock
    private AdminOperationLogRepository operationLogRepo;

    private AdminOperationLogService service;

    @BeforeEach
    void setUp() {
        service = new AdminOperationLogService(operationLogRepo);
    }

    @Test
    void searchDelegatesToRepositoryWithPageable() {
        var pageable = PageRequest.of(1, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        var log = new AdminOperationLog();
        log.setId(10L);
        when(operationLogRepo.findAll(anyLogSpec(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(log), pageable, 1));

        var result = service.search(
                " admin ",
                "hide",
                "comment",
                33L,
                Instant.parse("2026-06-01T00:00:00Z"),
                Instant.parse("2026-06-02T00:00:00Z"),
                pageable);

        assertThat(result.getContent()).containsExactly(log);
        verify(operationLogRepo).findAll(anyLogSpec(), eq(pageable));
    }

    @SuppressWarnings("unchecked")
    private Specification<AdminOperationLog> anyLogSpec() {
        return org.mockito.ArgumentMatchers.any(Specification.class);
    }
}
