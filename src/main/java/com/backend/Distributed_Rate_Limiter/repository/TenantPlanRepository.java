package com.backend.Distributed_Rate_Limiter.repository;


import com.backend.Distributed_Rate_Limiter.entity.TenantPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantPlanRepository extends JpaRepository<TenantPlan, String> {
    @Query("SELECT tp FROM TenantPlan tp JOIN FETCH tp.plan WHERE tp.tenantId = :tenantId")
    Optional<TenantPlan> findByTenantIdWithPlan(@Param("tenantId") String tenantId);
}
