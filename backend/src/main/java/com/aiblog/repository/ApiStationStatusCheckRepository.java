package com.aiblog.repository;

import com.aiblog.entity.ApiStationStatusCheck;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApiStationStatusCheckRepository extends JpaRepository<ApiStationStatusCheck, Long> {
    List<ApiStationStatusCheck> findByStationIdOrderByCheckedAtDesc(Long stationId, Pageable pageable);
}
