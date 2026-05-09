package com.backend.Distributed_Rate_Limiter.service;

import com.backend.Distributed_Rate_Limiter.dto.TenantConfig;
import com.backend.Distributed_Rate_Limiter.entity.Plan;
import com.backend.Distributed_Rate_Limiter.entity.TenantPlan;
import com.backend.Distributed_Rate_Limiter.repository.PlanRepository;
import com.backend.Distributed_Rate_Limiter.repository.TenantPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantConfigService {

    private final TenantPlanRepository tenantPlanRepository;
    private final PlanRepository planRepository;

    @Cacheable(value = "tenant-configs", key = "#tenantId")
    public TenantConfig getConfig(String tenantId) {
        log.info("Cache miss for tenant: {} - fetching from db", tenantId);

        TenantPlan tenantPlan = tenantPlanRepository
                .findByTenantIdWithPlan(tenantId)
                .orElseThrow(() -> new RuntimeException(
                        "Tenant not found: " + tenantId
                ));

        return mapToConfig(tenantPlan);
    }

    @CacheEvict(value = "tenant-configs", key = "#tenantId")
    public void evictConfig(String tenantId) {
        log.info("Evicting cache for tenant: {}", tenantId);
    }


    @CacheEvict(value = "tenant-configs", key = "#tenantId")
    public void updateTenantPlan(String tenantId, String newPlanName) {
        log.info("Updating plan for tenant: {} to {}", tenantId, newPlanName);

        TenantPlan tenantPlan = tenantPlanRepository
                .findByTenantIdWithPlan(tenantId)
                .orElseThrow(() -> new RuntimeException(
                        "Tenant not found: " + tenantId
                ));

        Plan newPlan = planRepository
                .findById(newPlanName)
                .orElseThrow(() -> new RuntimeException(
                        "Plan not found: " + newPlanName
                ));

        tenantPlan.setPlan(newPlan);

        tenantPlanRepository.save(tenantPlan);

        log.info("Tenant: {} successfully upgraded to plan: {}", tenantId, newPlanName);
    }


    private TenantConfig mapToConfig(TenantPlan tenantPlan) {
        return new TenantConfig(
                tenantPlan.getTenantId(),
                tenantPlan.getPlan().getCapacity(),
                tenantPlan.getPlan().getRefillRate(),
                tenantPlan.getPlan().getWindowMs(),
                tenantPlan.getFailBehavior()
        );
    }

}
