package com.backend.Distributed_Rate_Limiter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PlanUpgradeEvent {
    private String tenantId;
    private String newPlan;
}
