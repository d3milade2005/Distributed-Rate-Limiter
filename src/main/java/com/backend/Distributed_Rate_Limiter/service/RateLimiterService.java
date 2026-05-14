package com.backend.Distributed_Rate_Limiter.service;

import com.backend.Distributed_Rate_Limiter.dto.RateLimitRequest;
import com.backend.Distributed_Rate_Limiter.dto.RateLimitResponse;
import com.backend.Distributed_Rate_Limiter.dto.TenantConfig;
import com.backend.Distributed_Rate_Limiter.entity.AuditLog;
import com.backend.Distributed_Rate_Limiter.repository.AuditLogRepository;
import com.backend.Distributed_Rate_Limiter.strategy.RateLimitStrategy;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
@Slf4j
public class RateLimiterService {

    private final TenantConfigService tenantConfigService;
    private final AuditLogRepository auditLogRepository;
    private final Map<String, RateLimitStrategy> strategies;
    private final MeterRegistry meterRegistry;

    public RateLimiterService(TenantConfigService tenantConfigService, AuditLogRepository auditLogRepository, Map<String, RateLimitStrategy> strategies, MeterRegistry meterRegistry) {
        this.tenantConfigService = tenantConfigService;
        this.auditLogRepository = auditLogRepository;
        this.strategies = strategies;
        this.meterRegistry = meterRegistry;
    }

//    Note strategises looks like this at runtime:
//    {
//        "sliding_window" → SlidingWindowStrategy instance,
//        "token_bucket"   → TokenBucketStrategy instance
//    }


//    request comes in
//      ↓
//    get tenant config from caffeine
//      ↓
//    get the strategy bean based on config.algorithm
//      ↓
//    strategy executes:
//    redis available? → run lua script → return result
//    redis down? → check fail_behavior → allow or deny

    public RateLimitResponse check(RateLimitRequest rateLimitRequest) {
        TenantConfig config;

        try {
            config = tenantConfigService.getConfig(rateLimitRequest.getTenantId());
        } catch (Exception e) {
            log.error("Tenant not found: {}", rateLimitRequest.getTenantId());
            recordMetric(rateLimitRequest.getTenantId(), "unknown", "denied", "tenant_not_found");
            return new RateLimitResponse(false, 0, 0, "Tenant configuration not found");
        }

        RateLimitStrategy strategy = strategies.get(config.getAlgorithm());
        if (strategy == null) {
            log.error("Unknown algorithm: {}", config.getAlgorithm());
            recordMetric(rateLimitRequest.getTenantId(), config.getAlgorithm(), "denied", "unknown_algorithm");
            return new RateLimitResponse(false, 0, 0, "Unknown algorithm: " + config.getAlgorithm());
        }

        try {
            RateLimitResponse response = strategy.check(rateLimitRequest, config);

            String result = response.isAllowed() ? "allowed" : "denied";
            recordMetric(rateLimitRequest.getTenantId(), config.getAlgorithm(), result, rateLimitRequest.getAction());

            saveAuditLog(
                    rateLimitRequest.getTenantId(),
                    rateLimitRequest.getUserId(),
                    rateLimitRequest.getAction(),
                    result
            );

            return response;
        } catch (Exception e) {
            log.error("Redis error for tenant: {}", rateLimitRequest.getTenantId(), e);
            recordMetric(rateLimitRequest.getTenantId(), config.getAlgorithm(), "error", rateLimitRequest.getAction());
            return handleRedisFailure(config);
        }
    }

    private void recordMetric(String tenantId, String algorithm, String result, String action) {
        Counter.builder("rate_limiter.requests")
                .tag("tenant", tenantId)
                .tag("algorithm", algorithm)
                .tag("result", result)
                .tag("action", action)
                .register(meterRegistry)
                .increment();
    }


    private RateLimitResponse handleRedisFailure(TenantConfig config) {
        boolean allowed = config.getFailBehavior().equals("fail_open");
        log.warn("Redis unavailable - fail behavior: {}", config.getFailBehavior());
        saveAuditLog("unknown", "unknown", "unknown", allowed ? "allowed" : "denied");
        return new RateLimitResponse(
                allowed,
                0,
                0,
                allowed ? "redis unavailable - fail open" : "redis unavailable - fail closed"
        );
    }


    @Async
    protected void saveAuditLog(String tenantId, String userId, String action, String result) {
        try {
            AuditLog log = new AuditLog();
            log.setTenantId(tenantId);
            log.setUserId(userId);
            log.setAction(action);
            log.setResult(result);
            auditLogRepository.save(log);
        } catch (Exception e) {
            log.error("Failed to save audit log", e);
        }
    }
}
