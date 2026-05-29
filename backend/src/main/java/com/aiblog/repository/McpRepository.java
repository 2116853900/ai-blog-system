package com.aiblog.repository;

import com.aiblog.entity.Mcp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface McpRepository extends JpaRepository<Mcp, Long>, JpaSpecificationExecutor<Mcp> {
}
