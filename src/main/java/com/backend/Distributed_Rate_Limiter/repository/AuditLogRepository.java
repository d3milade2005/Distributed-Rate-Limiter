package com.backend.Distributed_Rate_Limiter.repository;

import com.backend.Distributed_Rate_Limiter.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    List<AuditLog> findByTenantIdAndUserIdOrderByCreatedAtDesc(String tenantId, String userId);
}
