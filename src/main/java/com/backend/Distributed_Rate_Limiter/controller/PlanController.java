package com.backend.Distributed_Rate_Limiter.controller;

import com.backend.Distributed_Rate_Limiter.dto.PlanUpgradeEvent;
import com.backend.Distributed_Rate_Limiter.kafka.PlanUpgradeProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
@Slf4j
public class PlanController {
    private final PlanUpgradeProducer planUpgradeProducer;

    @PostMapping("/upgrade")
    public ResponseEntity<String> planUpgrade(@RequestParam String tenantId, @RequestParam String newPlan) {
        log.info("Received request to upgrade tenant: {} to plan: {}", tenantId, newPlan);
        planUpgradeProducer.sendPlanUpgradeEvent(tenantId, newPlan);
        return ResponseEntity.ok("Plan upgrade event sent successfully for tenant: " + tenantId);
    }
}
