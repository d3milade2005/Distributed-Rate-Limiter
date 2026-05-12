package com.backend.Distributed_Rate_Limiter.service;

import com.backend.Distributed_Rate_Limiter.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final TenantConfigService tenantConfigService;
    private final AuditLogRepository auditLogRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final DefaultRedisScript<List<Long>> slidingWindowScript;

}
