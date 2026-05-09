package com.backend.Distributed_Rate_Limiter.repository;

import com.backend.Distributed_Rate_Limiter.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {
}
