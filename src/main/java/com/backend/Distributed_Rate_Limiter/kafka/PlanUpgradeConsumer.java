package com.backend.Distributed_Rate_Limiter.kafka;

import com.backend.Distributed_Rate_Limiter.dto.PlanUpgradeEvent;
import com.backend.Distributed_Rate_Limiter.service.TenantConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlanUpgradeConsumer {
    private final TenantConfigService tenantConfigService;
    private final ObjectMapper objectMapper;


    @KafkaListener(topics = "plan.upgraded", groupId = "rate-limiter-group")
    public void onPlanUpgraded(String message) {
        try {
            log.info("Received plan upgrade event: {}", message);

            PlanUpgradeEvent event = objectMapper.readValue(message, PlanUpgradeEvent.class);
            tenantConfigService.updateTenantPlan(event.getTenantId(), event.getNewPlan());

            log.info("Successfully processed plan upgrade for tenant: {} to plan: {}", event.getTenantId(), event.getNewPlan());
        } catch (Exception e) {
            log.error("Failed to process plan upgrade event: {}", message, e);
        }
    }
}
