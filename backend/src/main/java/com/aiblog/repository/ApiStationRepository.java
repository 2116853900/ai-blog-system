package com.aiblog.repository;

import com.aiblog.entity.ApiStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ApiStationRepository extends JpaRepository<ApiStation, Long>, JpaSpecificationExecutor<ApiStation> {
}
