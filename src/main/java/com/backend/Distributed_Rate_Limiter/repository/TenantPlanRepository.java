package com.backend.Distributed_Rate_Limiter.repository;


import com.backend.Distributed_Rate_Limiter.entity.TenantPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantPlanRepository extends JpaRepository<TenantPlan, String> {
}
