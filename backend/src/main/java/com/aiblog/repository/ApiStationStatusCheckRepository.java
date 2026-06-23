package com.aiblog.repository;

import com.aiblog.entity.ApiStationStatusCheck;
import com.aiblog.entity.ApiStation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ApiStationStatusCheckRepository extends JpaRepository<ApiStationStatusCheck, Long> {
    List<ApiStationStatusCheck> findByStationIdOrderByCheckedAtDesc(Long stationId, Pageable pageable);
    List<ApiStationStatusCheck> findByStatusNotOrderByCheckedAtDesc(ApiStation.Status status, Pageable pageable);
    List<ApiStationStatusCheck> findByCheckedAtGreaterThanEqualOrderByCheckedAtAsc(Instant checkedAt);
}
