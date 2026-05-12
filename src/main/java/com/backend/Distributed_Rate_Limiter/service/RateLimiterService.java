package com.backend.Distributed_Rate_Limiter.service;

import com.backend.Distributed_Rate_Limiter.dto.RateLimitRequest;
import com.backend.Distributed_Rate_Limiter.dto.RateLimitResponse;
import com.backend.Distributed_Rate_Limiter.dto.TenantConfig;
import com.backend.Distributed_Rate_Limiter.entity.AuditLog;
import com.backend.Distributed_Rate_Limiter.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RateLimiterService {

    private final TenantConfigService tenantConfigService;
    private final AuditLogRepository auditLogRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final DefaultRedisScript<List<Long>> slidingWindowScript;


//    request comes in
//      ↓
//    get tenant config from caffeine
//      ↓
//    redis available? → run lua script → return result
//    redis down? → check fail_behavior → allow or deny

    public RateLimitResponse check(RateLimitRequest rateLimitRequest) {
        TenantConfig config;

        try {
            config = tenantConfigService.getConfig(rateLimitRequest.getTenantId());
        } catch (Exception e) {
            return new RateLimitResponse(false, 0, 0, "Tenant configuration not found");
        }

        try {
            return runSlidingWindow(rateLimitRequest, config);
        } catch (Exception e) {
            log.error("Redis error for tenant: {}", rateLimitRequest.getTenantId(), e);
            return handleRedisFailure(config);
        }
    }

    private RateLimitResponse runSlidingWindow(RateLimitRequest request, TenantConfig config) {
        String key = buildKey(request);

        long now = System.currentTimeMillis();

        String requestId = UUID.randomUUID().toString(); // use unique id for each request so two requests at the same ms don't collide in the sorted set
        List<Long> result = redisTemplate.execute(
                slidingWindowScript,
                Collections.singletonList(key),
                String.valueOf(config.getWindowMs()),
                String.valueOf(config.getCapacity()),
                String.valueOf(now),
                requestId
        );

        boolean allowed = result.get(0) == 1L;
        int remaining = result.get(1).intValue();
        long resetAt = result.get(2);

        saveAuditLog(request.getTenantId(), request.getUserId(), request.getAction(), allowed ? "ALLOWED" : "REJECTED");

        String reason = allowed ? "ok" : "rate limit exceeded";
        return new RateLimitResponse(allowed, remaining, resetAt, reason);
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

    private String buildKey(RateLimitRequest request) {
        return request.getTenantId() + ":" +
                request.getUserId()   + ":" +
                request.getAction();
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
