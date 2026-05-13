package com.backend.Distributed_Rate_Limiter.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TenantConfig {
    private String tenantId;
    private Integer capacity;
    private Double refillRate;
    private Long windowMs;
    private String failBehavior;
    private String algorithm;
}
