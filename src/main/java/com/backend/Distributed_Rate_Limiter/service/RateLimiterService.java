package com.backend.Distributed_Rate_Limiter.service;

import com.backend.Distributed_Rate_Limiter.dto.RateLimitRequest;
import com.backend.Distributed_Rate_Limiter.dto.RateLimitResponse;
import com.backend.Distributed_Rate_Limiter.dto.TenantConfig;
import com.backend.Distributed_Rate_Limiter.entity.AuditLog;
import com.backend.Distributed_Rate_Limiter.repository.AuditLogRepository;
import com.backend.Distributed_Rate_Limiter.strategy.RateLimitStrategy;
import com.backend.Distributed_Rate_Limiter.strategy.SlidingWindowStrategy;
import com.backend.Distributed_Rate_Limiter.strategy.TokenBucketStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class RateLimiterService {

    private final TenantConfigService tenantConfigService;
    private final AuditLogRepository auditLogRepository;
    private final Map<String, RateLimitStrategy> strategies;

    public RateLimiterService(TenantConfigService tenantConfigService, AuditLogRepository auditLogRepository, Map<String, RateLimitStrategy> strategies) {
        this.tenantConfigService = tenantConfigService;
        this.auditLogRepository = auditLogRepository;
        this.strategies = strategies;
    }


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
            return new RateLimitResponse(false, 0, 0, "Tenant configuration not found");
        }

        RateLimitStrategy strategy = strategies.get(config.getAlgorithm());
        if (strategy == null) {
            log.error("Unknown algorithm: {}", config.getAlgorithm());
            return new RateLimitResponse(false, 0, 0, "Unknown algorithm: " + config.getAlgorithm());
        }

        try {
            RateLimitResponse response = strategy.check(rateLimitRequest, config);

            saveAuditLog(
                    rateLimitRequest.getTenantId(),
                    rateLimitRequest.getUserId(),
                    rateLimitRequest.getAction(),
                    response.isAllowed() ? "allowed" : "denied"
            );

            return response;
        } catch (Exception e) {
            log.error("Redis error for tenant: {}", rateLimitRequest.getTenantId(), e);
            return handleRedisFailure(config);
        }
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
