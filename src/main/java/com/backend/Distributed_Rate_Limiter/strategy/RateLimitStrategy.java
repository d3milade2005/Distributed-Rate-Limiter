package com.backend.Distributed_Rate_Limiter.strategy;

import com.backend.Distributed_Rate_Limiter.dto.RateLimitRequest;
import com.backend.Distributed_Rate_Limiter.dto.RateLimitResponse;
import com.backend.Distributed_Rate_Limiter.dto.TenantConfig;

public interface RateLimitStrategy {
    RateLimitResponse check(RateLimitRequest request, TenantConfig config);
}
