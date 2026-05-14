package com.backend.Distributed_Rate_Limiter.controller;

import com.backend.Distributed_Rate_Limiter.dto.CreateTenantRequest;
import com.backend.Distributed_Rate_Limiter.dto.TenantConfig;
import com.backend.Distributed_Rate_Limiter.service.TenantConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
@Slf4j
public class TenantController {
    private final TenantConfigService tenantConfigService;

    @PostMapping
    public ResponseEntity<TenantConfig> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        log.info("Create tenant request received for tenant: {}", request.getTenantId());

        TenantConfig tenantConfig = tenantConfigService.createTenant(
                request.getTenantId(),
                request.getPlanName(),
                request.getFailBehavior()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(tenantConfig);
    }

    @PutMapping("/{tenantId}/plan")
    public ResponseEntity<TenantConfig> upgradePlan(
            @PathVariable String tenantId,
            @RequestParam String newPlan) {
        log.info("Upgrade plan request received for tenant: {} to plan: {}", tenantId, newPlan);

        TenantConfig tenantConfig = tenantConfigService.updateTenantPlan(tenantId, newPlan);
        return ResponseEntity.ok(tenantConfig);
    }

    @GetMapping("/{tenantId}")
    public ResponseEntity<TenantConfig> getTenant(@PathVariable String tenantId) {
        log.info("Get tenant config request received for tenant: {}", tenantId);

        TenantConfig tenantConfig = tenantConfigService.getConfig(tenantId);
        return ResponseEntity.ok(tenantConfig);
    }
}
