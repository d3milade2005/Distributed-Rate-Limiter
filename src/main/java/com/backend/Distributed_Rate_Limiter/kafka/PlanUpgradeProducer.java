package com.backend.Distributed_Rate_Limiter.kafka;

import com.backend.Distributed_Rate_Limiter.dto.PlanUpgradeEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlanUpgradeProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendPlanUpgradeEvent(String tenantId, String newPlan) {
        try {
            PlanUpgradeEvent event = new PlanUpgradeEvent(tenantId, newPlan);
            String message = objectMapper.writeValueAsString(event);

            kafkaTemplate.send("plan.upgraded", message);
            log.info("Sent plan upgrade event for tenant: {} to plan: {}", tenantId, newPlan);
        } catch (Exception e) {
            log.error("Failed to send plan upgrade event for tenant: {} to plan: {}", tenantId, newPlan, e);
        }
    }
}
