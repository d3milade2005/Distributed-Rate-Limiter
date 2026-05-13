package com.backend.Distributed_Rate_Limiter.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RateLimitRequest {
    @NotBlank(message = "tenantId is required")
    private String tenantId;

    @NotBlank(message = "cost is required")
    private String userId;

    @NotBlank(message = "action is required")
    private String action;

    @Min(value=1, message="cost must be at least 1")
    private int cost;
}
