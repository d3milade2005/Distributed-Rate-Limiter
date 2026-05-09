package com.backend.Distributed_Rate_Limiter.repository;

import com.backend.Distributed_Rate_Limiter.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanRepository extends JpaRepository<Plan, String> {
}
